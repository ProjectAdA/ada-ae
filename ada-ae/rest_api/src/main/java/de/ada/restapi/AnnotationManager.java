package de.ada.restapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

public class AnnotationManager {

	private final String sparqlEndpoint;
	private final String sparqlAuthEndpoint;
	private final String sparqlUser;
	private final String sparqlPassword;
	private static AnnotationManager instance;

	final Logger logger;

	private AnnotationManager(String endpoint, String authEndpoint, String user, String password) {
		this.sparqlEndpoint = endpoint;
		this.sparqlAuthEndpoint = authEndpoint;
		this.sparqlUser = user;
		this.sparqlPassword = password;
		logger = LoggerFactory.getLogger(AnnotationManager.class);
	}

	public static AnnotationManager getInstance(String endpoint, String authEndpoint, String user, String password) {
		if (instance == null) {
			instance = new AnnotationManager(endpoint, authEndpoint, user, password);
		}

		return instance;
	}
	
	private String createSceneFilter(Set<String> scenes) {
		String result = "";
		
		if (scenes != null && scenes.size() > 0) {
			int numSzenes = scenes.size();
        	int i = 0;
        	result = "?anno ao:sceneId ?sceneId. FILTER (";
			
			for (String sceneId : scenes) {
				result = result + "?sceneId = \""+sceneId+"\" ";
				i++;
    			if (i == numSzenes) {
    				result = result + " ) ";
    			} else {
    				result = result + " || ";
    			}
			}
		}
		
		return result;
	}

	private String createTypeFilter( Set<String> annotationTypes) {
		String result = "";
		
		if (annotationTypes != null && annotationTypes.size() > 0) {
			int numTypes = annotationTypes.size();
			int i = 0;
			result = "?body ao:annotationType ?annotype. FILTER (";
			
			for (String type : annotationTypes) {
				result = result + "?annotype = <"+URIconstants.RESOURCE_PREFIX()+type+"> ";
				i++;
    			if (i == numTypes) {
    				result = result + " ) ";
    			} else {
    				result = result + " || ";
    			}
			}

		}
		
		return result;
	}

	private String createMediaFilter(Set<String> mediaIdSet) {
		String result = "";
		
		if (mediaIdSet != null && mediaIdSet.size() > 0) {
			int numMedia = mediaIdSet.size();
			int i = 0;
			result = "?anno oa:hasTarget ?target. ?target oa:hasSource ?source. FILTER (";
			
			for (String media : mediaIdSet) {
				result = result + "?source = <"+URIconstants.MEDIA_PREFIX()+media+"> ";
				i++;
    			if (i == numMedia) {
    				result = result + " ) ";
    			} else {
    				result = result + " || ";
    			}
			}

		}
		
		return result;
	}

	private String createValueFilter(String valueIds) {
		String result = "";
		
		Set<String> valuesOR = new HashSet<String>();
    	if (valueIds.contains(",") ) {
    		for (String s : valueIds.split(",")) {
    			valuesOR.add(s);
			}
    	} else {
    		valuesOR.add(valueIds);
    	}
		
		int numValue = valuesOR.size();
		int i = 0;
		result = "FILTER (";
    	for (String value : valuesOR) {
    		result = result + "?annovalue = <"+URIconstants.RESOURCE_PREFIX()+"AnnotationValue/"+value+"> || ?seqValue = <"+URIconstants.RESOURCE_PREFIX()+"AnnotationValue/"+value+">";
			i++;
			if (i == numValue) {
				result = result + " )";
			} else {
				result = result + " || ";
			}
		}
		
		return result;
	}


	public Model getAnnotations(String mediaId, Set<String> scenes, Set<String> types) {
		Model result = null;
		
		String sceneFilter = createSceneFilter(scenes);
		String typeFilter = createTypeFilter(types);
		
		String query = URIconstants.QUERY_PREFIXES() + AnnotationQueries.QUERY_ANNOTATIONS_TEMPLATE;
		if ("".equals(mediaId)) {
			query = query.replaceFirst("\\?target oa:hasSource <<MEDIA>>.","");
		} else {
			query = query.replaceFirst("<<MEDIA>>", "<"+URIconstants.MEDIA_PREFIX() + mediaId + ">");
		}
		query = query.replaceFirst("<<SCENEFILTER>>", sceneFilter);
		query = query.replaceFirst("<<TYPEFILTER>>", typeFilter);
		
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
			logger.info("{} Query for mediaId ({}) scenes({}) types({})", "getAnnotations", mediaId, scenes, types);
			result = qexec.execDescribe();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - Query annotations - Triplestore query failed {}", "getAnnotations", msg);
			return null;
		}
		
		return result;
	}
	
	public Model textSearch(String searchTerm, boolean whole, Set<String> mediaIdSet, Set<String> typeSet) {
		Model result = null;
		
		String typeFilter = createTypeFilter(typeSet);
		String mediaFilter = createMediaFilter(mediaIdSet);
		String term = searchTerm;
		if (whole) {
			term = "\\\\\\\\b"+searchTerm+"\\\\\\\\b";
		}

		logger.info("{} Query for \"{}\" {} {} {}", "textSearch", searchTerm, (whole) ? "whole" : "substring", mediaIdSet, typeSet);

		String textFilter = "FILTER regex(?annotextvalue, \""+term+"\" ,\"i\")";
		
		String query = URIconstants.QUERY_PREFIXES() + AnnotationQueries.TEXT_SEARCH_TEMPLATE;
		query = query.replaceFirst("<<TEXTFILTER>>", textFilter);
		query = query.replaceFirst("<<TYPEFILTER>>", typeFilter);
		query = query.replaceFirst("<<MEDIAFILTER>>", mediaFilter);
		
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
			result = qexec.execDescribe();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - Text search annotations - Triplestore query failed {}", "textSearch", msg);
			return null;
		}
		
		return result;
	}
	
	private Set<AnnotationMetadata> getMatchingAnnotations(AnnotationMetadata annoCheck, Set<AnnotationMetadata> annoList) {
		Set<AnnotationMetadata> result = new HashSet<AnnotationMetadata>();
		
		for (AnnotationMetadata anno : annoList) {
			String id1 = annoCheck.getMediaId();
			String id2 = anno.getMediaId();
			if (id1 != null && id2 != null && id1.equals(id2)) {
				if (Interval.isThereOverlap(annoCheck.getInterval(), anno.getInterval())) {
					result.add(anno);
				}
			}
		}
		
		return result;
	}
	
	
	public Model valueSearch(Set<String> valueSet, Set<String> mediaIdSet) {
		Model result = null;

		logger.info("{} Query for {} {}", "valueSearch", valueSet, mediaIdSet);

		String mediaFilter = createMediaFilter(mediaIdSet);
		mediaFilter = mediaFilter.replace("?anno oa:hasTarget ?target. ?target oa:hasSource ?source.", "");
		
		SortedMap<String, Set<AnnotationMetadata>> resultSets = new TreeMap<String, Set<AnnotationMetadata>>();
		Set<AnnotationMetadata> tempMatchedAnnotations = new HashSet<AnnotationMetadata>();
		Set<AnnotationMetadata> finalMatchedAnnotations = new HashSet<AnnotationMetadata>();

		for (String values : valueSet) {

			String valueFilter = createValueFilter(values);
			String query = URIconstants.QUERY_PREFIXES() + AnnotationQueries.VALUE_SEARCH_SELECT_TEMPLATE;
			query = query.replaceFirst("<<VALUEFILTER>>", valueFilter);
			query = query.replaceFirst("<<MEDIAFILTER>>", mediaFilter);
			
			Set<AnnotationMetadata> annotations = new HashSet<AnnotationMetadata>();
			
	        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
				logger.info("{} - QUERY VALUE_SEARCH_SELECT_TEMPLATE {}", "valueSearch", values);
	        	ResultSet set = qexec.execSelect();
	        	while (set.hasNext()) {
					QuerySolution sol = set.next();
					String annoUri = sol.get("anno").asResource().getURI();
					Literal begin = sol.getLiteral("begin");
					Literal end = sol.getLiteral("end");
					
					AnnotationMetadata amd = new AnnotationMetadata(annoUri, new Interval(begin.getLong(), end.getLong()));
					annotations.add(amd);
	        	}
	        	
	        } catch (Exception e) {
				String msg = e.toString().replace("\n", " ");
				logger.error("{} - QUERY VALUE_SEARCH_SELECT_TEMPLATE - Triplestore query failed {} {}", "valueSearch", values, msg);
				return null;
			}
	        
	        resultSets.put(values, annotations);
			
		}
		
		if (resultSets.size() > 1) {
		
			// Record pairwise matches in annotation metadata
			String[] queryValues = resultSets.keySet().toArray(new String[resultSets.keySet().size()]);
			for (int i = 0; i < queryValues.length; i++) {
				Set<AnnotationMetadata> list1 = resultSets.get(queryValues[i]);
				for (int j = 0; j < queryValues.length; j++) {
					if (i != j) {
						Set<AnnotationMetadata> list2 = resultSets.get(queryValues[j]);					
						for (AnnotationMetadata anno : list1) {
							Set<AnnotationMetadata> matchedAnnotations = getMatchingAnnotations(anno, list2);
							if (matchedAnnotations.size() > 0) {
								tempMatchedAnnotations.add(anno);
								anno.searchValue = queryValues[i];
								anno.matches.put(queryValues[j], matchedAnnotations);
							}
						}
					}
				}
			}
	
			// Remove matched annotations that do not have matches in all categories
			Set<AnnotationMetadata> cleanedMatches = new HashSet<AnnotationMetadata>(tempMatchedAnnotations);
			for (AnnotationMetadata anno : tempMatchedAnnotations) {
				if (anno.matches.keySet().size() < valueSet.size()-1) {
					for (String matchSearchValue : anno.matches.keySet()) {
						for (AnnotationMetadata matchAnno : anno.matches.get(matchSearchValue)) {
							Set<AnnotationMetadata> backMatches = matchAnno.matches.get(anno.searchValue);
							backMatches.remove(anno);
							if (backMatches.size() == 0) {
								matchAnno.matches.remove(anno.searchValue);
							}
						}
					}
					cleanedMatches.remove(anno);
				}
			}
	
			// Re-check annotations after match removal
			for (AnnotationMetadata anno : cleanedMatches) {
				if (anno.matches.keySet().size() == valueSet.size()-1) {
					finalMatchedAnnotations.add(anno);
				}
			}
			
		} else {
			finalMatchedAnnotations = resultSets.get(resultSets.firstKey());
		}
		
		
   		int i = 0;
   		int numValue = finalMatchedAnnotations.size();
		String annoFilter = "FILTER (?anno IN ( ";
		for (AnnotationMetadata anno : finalMatchedAnnotations) {
			i++;
			annoFilter = annoFilter + "<"+anno.getAnnoUri()+">";
			if (i == numValue) {
				annoFilter = annoFilter + " ))";
			} else {
				annoFilter = annoFilter + ", ";
			}
		}

		String query = URIconstants.QUERY_PREFIXES() + AnnotationQueries.QUERY_ANNOTATIONS_TEMPLATE;
		query = query.replaceFirst("\\?target oa:hasSource <<MEDIA>>.","");
		query = query.replaceFirst("<<SCENEFILTER>>", "");
		query = query.replaceFirst("<<TYPEFILTER>>", annoFilter);
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
			logger.info("{} - QUERY_ANNOTATIONS_TEMPLATE - Annotations: {}", "valueSearch", numValue);
			result = qexec.execDescribe();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - QUERY_ANNOTATIONS_TEMPLATE - Triplestore query failed - Annotations: {} - {}", "valueSearch", numValue, msg);
			return null;
		}

		/*
		result = ModelFactory.createDefaultModel();

   		int i = 0;
   		int numValue = finalMatchedAnnotations.size();
   		int maxAnno = 100;
   		
   		String annoFilter = "FILTER (";
   		for (AnnotationMetadata anno : finalMatchedAnnotations) {
   			annoFilter = annoFilter + "?anno = <"+anno.getAnnoUri()+">";
   			i++;
   			if (i % maxAnno == 0 || i == numValue) {
   				annoFilter = annoFilter + " )";
   				String query = URIconstants.QUERY_PREFIXES + AnnotationQueries.QUERY_ANNOTATIONS_TEMPLATE;
   				query = query.replaceFirst("\\?target oa:hasSource <<MEDIA>>.","");
   				query = query.replaceFirst("<<SCENEFILTER>>", "");
   				query = query.replaceFirst("<<TYPEFILTER>>", annoFilter);
   				Model model = null;
   				System.out.println(query);
   				try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
   					model = qexec.execDescribe();
   				}
   				result.add(model);
   				annoFilter = "FILTER (";
   			} else {
   				annoFilter = annoFilter + " || ";
   			}
   		}
		*/
		
		return result;
	}
	
	public Map<String, Object> modelToFramedJsonld(Model model) {
    	Map<String, Object> result = null;
    	if (model != null ) {
			StringWriter queryResult = new StringWriter();
			RDFDataMgr.write(queryResult, model, RDFLanguages.JSONLD);
			if (queryResult.toString().length() > 0) {
				try {
					Object context = JsonUtils.fromString(JSONLDconstants.ANNOTATIONS_FULL_CONTEXT);
					Object frame = JsonUtils.fromString(JSONLDconstants.ANNOTATIONS_FULL_FRAME);
					Object data = JsonUtils.fromString(queryResult.toString());
					
					JsonLdOptions opts = new JsonLdOptions();
					opts.setPruneBlankNodeIdentifiers(true);
					
					result = JsonLdProcessor.compact(data, context, opts);
					if (!model.isEmpty()) {
						result = JsonLdProcessor.frame(result, frame, opts);
					}
				} catch (Exception e) {
					String msg = e.toString().replace("\n", " ");
					logger.error("{} - Conversion to jsonld failed  {}", "modelToFramedJsonld", msg);
					return null;
				}
			}
    	}    	
    	
    	return result;
    }
	
	/*
	private ArrayList<String> paginateQuery(StringWriter query) {
		ArrayList<String> result = new ArrayList<String>();
		
		String[] lines = query.toString().split("\n");
		
		int i = 0;
		StringBuilder sb = new StringBuilder();
		while (i < lines.length) {
			int length = sb.toString().length();
			if (length > AnnotationConstants.MAX_VIRTUOSO_QUERY_SIZE) {
				result.add(sb.toString());
				sb = new StringBuilder();
			} else {
				sb.append(lines[i]);
				sb.append("\n");
				i++;
			}
		}
		
		return result;
	}
	
	*/
	
	/*
	private ArrayList<String> paginateQuery(StringWriter query) {
		ArrayList<String> result = new ArrayList<String>();
		
		String[] lines = query.toString().split("\n");
		
		int i = 0;
		StringBuilder sb = new StringBuilder();
		while (i < lines.length) {
			int length = sb.toString().length();
			if (length > AnnotationConstants.MAX_VIRTUOSO_QUERY_SIZE) {
				result.add(sb.toString());
				sb = new StringBuilder();
			} else {
				sb.append(lines[i]);
				sb.append("\n");
				i++;
			}
		}
		
		return result;
	}
	
	*/
	
	private UpdateProcessor createUpdateProcessor(UpdateRequest request, boolean auth) {
		if (auth) {
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			Credentials credentials = new UsernamePasswordCredentials(sparqlUser, sparqlPassword);
			credsProvider.setCredentials(AuthScope.ANY, credentials);
			CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).build();
			return UpdateExecutionFactory.createRemote(request, sparqlAuthEndpoint, httpClient);
		} else {
			return UpdateExecutionFactory.createRemote(request, sparqlEndpoint);
		}
	}
	
	/*
	 * This methods splits annotation definitions in turtle syntax into separate strings 	
	 */
	private List<String> splitAnnotations(String turtleString) {
		List<String> result = new ArrayList<String>();
		
		Scanner scanner = new Scanner(turtleString);
		String anno = "";
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.startsWith("armid")) {
				if (!anno.isEmpty()) {
					result.add(anno);
				}
				anno = line;
			} else {
				if (!line.trim().isEmpty()) {
					anno = anno + line + "\n";
				}
			}
		}		
		if (!anno.isEmpty()) {
			result.add(anno);
		}		
		return result;
	}
	
	private String removePrefixes(String turtleString) {
		StringBuilder sb = new StringBuilder();
		Scanner scanner = new Scanner(turtleString);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (!line.startsWith("@")) {
				sb.append(line+"\n");
			}
		}		
		return sb.toString();
	}
	
	/*
	 * This method extracts prefix definitions from an RDF model and converts them to be used in a SPARQL query. 
	 */
	private String extractPrefixes(Model model) {
		return model.getNsPrefixMap().entrySet().stream().map(e -> "prefix "+e.getKey()+": <"+e.getValue()+">").collect(Collectors.joining("\n"));
	}
	
	private String submitUpdateRequestToTripleStore(UpdateRequest request) {
		try {
			logger.info("submitUpdateRequestToTripleStore - request size {}", request.toString().length());
			logger.debug("submitUpdateRequestToTripleStore - request - {}", request.toString());
			UpdateProcessor processor = createUpdateProcessor(request, true);
			processor.execute();
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("submitUpdateRequestToTripleStore - request failed - {}", msg);
			return "Triplestore update request failed. "+msg.replace("\"", "");
		}
	
		return null;
	}
	
	private String insertModelIntoTripleStore(Model model, String mediaId, String targetGraphUri) {
		
		// Usually we would insert a model using JENA Update Requests, but Virtuoso has bug since
		// several years that prevents us from inserting larger models.
		// See: https://stackoverflow.com/questions/16487746/jena-sparql-update-doesnt-execute
		
		/*
		StringWriter triples = new StringWriter();
		RDFDataMgr.write(triples, model, RDFFormat.NTRIPLES_UTF8);
		
		String update = "INSERT { GRAPH <"+targetGraphUri+"> {\n";
		update = update + triples.toString() +"\n";
		update = update + "} } WHERE {}";
		UpdateRequest request = UpdateFactory.create("CLEAR GRAPH <"+targetGraphUri+">");
		request.add(update);
		
		return submitUpdateRequestToTripleStore(request);
		
		*/
		
		/*
		 * Instead we upload the turtle model to our RDF upload service that does a bulk load via isql-v
		 */
		
		// Add a new namespace prefix to shorten turtle triples 
		model.setNsPrefix("armid", URIconstants.MEDIA_PREFIX()+mediaId+"/");
		
		StringWriter turtleWriter = new StringWriter();
		RDFDataMgr.write(turtleWriter, model, RDFFormat.TURTLE_PRETTY);
		String turtleString = turtleWriter.toString();
		
		UpdateRequest request = UpdateFactory.create("CLEAR GRAPH <"+targetGraphUri+">");
		submitUpdateRequestToTripleStore(request);

		try {
			CloseableHttpClient client = HttpClients.createDefault();
			
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addBinaryBody("file", turtleString.getBytes(), ContentType.APPLICATION_OCTET_STREAM, mediaId+".ttl");
			builder.addTextBody("graph", targetGraphUri);

			HttpUriRequest uriRequest = RequestBuilder.post()
					.setUri(Server.RDF_UPLOADER_URL)
					.addHeader(Server.API_TOKEN_HEADER_FIELD, Server.API_TOKEN)
					.setEntity(builder.build())
					.build();
			
			logger.info("insertModelIntoTripleStore - RDF uploader service call at "+Server.RDF_UPLOADER_URL);

			HttpResponse response = client.execute(uriRequest);
			int statusCode = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			
			String errormsg = null;
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode root = mapper.readTree(entity.getContent());
				if (root.has("error")) {
					JsonNode err = root.get("error");
					if (err.has("message")) {
						errormsg = err.get("message").asText();
					}
				}
			} catch (IOException e) {
				logger.error("insertModelIntoTripleStore - Response of RDF uploader could not be parsed. {}", IOUtils.toString(entity.getContent(), "UTF-8"));
				return "Response of RDF uploader could not be parsed.";
			}			
			
			if (statusCode != HttpStatus.SC_OK) {
				logger.error("insertModelIntoTripleStore - RDF uploader service call failed. Code: {} Msg: {}", statusCode, errormsg);
				return "RDF uploader service call failed. Code: "+statusCode+" Msg: "+errormsg;
			}
			
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("insertModelIntoTripleStore - RDF uploader service call failed - {}", msg);
			return "RDF uploader service call failed. "+msg.replace("\"", "");
		}
		
		return null;
		
		/*
		
		// Add a new namespace prefix to shorten turtle triples 
		model.setNsPrefix("armid", URIconstants.MEDIA_PREFIX()+mediaId+"/");
		
		StringWriter turtleWriter = new StringWriter();
		RDFDataMgr.write(turtleWriter, model, RDFFormat.TURTLE_PRETTY);
		String turtleString = turtleWriter.toString();
		String insertPrefixes = extractPrefixes(model);
		String annotationTurtleString = removePrefixes(turtleString);

//		try {
//			BufferedWriter out = new BufferedWriter(new FileWriter("d:\\hagt\\googledrive\\HPI\\ada\\advene_service\\shot_insert.ttl"));
//			out.write(annotationTurtleString);
//			out.close();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}

		if (!AnnotationConstants.USE_VIRTUOSO_QUERY_PAGINATION) {
			String update = insertPrefixes + "\n"; 
			update = update + "INSERT { GRAPH <"+targetGraphUri+"> {\n";
			update = update + annotationTurtleString.toString() +"\n";
			update = update + "} } WHERE {}";
			UpdateRequest request = UpdateFactory.create("CLEAR GRAPH <"+targetGraphUri+">");
			request.add(update);
			
			return submitUpdateRequestToTripleStore(request);
		} else {
			List<String> splitAnnotations = splitAnnotations(annotationTurtleString);
			System.out.println(splitAnnotations.size());
			UpdateRequest crequest = UpdateFactory.create("CLEAR GRAPH <"+targetGraphUri+">");
			String cresult = submitUpdateRequestToTripleStore(crequest);
			if (cresult != null) {
				return cresult;
			}

//			String update = insertPrefixes + "\n"; 
//			update = update + "INSERT { GRAPH <"+targetGraphUri+"> {\n";
//			update = update + splitAnnotations.get(0)+"\n" + splitAnnotations.get(1)+"\n";
//			update = update + "} } WHERE {}";
//			
//			System.out.println(update);
//			UpdateRequest request = UpdateFactory.create(update);
//			System.out.println(request.toString());
//			String uresult = submitUpdateRequestToTripleStore(request);

			String update = insertPrefixes + "\n"; 
			update = update + "INSERT { GRAPH <"+targetGraphUri+"> {\n";

			int i = 1;
			for (String anno : splitAnnotations) {
				if (i % AnnotationConstants.NUMBER_OF_ANNOTATIONS_PER_INSERT_QUERY == 0) {
					update = update + "} } WHERE {}";
					UpdateRequest request = UpdateFactory.create(update);
					String uresult = submitUpdateRequestToTripleStore(request);
					if (uresult != null) {
						return uresult;
					}
					update = insertPrefixes + "\n"; 
					update = update + "INSERT { GRAPH <"+targetGraphUri+"> {\n";
				} else {
					update = update + anno+"\n";
					i++;
				}
			}
			if (!update.isEmpty()) {
				update = update + "} } WHERE {}";
				UpdateRequest request = UpdateFactory.create(update);
				String uresult = submitUpdateRequestToTripleStore(request);
				if (uresult != null) {
					return uresult;
				}
			}
		}
		
		
		System.out.println(model.size());
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("d:\\hagt\\googledrive\\HPI\\ada\\advene_service\\shot_test.ttl"));
			
			StringWriter sw = new StringWriter();
			RDFDataMgr.write(sw, model, RDFFormat.TURTLE_PRETTY);
			
			out.write(sw.toString());
			
			out.close();
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		StringWriter triples = new StringWriter();
		RDFDataMgr.write(triples, model, RDFFormat.NTRIPLES_UTF8);
		
		String update = "INSERT { GRAPH <"+targetGraphUri+"> {\n";
		update = update + triples.toString() +"\n";
		update = update + "} } WHERE {}";
		UpdateRequest request = UpdateFactory.create("CLEAR GRAPH <"+targetGraphUri+">");
		request.add(update);

		*/
		
		/*
		
		// Split the insert query into parts because virtuoso truncates queries lager than ~220kb and just responds with 400 Bad Request
		// see also https://stackoverflow.com/questions/16487746/jena-sparql-update-doesnt-execute
		ArrayList<String> paginatedQuery = paginateQuery(queryString);
		
		logger.info("insertModelIntoTripleStore - paginatedQuery - number of queries {}", paginatedQuery.size());
	
		// Attention: The respective graph is first cleared and then the triples are inserted
		UpdateRequest clearRequest = UpdateFactory.create("CLEAR GRAPH <"+targetGraphUri+">");
		try {
			logger.info("insertModelIntoTripleStore - CLEAR GRAPH - {}", targetGraphUri);
			UpdateProcessor processor = UpdateExecutionFactory.createRemote(clearRequest, sparqlEndpoint);
			processor.execute();
		} catch (Exception e) {
			e.printStackTrace();
			String msg = e.toString().replace("\n", " ");
			logger.error("insertModelIntoTripleStore - UpdateProcessor - {}", msg);
			return "Triplestore clear graph failed.";
		}
		
		*/
	
	
		/*
		
		for (String query : paginatedQuery) {
			String update = "INSERT { GRAPH <"+targetGraphUri+"> {\n";
			update = update + query +"\n";
			update = update + "} } WHERE {}";
			UpdateRequest request = UpdateFactory.create(update);
			try {
				logger.info("insertModelIntoTripleStore - INSERT - size {}", request.toString().length());
				logger.debug("insertModelIntoTripleStore - INSERT - {}", request.toString());
				UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, sparqlEndpoint);
				processor.execute();
			} catch (Exception e) {
				e.printStackTrace();
				String msg = e.toString().replace("\n", " ");
				logger.error("insertModelIntoTripleStore - INSERT - {}", msg);
				return "Triplestore insert failed.";
			}
		}
		*/ 
		
	    /*
	     * Implementation with INSERT DATA
	     * 
		Node graph = NodeFactory.createURI(URIconstants.GRAPH_PREFIX() + record.getId() + GENERATED_SCENES_SUFFIX);
		UpdateRequest req = new UpdateRequest();
		QuadDataAcc acc = new QuadDataAcc();
		acc.setGraph(graph);
		
		sceneModel.listStatements().forEachRemaining(st -> {
			Triple t = st.asTriple();
			acc.addTriple(t);
		});
		
		req.add("CLEAR GRAPH <"+graph.toString()+">;");
		req.add(new UpdateDataInsert(acc));
		*/
	}
	
	public String insertGeneratedScene(MetadataRecord record) {
		logger.info("insertGeneratedScene for movie "+record.getId());
		
		String sceneAnnotation = AnnotationConstants.ANNOTATION_TEMPLATE();
		
		Date date = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		String dateFormatted = df.format(date);

		sceneAnnotation = sceneAnnotation.replace("XXMEDIAIDXX", record.getId());
		sceneAnnotation = sceneAnnotation.replace("XXDATETIMEXX", dateFormatted);
		sceneAnnotation = sceneAnnotation.replace("XXENDMS", Integer.toString(record.getDuration()));
        Float durfl = ((float)record.getDuration())/1000;
        sceneAnnotation = sceneAnnotation.replace("XXENDFLOAT", Float.toString(durfl));

        String graphUri = URIconstants.GRAPH_PREFIX() + record.getId() + URIconstants.GENERATED_SCENES_GRAPH_SUFFIX;

		String update = URIconstants.INSERT_PREFIXES() + "\n"; 
		update = update + "INSERT { GRAPH <"+graphUri+"> {\n";
		update = update + sceneAnnotation +"\n";
		update = update + "} } WHERE {}";
		UpdateRequest request = UpdateFactory.create("CLEAR GRAPH <"+graphUri+">");
		request.add(update);
		
		return submitUpdateRequestToTripleStore(request);
		
/*		
        Model sceneModel = ModelFactory.createDefaultModel();
        try {
        	sceneModel.read(IOUtils.toInputStream(sceneAnnotation, "UTF-8"), null, RDFLanguages.strLangTurtle);
		} catch (IOException e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("insertGeneratedScene - Conversion of generated scene to model failed. {}", msg);
			return "Conversion of generated scene to model failed.";
		}
        
        
        return insertModelIntoTripleStore(sceneModel, record.getId(), graphUri);
        
        */
		
	}
	
	private Map<String,Map<String,Interval>> retrieveSceneIntervals(Model modelWithScenes) {
		Map<String,Map<String,Interval>> movieSceneIntervals = new HashMap<String, Map<String,Interval>>();

		String queryScenes = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ALL_SCENES();
		QueryExecution qexec = null;
		if (modelWithScenes == null) {
			// Retrieve available scene annotations from triple store
			qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, queryScenes);	
		} else {
			// Retrieve available scene annotations from model
			qexec = QueryExecutionFactory.create(queryScenes, modelWithScenes);
		}

		try {
			logger.info("retrieveSceneIntervals - SPARQL - QUERY_ALL_SCENES");
        	ResultSet set = qexec.execSelect();
			while (set.hasNext()) {
				QuerySolution sol = set.next();
				Literal startTime = sol.getLiteral("startTime");
				Literal endTime = sol.getLiteral("endTime");
				Literal id = sol.getLiteral("id");
				String[] split = id.toString().split("/");
				String mediaid = split[0];
				String sceneid = split[1];
				
                Map<String, Interval> scenes = movieSceneIntervals.get(mediaid);
                if (scenes == null) {
                	scenes = new HashMap<String, Interval>();
                	movieSceneIntervals.put(mediaid, scenes);
                }
               	Interval i = new Interval(startTime.getLong(), endTime.getLong());
                scenes.put(sceneid.toString(), i);
			}

		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("retrieveSceneIntervals - QUERY_ALL_SCENES - query failed {}", msg);
			return null;
		}
		
		return movieSceneIntervals;
	}
	
	private Model matchScenes(Model annotationModel, Map<String,Map<String,Interval>> movieSceneIntervals) {

		Map<String,Map<String,Interval>> movieAnnotationIntervals = new HashMap<String, Map<String,Interval>>();
        
		String queryAnnotations = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ALL_SCENES().replaceFirst("\\?body ao:annotationType <"+URIconstants.RESOURCE_PREFIX()+"AnnotationType/Scene>.", "");
		try (QueryExecution qexec = QueryExecutionFactory.create(queryAnnotations, annotationModel)) {
			logger.info("matchScenes - SPARQL - QUERY_ALL_ANNOTATIONS");
        	ResultSet set = qexec.execSelect();
			while (set.hasNext()) {
				QuerySolution sol = set.next();
				Literal startTime = sol.getLiteral("startTime");
				Literal endTime = sol.getLiteral("endTime");
				Literal id = sol.getLiteral("id");
				String[] split = id.toString().split("/");
				String mediaid = split[0];
				String annoid = split[1];
				
                Map<String, Interval> annotations = movieAnnotationIntervals.get(mediaid);
                if (annotations == null) {
                	annotations = new HashMap<String, Interval>();
                	movieAnnotationIntervals.put(mediaid, annotations);
                }
               	Interval i = new Interval(startTime.getLong(), endTime.getLong());
               	annotations.put(annoid.toString(), i);

			}			
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("matchScenes - QUERY_ALL_ANNOTATIONS - Model query failed {}", msg);
			return null;
		}

        for (String mediaid : movieAnnotationIntervals.keySet()) {
        	Map<String, Interval> annotations = movieAnnotationIntervals.get(mediaid);
        	for (String annoid : annotations.keySet()) {
        		boolean annotationMatched = false;
        		Interval annoInterval = annotations.get(annoid);
        		Map<String, Interval> scenes = movieSceneIntervals.get(mediaid);
        		for (String sceneid : scenes.keySet()) {
        			Interval sceneInterval = scenes.get(sceneid);
        			if (Interval.isThereOverlapWithTolerance(annoInterval, sceneInterval)) {
        				annotationMatched = true;
        				Resource annoResource = annotationModel.getResource(URIconstants.MEDIA_PREFIX()+mediaid+"/"+annoid);
        	            Property sceneIdProp = annotationModel.getProperty(URIconstants.ONTOLOGY_PREFIX()+"sceneId");
        				Statement stmt = annotationModel.createStatement(annoResource, sceneIdProp, sceneid);
        				annotationModel.add(stmt);
        			}
				}
        		if (!annotationMatched) {
        			logger.error("matchScenes - Cannot find a suitable scene for annotation {}", mediaid+"/"+annoid);
        		}
			}
		}

		return annotationModel;
	}
	
	
	// TODO unify insert methods
	public String insertAnnotations(String mediaId, String extractor, InputStream content) {
		logger.info("insertAnnotations for movie "+mediaId+", extractor "+extractor);
		
		Model model = ModelFactory.createDefaultModel();
		try {
			model.read(content, "", RDFLanguages.strLangTurtle);
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - RDF data could not be loaded into a model. {}", "insertAnnotations", msg);
			return "Uploaded RDF data could not be loaded into a model.";
		}
		
		// Retrieve existing scene information from triple store
		Map<String, Map<String, Interval>> sceneIntervals = retrieveSceneIntervals(null);
		if (sceneIntervals == null) {
			return "Querying for existing scenes failed.";
		}
		if (sceneIntervals.size() == 0) {
			logger.error("insertAnnotations - no scenes found in triple store.");
			return "No scenes found in triple store.";
		}
		
		Model matchedModel = matchScenes(model, sceneIntervals);
		if (matchedModel == null) {
			logger.error("insertAnnotations - matchScenes failed");
			return "Scene matching for uploaded RDF data failed.";
		}
		
        String graphUri = URIconstants.GRAPH_PREFIX() + mediaId + "/" +extractor;
        
        return insertModelIntoTripleStore(matchedModel, mediaId, graphUri);
        
	}

	public String insertAdveneResult(String mediaId, InputStream content) {
		logger.info("insertAdveneResult for movie "+mediaId);
		
		Model model = ModelFactory.createDefaultModel();
		try {
			model.read(content, "", RDFLanguages.strLangJSONLD);
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - JSON-LD data could not be loaded into a model. {}", "insertAdveneResult", msg);
			return "JSON-LD data could not be loaded into a model.";
		}

		// Retrieve existing scene information from uploaded model
		Map<String, Map<String, Interval>> sceneIntervals = retrieveSceneIntervals(model);
		if (sceneIntervals == null) {
			return "Querying for existing scenes failed.";
		}
		if (sceneIntervals.size() == 0) {
			logger.error("insertAdveneResult - no scenes found in model.");
			return "No scenes found in model.";
		}
		
		Model matchedModel = matchScenes(model, sceneIntervals);
		if (matchedModel == null) {
			logger.error("insertAnnotations - matchScenes failed");
			return "Scene matching for uploaded Advene package failed.";
		}

        String graphUri = URIconstants.GRAPH_PREFIX() + mediaId + URIconstants.MANUAL_ANNOTATIONS_GRAPH_SUFFIX;

        return insertModelIntoTripleStore(matchedModel, mediaId, graphUri);
		
	}
}
