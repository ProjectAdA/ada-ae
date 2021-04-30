package de.ada.restapi;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

public class Server {
	
	// Port on which the API listens.
	// Warning: if the default port is modified, configuration of respective
	// docker images and/or reverse proxy must be adjusted as well 
	private static final int defaultPort = 7002;

	// Context path (url prefix) to which web requests are sent, e.g., /api
	private static String applicationContext = "";

	// URL of the SPARQL endpoint
	private static String sparqlEndpoint = "";

	// URL of the SPARQL endpoint with authentification
	private static String sparqlAuthEndpoint = "";

	// Token that is required to authenticate to the API for update/insert/delete requests
	private static String API_TOKEN = "";

	// User to authenticate at the triple store for update/insert/delete queries
	private static String SPARQL_UPDATE_USER = "";

	// Password to authenticate at the triple store for update/insert/delete queries
	private static String SPARQL_UPDATE_PASSWORD = "";

	// Name of the field in the HTTP request header
	private static final String API_TOKEN_HEADER_FIELD = "X-API-Token";
	
	// URL of the Advene service that converts AZP packages to JSON-LD
	private static String ADVENE_SERVICE_URL = "";


	final Logger logger = LoggerFactory.getLogger(Server.class);

	/**
	 * Converts media/scene request parameters to media id string and a set of scene id strings.
	 * @param ids
	 * @return
	 */
	private ImmutablePair<String, Set<String>> getScenes(String ids) {
		Set<String> scenes = new HashSet<String>();
		String mediaId = ids;
		if (ids.contains("_")) {
			String[] split = ids.split("_");
			if (split.length < 2) {
				return ImmutablePair.of(null, null);
			}
			mediaId = split[0];
			for (int i = 1; i < split.length; i++) {
				if (split[i].length() > 0) {
					scenes.add(split[i]);
				}
			}
		}
		return ImmutablePair.of(mediaId, scenes);
	}
	
	/**
	 * Converts type request parameter to a set of annotation type id strings.
	 * @param types
	 * @return
	 */
	private Set<String> getTypes(String types) {
		Set<String> result = new HashSet<String>();
		if (types != null) {
			if (types.contains(",")) {
				String[] split = types.split(",");
				for (int i = 0; i < split.length; i++) {
					result.add("AnnotationType/"+split[i]);
				}
			} else {
				result.add("AnnotationType/"+types);
			}
		}
		
		return result;
	}

	/**
	 * Converts media request parameter to a set of media id strings.
	 * @param mediaIds
	 * @return
	 */
	private Set<String> getMediaIds(String mediaIds) {
		Set<String> result = new HashSet<String>();
		if (mediaIds != null) {
			if (mediaIds.contains(",")) {
				String[] split = mediaIds.split(",");
				for (int i = 0; i < split.length; i++) {
					result.add(split[i]);
				}
			} else {
				result.add(mediaIds);
			}
		}
		
		return result;
	}

	/**
	 * Escapes input parameters sent to the API and removes all escaped characters as well as several additional special characters to avoid disruption of SPARQL queries.
	 * @param input
	 * @return
	 */
	private String properEscapeRemove(String input) {
		String escaped = StringEscapeUtils.escapeJava(input);
    	Pattern p = Pattern.compile("\\\\");
    	Matcher matcher = p.matcher(escaped);
    	ArrayList<Integer> posis = new ArrayList<Integer>();
    	while (matcher.find()) {
    		posis.add(matcher.start());
    	}
    	Collections.sort(posis, Collections.reverseOrder());
    	StringBuilder sb = new StringBuilder(escaped);
    	for (Integer i : posis) {
			sb.deleteCharAt(i);
			sb.deleteCharAt(i);
		}
    	return sb.toString().replaceAll("<", "").replaceAll(">", "").replaceAll("'", "").replaceAll("\\?", "").replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\]", "").replaceAll("\\[", "");
	}
	
	private void handleGetAnnotations(Context ctx, boolean withTypes) {
		
    	String param = properEscapeRemove(ctx.pathParam("mediaId"));
    	
    	String mediaId = getScenes(param).left;
    	if (mediaId == null) {
    		returnError(ctx, "Invalid format of supplied media id and/or scenes.", 400, null);
			return;
    	}
    	Set<String> scenes = getScenes(param).right;
    	Set<String> types = new HashSet<String>();
    	if (withTypes) {
        	String typeParam = properEscapeRemove(ctx.pathParam("type"));
        	types = getTypes(typeParam);
    	}

		AnnotationManager am = AnnotationManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
		Model annotations = am.getAnnotations(mediaId, scenes, types);
		if (annotations == null) {
    		returnError(ctx, "Query annotations - Triplestore query failed.", 500, null);
			return;
		}
		
   		Map<String, Object> jsonld = am.modelToFramedJsonld(annotations);
   		if (jsonld == null) {
    		returnError(ctx, "Query result processing - Conversion to jsonld failed.", 500, null);
			return;
   		}
   		ctx.json(jsonld);
	}
	
	
	private void handleTextSearch(Context ctx, boolean withMediaIds, boolean withTypes) {
    	String searchTerm = properEscapeRemove(ctx.pathParam("searchterm"));
    	String pw = properEscapeRemove(ctx.pathParam("whole"));
    	boolean whole = false;
    	if ("wholeword".equals(pw)) {
    		whole = true;
    	}
    	
    	Set<String> mediaIdSet = new HashSet<String>();
    	if (withMediaIds) {
        	String mediaIds = properEscapeRemove(ctx.pathParam("mediaIds"));
        	if (!"all".equals(mediaIds)) {
        		mediaIdSet = getMediaIds(mediaIds);
        	}
    	}

    	Set<String> typeSet = new HashSet<String>();
    	if (withTypes) {
        	String typeParam = properEscapeRemove(ctx.pathParam("types"));
        	typeSet = getTypes(typeParam);
    	}
    	
		AnnotationManager am = AnnotationManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
		Model annotations = am.textSearch(searchTerm, whole, mediaIdSet, typeSet);
		if (annotations == null) {
    		returnError(ctx, "Text search annotations - Triplestore query failed.", 500, null);
			return;
		}

   		Map<String, Object> jsonld = am.modelToFramedJsonld(annotations);
   		if (jsonld == null) {
    		returnError(ctx, "Query result processing - Conversion to jsonld failed.", 500, null);
			return;
   		}
   		ctx.json(jsonld);

	}
	
	private void handleValueSearch(Context ctx, boolean withMediaIds) {
    	String valueTerm = properEscapeRemove(ctx.pathParam("values"));
    	
    	Set<String> valueSet = new HashSet<String>();
    	if (valueTerm.contains(";") ) {
    		for (String s : valueTerm.split(";")) {
    			valueSet.add(s);
			}
    	} else {
    		valueSet.add(valueTerm);
    	}
		
		Set<String> mediaIdSet = new HashSet<String>();
    	if (withMediaIds) {
        	String mediaIds = properEscapeRemove(ctx.pathParam("mediaIds"));
        	if (!"all".equals(mediaIds)) {
        		mediaIdSet = getMediaIds(mediaIds);
        	}
    	}
    	
		AnnotationManager am = AnnotationManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
		Model annotations = am.valueSearch(valueSet, mediaIdSet);
		if (annotations == null) {
    		returnError(ctx, "Value search annotations - Triplestore query failed.", 500, null);
			return;
		}

   		Map<String, Object> jsonld = am.modelToFramedJsonld(annotations);
   		if (jsonld == null) {
			ctx.status(500);
			ctx.contentType("application/json");
    		returnError(ctx, "Query result processing - Conversion to jsonld failed.", 500, null);
			return;
   		}
   		ctx.json(jsonld);
	}

	private void returnError(Context ctx, String msg, Integer code, Exception ex) {
		String cause = "";
		if (ex != null) {
			cause = ex.toString().replace("\n", " ").replace("\"", "");
		}
		logger.error("{} {} {} {}", ctx.method(), ctx.url(), msg, cause);
		ctx.status(code);
		ctx.contentType("application/json");
		ctx.result("{\"error\": {\"message\": \""+msg+"\",\"code\": "+code+",\"cause\": \"" + cause + "\"}}");
	}

	private boolean checkApiToken(Context ctx) {
    	String token = ctx.header(API_TOKEN_HEADER_FIELD);
    	if (token == null || !API_TOKEN.equals(token)) {
    		returnError(ctx, "API Function Requires Authentification.", 403, null);
			return false;
    	}
    	return true;
	}
	
	void runServer() {
		
		Javalin app = Javalin.create(config -> {
			config.contextPath = applicationContext;
			//TODO Evaluate CORS restrictions
			config.enableCorsForAllOrigins();
			config.requestLogger((ctx, executionTimeMs) -> {
				logger.info("{} {} {} {} \"{}\" {}", ctx.method(), ctx.url(), ctx.req.getRemoteHost(),
						ctx.res.getStatus(), ctx.userAgent(), executionTimeMs.longValue());
			});
		}).start(defaultPort);
		
		app.get("/getMovieMetadata", ctx -> {
			MetadataManager mdm = MetadataManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
			
        	List<Map<String,Object>> movieMetadata = mdm.getMovieMetadata(null);
        	
        	if (movieMetadata == null) {
        		returnError(ctx, "Triplestore query for movie metadata failed.", 500, null);
        	} else {
        		ctx.json(movieMetadata);
        	}

		});

		app.get("/getMovieMetadata/:mediaId", ctx -> {
			MetadataManager mdm = MetadataManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
			
			String queryId = properEscapeRemove(ctx.pathParam("mediaId"));
			
        	List<Map<String,Object>> movieMetadata = mdm.getMovieMetadata(queryId);
        	
        	if (movieMetadata == null) {
        		returnError(ctx, "Triplestore query for movie metadata "+queryId+"failed.", 500, null);
        	} else {
        		ctx.json(movieMetadata);
        	}

		});

        app.get("/getOntology", ctx -> {
			MetadataManager mdm = MetadataManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
			
			List<Map<String,Object>> ontology = mdm.getOntology();
			
        	if (ontology == null) {
        		returnError(ctx, "Triplestore query for ontology failed.", 500, null);
        	} else {
    			ctx.json(ontology);
        	}
        });
        
        app.get("/jsonld/getAnnotations/:mediaId/:cat/:type", ctx -> {
        	handleGetAnnotations(ctx, true);
        });
        
        app.get("/jsonld/getAnnotations/:mediaId", ctx -> {
        	handleGetAnnotations(ctx, false);
        });
        
        app.get("/jsonld/textSearch/:searchterm/:whole", ctx -> {
        	handleTextSearch(ctx, false, false);
        });
        
        app.get("/jsonld/textSearch/:searchterm/:whole/:mediaIds", ctx -> {
        	handleTextSearch(ctx, true, false);
        });
        
        app.get("/jsonld/textSearch/:searchterm/:whole/:mediaIds/:cat/:types", ctx -> {
        	handleTextSearch(ctx, true, true);
        });

        app.get("/jsonld/valueSearch/:values", ctx -> {
        	handleValueSearch(ctx, false);
        });

        app.get("/jsonld/valueSearch/:values/:mediaIds", ctx -> {
        	handleValueSearch(ctx, true);
        });

		app.post("/deleteMedia", ctx -> {
			if (!checkApiToken(ctx)) {
				return;
			}
			logger.info("deleteMedia - input {}", ctx.body().replace("\n", ""));
        	
        	String mediaId = null;
        	
			try {
				ObjectMapper om = new ObjectMapper();
				Map<String, String> input = om.readValue(ctx.body(), new TypeReference<Map<String, String>>() {});
				
				mediaId = input.get("id");
				
				if ( mediaId == null || mediaId.length() == 0) {
					returnError(ctx, "deleteMedia - json field id is missing or empty. "+ctx.body().replace("\n", " ").replace("\"", ""), 400, null);
					return;
				}

			} catch (JsonProcessingException e) {
				returnError(ctx, "Not a valid json input.", 400, e);
				return;
			} catch (IllegalArgumentException iae) {
				returnError(ctx, "Unknown json field supplied.", 400, iae);
				return;
			}
			
			mediaId = properEscapeRemove(mediaId);

			MetadataManager mdm = MetadataManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
			
			String result = mdm.deleteMedia(mediaId);
			if (result != null) {
				returnError(ctx, result, 500, null);
				return;
			}
			logger.info("deleteMedia - id {} deleted", mediaId);
			ctx.status(200);
		});

		app.post("/addMedia", ctx -> {
			if (!checkApiToken(ctx)) {
				return;
			}

			MetadataManager mdm = MetadataManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
			MetadataRecord record = null;
			
			try {
				ObjectMapper om = new ObjectMapper();
				
				logger.info("addMedia - input {}", ctx.body().replace("\n", ""));
				record = om.readValue(ctx.body(), MetadataRecord.class);

			} catch (JsonProcessingException e) {
				returnError(ctx, "Not a valid json input. JsonProcessingException.", 400, e);
				return;
			} catch (IllegalArgumentException iae) {
				returnError(ctx, "Unknown json field supplied. IllegalArgumentException.", 400, iae);
				return;
			}
			
			if (mdm.validateMissing(record)) {
				returnError(ctx, "Mandatory json field missing,", 400, null);
				return;
			} else {
				String result = mdm.addMedia(record);
				if (result != null) {
					returnError(ctx, result, 500, null);
					return;
				}
				logger.info("addMedia - added {}", record.toString().replace("\n", ""));
			}

			ctx.status(201);
		});

		app.post("/uploadAdvenePackage", ctx -> {
			if (!checkApiToken(ctx)) {
				return;
			}

			if (!ctx.isMultipartFormData()) {
				returnError(ctx, "Request is not multipart/form-data.", 500, null);
				return;
			}
			
			String mediaId = ctx.formParam("media_id");
			if (mediaId == null) {
				returnError(ctx, "Field media_id is missing in request.", 500, null);
				return;
			}

			List<UploadedFile> uploadedFiles = ctx.uploadedFiles("upload_file");
			
			if (uploadedFiles == null || uploadedFiles.size() != 1) {
				returnError(ctx, "Exactly one upload_file is required.", 500, null);
				return;
			}
			
			InputStream upload_content = uploadedFiles.get(0).getContent();
			String filename = uploadedFiles.get(0).getFilename();
			
			InputStream converted_result = null;
			
			logger.info("uploadAdvenePackage - media id "+mediaId+" - filename "+uploadedFiles.get(0).getFilename());
			
			try {
				CloseableHttpClient client = HttpClients.createDefault();
				HttpPost post = new HttpPost(ADVENE_SERVICE_URL);
				MultipartEntityBuilder builder = MultipartEntityBuilder.create();
				builder.addBinaryBody("file", upload_content, ContentType.APPLICATION_OCTET_STREAM, filename);
				post.setEntity(builder.build());

				logger.info("uploadAdvenePackage - Advene service call at "+ADVENE_SERVICE_URL);

				HttpResponse response = client.execute(post);
				HttpEntity entity = response.getEntity();
				InputStream resultInputStream = entity.getContent();
				
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					String result = IOUtils.toString(resultInputStream, StandardCharsets.UTF_8.name());
					returnError(ctx, "Advene service call failed.", 500, new Exception(result));
					return;
				}
				
				converted_result = resultInputStream;
				
			} catch (Exception e) {
				returnError(ctx, "Advene Service call failed.", 500, e);
				return;
			}
			
			if (converted_result != null) {
				AnnotationManager am = AnnotationManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
				String result = am.insertAdveneResult(mediaId, converted_result);
				if (result != null) {
					returnError(ctx, result, 500, null);
					return;
				}
			}
			
			
		});

		app.post("/uploadExtractorResult", ctx -> {
			if (!checkApiToken(ctx)) {
				return;
			}
			
			if (!ctx.isMultipartFormData()) {
				returnError(ctx, "Request is not multipart/form-data.", 500, null);
				return;
			}
			
			String mediaId = ctx.formParam("media_id");
			if (mediaId == null) {
				returnError(ctx, "Field media_id is missing in request.", 500, null);
				return;
			}

			String extractor = ctx.formParam("extractor");
			if (extractor == null) {
				returnError(ctx, "Field extractor is missing in request.", 500, null);
				return;
			}

			List<UploadedFile> uploadedFiles = ctx.uploadedFiles("upload_file");
			
			if (uploadedFiles == null || uploadedFiles.size() != 1) {
				returnError(ctx, "Exactly one upload_file is required.", 500, null);
				return;
			}
			
			InputStream content = uploadedFiles.get(0).getContent();
			
			logger.info("uploadExtractorResult - media id "+mediaId+" - filename "+uploadedFiles.get(0).getFilename());
			
			AnnotationManager am = AnnotationManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, SPARQL_UPDATE_USER, SPARQL_UPDATE_PASSWORD);
			String result = am.insertAnnotations(mediaId, extractor, content);		
			if (result != null) {
				returnError(ctx, result, 500, null);
				return;
			}
		});
		
	}

	public static void main(String[] args) {
		
//		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug");
		System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_DATE_TIME_KEY, "true");
		System.setProperty(org.slf4j.impl.SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss:SSS Z");

		final Logger logger = LoggerFactory.getLogger(Server.class);
		logger.info("Starting AdA REST API...");

		if (args.length == 3) {
            sparqlEndpoint = args[0];
            sparqlAuthEndpoint = args[1];
            applicationContext = args[2];
		} else {
			logger.error("Usage  : java -jar ada_rest_api.jar <sparqlEndpointURL> <sparqlAuthEndpointURL> <applicationContext>");
			logger.error("Example: java -jar ada_rest_api.jar http://127.0.0.1:8890/sparql http://127.0.0.1:8890/sparql-auth /api");
			System.exit(1);
		}
		
		logger.info("Using configuration defaultPort "+defaultPort+ ", sparqlEndpoint "+sparqlEndpoint+", applicationContext "+applicationContext);
		
		if (System.getenv("ONTOLOGY_BASE_URI") != null) {
			URIconstants.ONTOLOGY_BASE_URI = System.getenv("ONTOLOGY_BASE_URI");
			if (URIconstants.ONTOLOGY_BASE_URI.endsWith("/")) {
				URIconstants.ONTOLOGY_BASE_URI = URIconstants.ONTOLOGY_BASE_URI.substring(0, URIconstants.ONTOLOGY_BASE_URI.length()-1);
			}
			logger.info("Setting ONTOLOGY_BASE_URI to "+URIconstants.ONTOLOGY_BASE_URI);
		} else {
			logger.error("Environment variable ONTOLOGY_BASE_URI is not set");
			System.exit(1);
		}
		
		if (System.getenv("ONTOLOGY_VERSION") != null) {
			URIconstants.ONTOLOGY_VERSION = System.getenv("ONTOLOGY_VERSION");
			logger.info("Setting ONTOLOGY_VERSION to "+URIconstants.ONTOLOGY_VERSION);
		} else {
			logger.error("Environment variable ONTOLOGY_VERSION is not set");
			System.exit(1);
		}

		if (System.getenv("ADVENE_SERVICE_URL") != null) {
			ADVENE_SERVICE_URL = System.getenv("ADVENE_SERVICE_URL");
			logger.info("Setting ADVENE_SERVICE_URL to "+ADVENE_SERVICE_URL);
		} else {
			logger.error("Environment variable ADVENE_SERVICE_URL is not set");
			System.exit(1);
		}

		if (System.getenv("SPARQL_UPDATE_USER") != null) {
			SPARQL_UPDATE_USER = System.getenv("SPARQL_UPDATE_USER");
			logger.info("Setting SPARQL_UPDATE_USER to "+SPARQL_UPDATE_USER);
		} else {
			logger.error("Environment variable SPARQL_UPDATE_USER is not set");
			System.exit(1);
		}

		if (System.getenv("SPARQL_UPDATE_PASSWORD") != null) {
			SPARQL_UPDATE_PASSWORD = System.getenv("SPARQL_UPDATE_PASSWORD");
			logger.info("Setting SPARQL_UPDATE_PASSWORD");
		} else {
			logger.error("Environment variable SPARQL_UPDATE_PASSWORD is not set");
			System.exit(1);
		}

		if (System.getenv("API_TOKEN") != null) {
			Server.API_TOKEN = System.getenv("API_TOKEN");
			logger.info("Setting API_TOKEN");
		} else {
			logger.error("Environment variable API_TOKEN is not set");
			System.exit(1);
		}
		
		new Server().runServer();
	}

}
