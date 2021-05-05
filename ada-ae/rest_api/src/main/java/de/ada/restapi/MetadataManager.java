package de.ada.restapi;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.XSDBaseNumericType;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetadataManager {

	private final String sparqlEndpoint;
	private final String sparqlAuthEndpoint;
	private final String sparqlUser;
	private final String sparqlPassword;
	private static MetadataManager instance;

	final Logger logger;

	private MetadataManager(String endpoint, String authEndpoint, String user, String password) {
		this.sparqlEndpoint = endpoint;
		this.sparqlAuthEndpoint = authEndpoint;
		this.sparqlUser = user;
		this.sparqlPassword = password;
		logger = LoggerFactory.getLogger(MetadataManager.class);
	}

	public static MetadataManager getInstance(String endpoint, String authEndpoint, String user, String password) {
		if (instance == null) {
			instance = new MetadataManager(endpoint, authEndpoint, user, password);
		}

		return instance;
	}

	public boolean validateMissing(MetadataRecord record) {
		return record.getId() == null || record.getRuntime() == null || record.getDuration() == null
				|| record.getTitle() == null || record.getYear() == null || record.getCategory() == null
				|| record.getPlayoutUrl() == null;
	}
	
	/*
	public String clearGraph(String graphURI) {
		logger.info("clearGraph - {}", graphURI);
		
		UpdateRequest request = new UpdateRequest();
		request.add("CLEAR GRAPH <"+graphURI+">;");
		UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, sparqlEndpoint);
		
		try {
			logger.debug("clearGraph - SPARQL UPDATE {}", request.toString());
			processor.execute();
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("clearGraph - UpdateProcessor - Exception {}", msg);
			return "Triplestore clear graph failed.";
//			msg = msg.replace("\"", "");
//			return "{\"error\": {\"message\": \"TripleStore delete metadata graph failed .\",\"code\": 500,\"cause\": \"" + msg
//					+ "\"}}";
		}

		return null;
	}
	*/
	public Node createURI(String value) {
		Node object = null;
		try {
			URL u = new URL((String)value);
			if (u.getProtocol().startsWith("http")) {
				object = NodeFactory.createURI((String)value);	
			}
		} catch (Exception e) {
		}
		
		return object;
	}
	
	// TODO refactor to reuse in AnnotationManager
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
	
	public String deleteMedia(String mediaId) {
		logger.info("deleteMedia - "+mediaId);
		Set<String> graphsToDelete = new HashSet<String>();
		graphsToDelete.add(URIconstants.GRAPH_PREFIX() + mediaId + URIconstants.METADATA_GRAPH_SUFFIX);
		graphsToDelete.add(URIconstants.GRAPH_PREFIX() + mediaId + URIconstants.GENERATED_SCENES_GRAPH_SUFFIX);
		for (Map<String, Object> type : MetadataConstants.AUTOMATED_ANALYSIS_TYPES) {
			String fullid = (String)type.get("id");
			String extractorid = fullid.split("/")[1];
			graphsToDelete.add(URIconstants.GRAPH_PREFIX() + mediaId + "/" + extractorid);
		}

		UpdateRequest request = new UpdateRequest();
		for (String graph : graphsToDelete) {
			request.add("CLEAR GRAPH <"+graph+">;");
		}
		
		
		try {
			logger.info("deleteMedia - clear graphs - size "+graphsToDelete.size());
			logger.debug("deleteMedia - SPARQL UPDATE {}", request.toString());
			UpdateProcessor processor = createUpdateProcessor(request, true);
			processor.execute();
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("clearGraph - UpdateProcessor - Exception {}", msg);
			return "Triplestore clear graph failed.";
		}
		
		return null;
		
	}

	public String addMedia(MetadataRecord record) {
		
		Node graph = NodeFactory.createURI(URIconstants.GRAPH_PREFIX() + record.getId() + URIconstants.METADATA_GRAPH_SUFFIX);
		String mediaUri = URIconstants.MEDIA_PREFIX() + record.getId();
		Node mediaNode = NodeFactory.createURI(mediaUri);

		// Create a new short identifier in case metadata record is initially created
		if (record.getShortId() == null) {
			logger.info("addMedia - QUERY_MAX_SHORTID");
			String queryMax = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_MAX_MOVIE_SHORTID();
			Integer maxShortId = 0;
	        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, queryMax)) {
	        	logger.debug("addMedia - SPARQL QUERY {}", queryMax);
	        	ResultSet set = qexec.execSelect();
	        	if (set.hasNext()) {
	        		QuerySolution next = set.next();
	        		Literal maxLit = next.getLiteral("maxShortid");
	        		if (maxLit != null) {
	        			maxShortId = maxLit.getInt();
	        		}
	        	}
	        } catch (Exception e) {
				String msg = e.toString().replace("\n", " ");
				logger.error("addMedia - QUERY_MAX_SHORTID - Triplestore query failed {}", msg);
				return "Triplestore query for max movie short id failed.";
//				msg = msg.replace("\"", "");
//				return "{\"error\": {\"message\": \"Triplestore query for max movie short id failed .\",\"code\": 500,\"cause\": \"" + msg
//						+ "\"}}";
			}
	        
	        record.setShortId(maxShortId+1);
		}

        ObjectMapper om = new ObjectMapper();
		Map<String, Object> propertyValueMapping = null;

		try {
			propertyValueMapping = om.convertValue(record, new TypeReference<Map<String, Object>>(){});
		} catch (IllegalArgumentException e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("addMedia - ObjectMapper - convertValue - {}", msg);
			return "MetadataRecord conversion failed.";
//			msg = msg.replace("\"", "");
//			return "{\"error\": {\"message\": \"MetadataRecord conversion failed .\",\"code\": 500,\"cause\": \"" + msg
//					+ "\"}}";
		}
		
		UpdateRequest request = new UpdateRequest();
		QuadDataAcc acc = new QuadDataAcc();
		acc.setGraph(graph);
		
		for (String property : propertyValueMapping.keySet()) {
			Object value = propertyValueMapping.get(property);
			
			if (value == null) {
				continue;
			}
			
			Node object = null;
			if (value instanceof String) {
				object = createURI((String)value);
			}
			
			if (object == null) {
				if (value instanceof Integer) {
					object = NodeFactory.createLiteralByValue(value, XSDBaseNumericType.XSDinteger);
				} else {
					object = NodeFactory.createLiteralByValue(value, XSDDatatype.XSDstring);
				}
			}
			
			Node propertyNode = NodeFactory.createURI(property);
			acc.addTriple(new Triple(mediaNode, propertyNode, object));
			
		}

		request.add("CLEAR GRAPH <"+graph.getURI()+">;");
		request.add(new UpdateDataInsert(acc));		
		
		try {
			logger.info("addMedia - SPARQL UPDATE - size {}", request.toString().length());
			logger.debug("addMedia - SPARQL UPDATE - {}", request.toString());
			UpdateProcessor processor = createUpdateProcessor(request, true);
			processor.execute();
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("addMedia - insert - UpdateProcessor - Exception - {}", msg);
			return "Triplestore metadata update failed.";
		}
		
		AnnotationManager am = AnnotationManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, sparqlUser, sparqlPassword);
		return am.insertGeneratedScene(record);

	}

	private List<Map<String, Object>> convertResultSetToListOfMaps(ResultSet set) {
		final List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();

		set.forEachRemaining(qsol -> {
			Map<String, Object> row = new HashMap<String,Object>();
			set.getResultVars().forEach(var -> {
    			RDFNode rdfNode = qsol.get(var);
    			if (rdfNode == null) {
    				row.put(var, null);
    			} else if (rdfNode.isLiteral()) {
    				Literal lit = rdfNode.asLiteral();
    				row.put(var, lit.getValue());
    			} else if (rdfNode.isResource()) {
    				Resource res = rdfNode.asResource();
    				row.put(var, res.getURI());
    			}
    		});
			result.add(row);
    	});
		logger.info("convertResultSetToListOfMaps - Entries processed: "+result.size());
		return result;
	}

	public List<Map<String, Object>> getMovieMetadata(String queryId) {
		logger.info("getMovieMetadata - " + queryId == null ? "all" : "id "+queryId);
		
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> countres = new ArrayList<Map<String,Object>>();
		Map<Object,Map<Object,Object>> counts = new HashMap<Object, Map<Object,Object>>(); 
		
		List<Map<String, Object>> sceneResult = new ArrayList<Map<String,Object>>();
		Map<String,List<Map<String, Object>>> scenes = new HashMap<String, List<Map<String,Object>>>();
		
		String queryScenes = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ALL_SCENES();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, queryScenes)) {        	
			logger.info("getMovieMetadata - SPARQL - QUERY_ALL_SCENES");
        	logger.debug("getMovieMetadata - SPARQL QUERY {}", queryScenes);
        	ResultSet set = qexec.execSelect();
        	sceneResult = convertResultSetToListOfMaps(set);
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("getMovieMetadata - QUERY_ALL_SCENES - Triplestore query failed {}", msg);
			return null;
		}
        for (Map<String, Object> scene : sceneResult) {
        	String id = (String)scene.get("id");
        	String mediaId = id.split("/")[0];
        	String sceneId = id.split("/")[1];
        	List<Map<String, Object>> list = scenes.get(mediaId);
        	if (list == null) {
        		list = new ArrayList<Map<String,Object>>();
        		scenes.put(mediaId, list);
        	}
        	scene.put("id", sceneId);
        	list.add(scene);
        }

		String queryCounts = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ANNOTATION_TYPE_COUNTS();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, queryCounts)) {
			logger.info("getMovieMetadata - SPARQL - QUERY_ANNOTATION_TYPE_COUNTS");
			logger.debug("getMovieMetadata - SPARQL QUERY {}", queryCounts);
        	ResultSet set = qexec.execSelect();
        	countres = convertResultSetToListOfMaps(set);
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("getMovieMetadata - QUERY_ANNOTATION_TYPE_COUNTS - Triplestore query failed {}", msg);
			return null;
		}
        
        for (Map<String, Object> co : countres) {
        	Object source = co.get("id");
        	Object annotype = co.get("annotype");
        	Object count = co.get("count");
        	
        	Map<Object, Object> map = counts.get(source);
        	if (map == null) {
        		map = new HashMap<Object, Object>();
        		counts.put(source, map);
        	}
        	map.put(annotype, count);
		}
        
        List<Map<String, Object>> tmpResult = null;
		String query = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_METADATA();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {        	
			logger.info("getMovieMetadata - SPARQL - QUERY_METADATA");
			logger.debug("getMovieMetadata - SPARQL QUERY {}", query);
        	ResultSet set = qexec.execSelect();        	
        	tmpResult = convertResultSetToListOfMaps(set);
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - QUERY_METADATA - Triplestore query failed {}", "getMovieMetadata", msg);
			return null;
		}
        
		for (Map<String, Object> map : tmpResult) {
			String mediaId  = ((String)map.get("id"));
			if (queryId == null || mediaId.equals(queryId)) {
				Map<Object, Object> typeCounts = counts.get(mediaId);
				map.put("typeCounts", typeCounts);
				
				List<Map<String, Object>> sceneList = scenes.get(mediaId);
				map.put("scenes", sceneList);
				result.add(map);
			}
		}

		return result;
	}
	
	public List<Map<String, Object>> getOntology() {
		List<Map<String, Object>> annotationLevels = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> annotationTypes = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> annotationValues = new ArrayList<Map<String,Object>>();
		
		List<Map<String, Object>> tempResult = new ArrayList<Map<String,Object>>();

		String query = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ONTOLOGY_ELEMENTS();
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
			logger.info("getOntology - SPARQL - QUERY_ONTOLOGY_ELEMENTS");
			logger.debug("getOntology - SPARQL QUERY {}", query);
            ResultSet set = qexec.execSelect();
            tempResult = convertResultSetToListOfMaps(set);
            
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - QUERY_ONTOLOGY_ELEMENTS - Triplestore query failed {}", "getOntology", msg);
			return null;
		}
		
        for (Map<String, Object> map : tempResult) {
        	String id = (String)map.get("id");
        	
        	if (id.startsWith("AnnotationLevel")) {
        		annotationLevels.add(map);
        	}
        	if (id.startsWith("AnnotationType")) {
        		annotationTypes.add(map);
        	}
        	if (id.startsWith("AnnotationValue")) {
        		annotationValues.add(map);
        	}
		}
        
        Map<String, Set<String>> subElements = new HashMap<String, Set<String>>();
        
		query = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ONTOLOGY_LINKS();
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
			logger.info("getOntology - SPARQL - QUERY_ONTOLOGY_LINKS");
			logger.debug("getOntology - SPARQL QUERY {}", query);
            ResultSet set = qexec.execSelect();
            while (set.hasNext()) {
                QuerySolution sol = set.next();
                RDFNode elemNode = sol.get("elementUri");
                Literal sub = sol.getLiteral("subElements");
                
                String[] split = sub.toString().split(";");
                
                Set<String> subs = Set.of(split);
                
                subElements.put(elemNode.asResource().getURI(), subs);
                
            }
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - QUERY_ONTOLOGY_LINKS - Triplestore query failed {}", "getOntology", msg);
			return null;
		}
		
		for (Map<String, Object> level : annotationLevels) {
			String levelUri = (String)level.get("elementUri");
			Set<String> subs = subElements.get(levelUri);
			if (subs != null) {
				List<Map<String, Object>> subList = new ArrayList<Map<String,Object>>();
				
				for (Map<String, Object> type : annotationTypes) {
					String typeUri = (String)type.get("elementUri");
					if (subs.contains(typeUri)) {
						subList.add(type);
					}
				}
				level.put("subElements", subList);
			} else {
				level.put("subElements", null);
			}
		}

		for (Map<String, Object> type : annotationTypes) {
			String typeUri = (String)type.get("elementUri");
			Set<String> subs = subElements.get(typeUri);
			if (subs != null) {
				List<Map<String, Object>> subList = new ArrayList<Map<String,Object>>();
				
				for (Map<String, Object> value : annotationValues) {
					String valueUri = (String)value.get("elementUri");
					if (subs.contains(valueUri)) {
						subList.add(value);
					}
				}
				type.put("subElements", subList);
				Float maxNumericValue = null;
				for (Map<String, Object> value : subList) {
					Float numVal = (Float)value.get("elementNumericValue");
					if ( numVal != null) {
						if (maxNumericValue == null) {
							maxNumericValue = (float) -1;
						}
						if (numVal > maxNumericValue) {
							maxNumericValue = numVal;
						}
					}
				}
				type.put("maxNumericValue", maxNumericValue);
			} else {
				type.put("subElements", null);
			}
		}
		
		annotationLevels.add(MetadataConstants.AUTOMATED_ANALYSIS_LEVEL);

		return annotationLevels;
		
	}

}
