package de.ada.restapi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringEscapeUtils;
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
	
	private static final String API_TOKEN_HEADER_FIELD = "X-API-Token";
	
	private static int defaultPort = 7002;
	private static String defaultContext = "/api_lt";
//    private static String defaultEndpoint = "http://127.0.0.1:8890/sparql";
	private static String defaultEndpoint = "http://ada.filmontology.org/sparql_lt";
	
	private static String API_TOKEN;
	
	final Logger logger = LoggerFactory.getLogger(Server.class);

	public Server(String apitoken) {
		Server.API_TOKEN = apitoken;
	}

	/**
	 * Converts media/scene request parameter to media id string and a set of scene id strings.
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
			logger.error("{} {} Invalid format of supplied media id and/or scenes", ctx.method(), ctx.url());
			ctx.status(400);
			ctx.contentType("application/json");
			ctx.result("{\"error\": {\"message\": \"Invalid format of supplied media id and/or scenes.\",\"code\": 400}}");
			return;
    	}
    	Set<String> scenes = getScenes(param).right;
    	Set<String> types = new HashSet<String>();
    	if (withTypes) {
        	String typeParam = properEscapeRemove(ctx.pathParam("type"));
        	types = getTypes(typeParam);
    	}

		AnnotationManager am = AnnotationManager.getInstance(defaultEndpoint);
		Model annotations = am.getAnnotations(mediaId, scenes, types);
		if (annotations == null) {
			ctx.status(500);
			ctx.contentType("application/json");
			ctx.result("{\"error\": {\"message\": \"Query annotations - Triplestore query failed\",\"code\": 500}}");
			return;
		}
		
   		Map<String, Object> jsonld = am.modelToFramedJsonld(annotations);
   		if (jsonld == null) {
			ctx.status(500);
			ctx.contentType("application/json");
			ctx.result("{\"error\": {\"message\": \"Query result processing - Conversion to jsonld failed\",\"code\": 500}}");
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
    	
		AnnotationManager am = AnnotationManager.getInstance(defaultEndpoint);
		Model annotations = am.textSearch(searchTerm, whole, mediaIdSet, typeSet);
		if (annotations == null) {
			ctx.status(500);
			ctx.contentType("application/json");
			ctx.result("{\"error\": {\"message\": \"Text search annotations - Triplestore query failed\",\"code\": 500}}");
			return;
		}

   		Map<String, Object> jsonld = am.modelToFramedJsonld(annotations);
   		if (jsonld == null) {
			ctx.status(500);
			ctx.contentType("application/json");
			ctx.result("{\"error\": {\"message\": \"Query result processing - Conversion to jsonld failed\",\"code\": 500}}");
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
    	
		AnnotationManager am = AnnotationManager.getInstance(defaultEndpoint);
		Model annotations = am.valueSearch(valueSet, mediaIdSet);
		if (annotations == null) {
			ctx.status(500);
			ctx.contentType("application/json");
			ctx.result("{\"error\": {\"message\": \"Value search annotations - Triplestore query failed\",\"code\": 500}}");
			return;
		}

   		Map<String, Object> jsonld = am.modelToFramedJsonld(annotations);
   		if (jsonld == null) {
			ctx.status(500);
			ctx.contentType("application/json");
			ctx.result("{\"error\": {\"message\": \"Query result processing - Conversion to jsonld failed\",\"code\": 500}}");
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
			logger.error("{} {} deleteMedia - unauthorized access {}", ctx.method(), ctx.url(), ctx.body().replace("\n", ""));
			ctx.status(403);
			ctx.contentType("application/json");
			ctx.result("{\"error\": {\"message\": \"API Function Requires Authentification.\",\"code\": 403}}");
			return false;
    	}
    	return true;
	}
	
	void runServer() {

		Javalin app = Javalin.create(config -> {
			config.contextPath = defaultContext;
			config.enableCorsForAllOrigins();
			config.requestLogger((ctx, executionTimeMs) -> {
				logger.info("{} {} {} {} \"{}\" {}", ctx.method(), ctx.url(), ctx.req.getRemoteHost(),
						ctx.res.getStatus(), ctx.userAgent(), executionTimeMs.longValue());
			});
		}).start(defaultPort);
		
		app.get("/getMovieMetadata", ctx -> {
			MetadataManager mdm = MetadataManager.getInstance(defaultEndpoint);
			
        	List<Map<String,Object>> movieMetadata = mdm.getMovieMetadata(null);
        	
        	if (movieMetadata == null) {
				logger.error("{} {} Triplestore query for movie metadata failed.", ctx.method(), ctx.url());
				ctx.status(500);
				ctx.contentType("application/json");
				ctx.result("{\"error\": {\"message\": \"Triplestore query for movie metadata failed.\",\"code\": 500}}");
        	} else {
        		ctx.json(movieMetadata);
        	}

		});

		app.get("/getMovieMetadata/:mediaId", ctx -> {
			MetadataManager mdm = MetadataManager.getInstance(defaultEndpoint);
			
			String queryId = properEscapeRemove(ctx.pathParam("mediaId"));
			
        	List<Map<String,Object>> movieMetadata = mdm.getMovieMetadata(queryId);
        	
        	if (movieMetadata == null) {
				logger.error("{} {} Triplestore query for movie metadata failed.", ctx.method(), ctx.url());
				ctx.status(500);
				ctx.contentType("application/json");
				ctx.result("{\"error\": {\"message\": \"Triplestore query for movie metadata failed.\",\"code\": 500}}");
        	} else {
        		ctx.json(movieMetadata);
        	}

		});

        app.get("/getOntology", ctx -> {
			MetadataManager mdm = MetadataManager.getInstance(defaultEndpoint);
			
			List<Map<String,Object>> ontology = mdm.getOntology();
			
        	if (ontology == null) {
				logger.error("{} {} Triplestore query for ontology failed.", ctx.method(), ctx.url());
				ctx.status(500);
				ctx.contentType("application/json");
				ctx.result("{\"error\": {\"message\": \"Triplestore query for ontology failed.\",\"code\": 500}}");
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

			MetadataManager mdm = MetadataManager.getInstance(defaultEndpoint);
			
			String result = mdm.deleteMedia(mediaId);
			if (result != null) {
				//TODO Use returnError function. Requires changes of returns in metadata manager.
				ctx.status(500);
				ctx.contentType("application/json");
				ctx.result(result);
				return;
			}
			logger.info("deleteMedia - id {} deleted", mediaId);
			ctx.status(200);
		});

		app.post("/addMedia", ctx -> {
			if (!checkApiToken(ctx)) {
				return;
			}

			MetadataManager mdm = MetadataManager.getInstance(defaultEndpoint);
			MetadataRecord record = null;
			
			//TODO Nicht bei allen Logs die URL etc mit ausgeben

			try {
				ObjectMapper om = new ObjectMapper();
				
				logger.info("{} {} addMedia - input {}", ctx.method(), ctx.url(), ctx.body().replace("\n", ""));
				record = om.readValue(ctx.body(), MetadataRecord.class);				

			} catch (JsonProcessingException e) {
				String msg = e.toString().replace("\n", " ");
				logger.error("{} {} JsonProcessingException {}", ctx.method(), ctx.url(), msg);
				msg = msg.replace("\"", "");
				ctx.status(400);
				ctx.contentType("application/json");
				ctx.result("{\"error\": {\"message\": \"Not a valid json input.\",\"code\": 400,\"cause\": \"" + msg
						+ "\"}}");
				return;
			} catch (IllegalArgumentException iae) {
				String msg = iae.toString().replace("\n", " ");
				logger.error("{} {} IllegalArgumentException {}", ctx.method(), ctx.url(), msg);
				msg = msg.replace("\"", "");
				ctx.status(400);
				ctx.contentType("application/json");
				ctx.result("{\"error\": {\"message\": \"Unknown json field supplied.\",\"code\": 400,\"cause\": \"" + msg
						+ "\"}}");
				return;
			}

			if (mdm.validateMissing(record)) {
				String msg = record.toString().replace("\n", " ");
				logger.error("{} {} Mandatory Field Missing {}", ctx.method(), ctx.url(), msg);
				ctx.status(400);
				ctx.contentType("application/json");
				ctx.result("{\"error\": {\"message\": \"Mandatory field is null.\",\"code\": 400,\"cause\": \"" + msg
						+ "\"}}");
				return;
			} else {
				String result = mdm.addMedia(record);
				if (result != null) {
					ctx.status(500);
					ctx.contentType("application/json");
					ctx.result(result);
					return;
				}
				logger.info("{} {} addMedia - added {}", ctx.method(), ctx.url(), record.toString().replace("\n", ""));
			}

			ctx.status(201);
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
			
			List<UploadedFile> uploadedFiles = ctx.uploadedFiles("upload_file");
			
			if (uploadedFiles == null || uploadedFiles.size() != 1) {
				returnError(ctx, "Exactly one upload_file is required.", 500, null);
				return;
			}
			
			InputStream content = uploadedFiles.get(0).getContent();
			
			AnnotationManager am = AnnotationManager.getInstance(defaultEndpoint);
			am.insertAnnotations(mediaId, content);
			
		});
		
	}

	public static void main(String[] args) {
		if (args.length == 2) {
            defaultEndpoint = args[0];
            defaultContext = args[1];
		}
		System.out.println("Using configuration port: " + defaultPort + " and endpoint: " + defaultEndpoint + " context: " + defaultContext);

		String apitoken = System.getenv("API_TOKEN");
		if (apitoken == null) {
			System.err.println("API requires the environment variable \"API_TOKEN\" to be set.");
			System.exit(1);
		} else {
			new Server(apitoken).runServer();
		}
	}

}
