package de.ada.restapi;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.client.CloseableHttpClient;
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetadataManager {

	private String sparqlEndpoint;
	private String sparqlAuthEndpoint;
	private String sparqlUser;
	private String sparqlPassword;
	
	private static MetadataManager instance;

	private static final Logger logger = LoggerFactory.getLogger(MetadataManager.class);

	private MetadataManager(String endpoint, String authEndpoint, String user, String password) {
		this.sparqlEndpoint = endpoint;
		this.sparqlAuthEndpoint = authEndpoint;
		this.sparqlUser = user;
		this.sparqlPassword = password;
	}

	public static MetadataManager getInstance(String endpoint, String authEndpoint, String user, String password) {
		if (instance == null) {
			instance = new MetadataManager(endpoint, authEndpoint, user, password);
		}

		return instance;
	}

	public boolean isMandatoryFieldMissing(MetadataRecord record) {
		return record.getId() == null || record.getRuntime() == null || record.getDuration() == null
				|| record.getTitle() == null || record.getYear() == null || record.getCategory() == null
				|| record.getPlayoutUrl() == null;
	}
	
	public Node createURI(String value) {
		if (value == null || value.contains(" ")) {
			return null;
		}
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

	public String deleteMedia(String mediaId) {
		logger.info("deleteMedia - "+mediaId);
		Set<String> graphsToDelete = new HashSet<String>();
		graphsToDelete.add(URIconstants.GRAPH_PREFIX() + mediaId + URIconstants.METADATA_GRAPH_SUFFIX);
		graphsToDelete.add(URIconstants.GRAPH_PREFIX() + mediaId + URIconstants.GENERATED_SCENES_GRAPH_SUFFIX);
		graphsToDelete.add(URIconstants.GRAPH_PREFIX() + mediaId + URIconstants.MANUAL_ANNOTATIONS_GRAPH_SUFFIX);
		
		for (String extractor : MetadataConstants.EXTRACTORS) {
			graphsToDelete.add(URIconstants.GRAPH_PREFIX() + mediaId + "/" + extractor);
		}
		
		UpdateRequest request = new UpdateRequest();
		for (String graph : graphsToDelete) {
			request.add("CLEAR GRAPH <"+graph+">;");
		}
		
		AnnotationManager am = AnnotationManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, sparqlUser, sparqlPassword);
		String result = am.submitUpdateRequestToTripleStore(request);
		if (result != null) {
			return "Triplestore clear graph failed. "+result;
		}
		
		return null;
		
	}

	public String addUpdateMedia(MetadataRecord record, boolean update) {
		
		Node graph = NodeFactory.createURI(URIconstants.GRAPH_PREFIX() + record.getId() + URIconstants.METADATA_GRAPH_SUFFIX);
		String mediaUri = URIconstants.MEDIA_PREFIX() + record.getId();
		Node mediaNode = NodeFactory.createURI(mediaUri);
		
		// Create a new short identifier in case metadata record is created the first time
		if (record.getShortId() == null) {
			logger.info("addUpdateMedia - QUERY_MAX_SHORTID");
			
			CloseableHttpClient httpClient = Server.getSslReadyHttpClient();
			if (httpClient == null) {
				logger.error("addUpdateMedia - httpClient could not be created.");
				return "httpClient could not be created.";
			}

			String queryMax = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_MAX_MOVIE_SHORTID();
			Integer maxShortId = 0;
	        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, queryMax, httpClient)) {
	        	logger.debug("addUpdateMedia - SPARQL QUERY {}", queryMax);
	        	ResultSet set = qexec.execSelect();
	        	if (set.hasNext()) {
	        		QuerySolution next = set.next();
	        		Literal maxLit = next.getLiteral("maxShortid");
	        		if (maxLit != null) {
	        			maxShortId = maxLit.getInt();
	        		}
	        	}
	        	qexec.close();
	        } catch (Exception e) {
				String msg = e.toString().replace("\n", " ");
				logger.error("addUpdateMedia - QUERY_MAX_SHORTID - Triplestore query failed {}", msg);
				return "Triplestore query for max movie short id failed.";
			} finally {
				if (httpClient != null) {
					try {
						httpClient.close();
					} catch (IOException e) {
						String msg = e.toString().replace("\n", " ");
						logger.error("addUpdateMedia - HTTP Connection to triplestore could not be closed. {}", msg);
						return "HTTP Connection to triplestore could not be closed. "+msg.replace("\"", "");
					}
				}
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

		AnnotationManager am = AnnotationManager.getInstance(sparqlEndpoint, sparqlAuthEndpoint, sparqlUser, sparqlPassword);
		String result = am.submitUpdateRequestToTripleStore(request);
		if (result != null) {
			return "Triplestore metadata update failed. "+result;
		}
		
		if (!update) {
			return am.insertGeneratedScene(record);
		} else {
			return null;
		}

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
	
	public String exportMovieMetadata() {
		CloseableHttpClient httpClient = Server.getSslReadyHttpClient();
		if (httpClient == null) {
			logger.error("exportMovieMetadata - httpClient could not be created.");
			return null;
		}
		
		List<Map<String, Object>> graphResult = null;
		
		String query = "SELECT DISTINCT ?g WHERE {GRAPH ?g {?s a ?o}}";
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query, httpClient)) {        	
			logger.info("exportMovieMetadata - SPARQL - QUERY GRAPHS");
        	logger.debug("exportMovieMetadata - SPARQL QUERY {}", query);
        	ResultSet set = qexec.execSelect();
        	graphResult = convertResultSetToListOfMaps(set);
        	qexec.close();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("exportMovieMetadata - QUERY GRAPHS - Triplestore query failed {}", msg);
			return null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					String msg = e.toString().replace("\n", " ");
					logger.error("exportMovieMetadata - HTTP Connection to triplestore could not be closed. {}", msg);
					return null;
				}
			}
		}

        if (graphResult == null || graphResult.size() == 0) {
        	return "";
        }

        
        Set<String> metaGraphs = new HashSet<String>();
    	for (Map<String, Object> map : graphResult) {
    		String g = (String)map.get("g");
    		if (g != null && g.endsWith("/meta")) {
    			metaGraphs.add(g);
    		}
		}
        
        if (metaGraphs.size() == 0) {
        	return "";
        }
        
        String metaQuery = "describe * ";
        for (String graph : metaGraphs) {
        	metaQuery += "FROM <"+graph+"> ";
		}
        metaQuery += "WHERE {?s ?p ?o.}";
        
        httpClient = Server.getSslReadyHttpClient();
		if (httpClient == null) {
			logger.error("exportMovieMetadata - httpClient could not be created.");
			return null;
		}
		
		Model metaResult = null;
        
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, metaQuery, httpClient)) {
			metaResult = qexec.execDescribe();
        	qexec.close();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("exportMovieMetadata - QUERY META FROM GRAPHS - Triplestore query failed {}", msg);
			return null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					String msg = e.toString().replace("\n", " ");
					logger.error("exportMovieMetadata - HTTP Connection to triplestore could not be closed. {}", msg);
					return null;
				}
			}
		}
		
		if (metaResult.size() > 0) {
			StringWriter queryResult = new StringWriter();
			RDFDataMgr.write(queryResult, metaResult, RDFLanguages.TURTLE);
			return queryResult.toString();
		} else {
			return "";
		}
}

	public List<Map<String, Object>> getMovieMetadata(String queryId) {
		if (queryId == null) {
			logger.info("getMovieMetadata - all");
		} else {
			logger.info("getMovieMetadata - queryId " + queryId);
		}
		
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> countres = new ArrayList<Map<String,Object>>();
		Map<Object,Map<Object,Object>> counts = new HashMap<Object, Map<Object,Object>>();
		Map<Object,Integer> totalCounts = new HashMap<Object, Integer>();
		
		List<Map<String, Object>> sceneResult = new ArrayList<Map<String,Object>>();
		Map<String,List<Map<String, Object>>> scenes = new HashMap<String, List<Map<String,Object>>>();
		
		CloseableHttpClient httpClient = Server.getSslReadyHttpClient();
		if (httpClient == null) {
			logger.error("getMovieMetadata - httpClient could not be created.");
			return null;
		}

		String queryScenes = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ALL_SCENES();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, queryScenes, httpClient)) {        	
			logger.info("getMovieMetadata - SPARQL - QUERY_ALL_SCENES");
        	logger.debug("getMovieMetadata - SPARQL QUERY {}", queryScenes);
        	ResultSet set = qexec.execSelect();
        	sceneResult = convertResultSetToListOfMaps(set);
        	qexec.close();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("getMovieMetadata - QUERY_ALL_SCENES - Triplestore query failed {}", msg);
			return null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					String msg = e.toString().replace("\n", " ");
					logger.error("getMovieMetadata - HTTP Connection to triplestore could not be closed. {}", msg);
					return null;
				}
			}
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

		httpClient = Server.getSslReadyHttpClient();
		if (httpClient == null) {
			logger.error("getMovieMetadata - httpClient could not be created.");
			return null;
		}

		String queryCounts = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ANNOTATION_TYPE_COUNTS();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, queryCounts, httpClient)) {
			logger.info("getMovieMetadata - SPARQL - QUERY_ANNOTATION_TYPE_COUNTS");
			logger.debug("getMovieMetadata - SPARQL QUERY {}", queryCounts);
        	ResultSet set = qexec.execSelect();
        	countres = convertResultSetToListOfMaps(set);
        	qexec.close();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("getMovieMetadata - QUERY_ANNOTATION_TYPE_COUNTS - Triplestore query failed {}", msg);
			return null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					String msg = e.toString().replace("\n", " ");
					logger.error("getMovieMetadata - HTTP Connection to triplestore could not be closed. {}", msg);
					return null;
				}
			}
		}
        
        for (Map<String, Object> co : countres) {
//        	Object source = co.get("id");
        	Object source = co.get("source");
        	Object annotype = co.get("annotype");
        	Object count = co.get("count");
        	
        	Map<Object, Object> map = counts.get(source);
        	if (map == null) {
        		map = new HashMap<Object, Object>();
        		counts.put(source, map);
        	}
        	map.put(annotype, count);
        	Integer total = totalCounts.get(source);
        	if (total == null) {
        		total = (Integer)count;
        	} else {
        		total = total + (Integer)count;
        	}
    		totalCounts.put(source, total);
		}
        
		httpClient = Server.getSslReadyHttpClient();
		if (httpClient == null) {
			logger.error("getMovieMetadata - httpClient could not be created.");
			return null;
		}
        
        List<Map<String, Object>> tmpResult = null;
		String query = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_METADATA();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query, httpClient)) {        	
			logger.info("getMovieMetadata - SPARQL - QUERY_METADATA");
			logger.debug("getMovieMetadata - SPARQL QUERY {}", query);
        	ResultSet set = qexec.execSelect();        	
        	tmpResult = convertResultSetToListOfMaps(set);
        	qexec.close();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - QUERY_METADATA - Triplestore query failed {}", "getMovieMetadata", msg);
			return null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					String msg = e.toString().replace("\n", " ");
					logger.error("getMovieMetadata - HTTP Connection to triplestore could not be closed. {}", msg);
					return null;
				}
			}
		}
        
		for (Map<String, Object> map : tmpResult) {
			String mediaId  = ((String)map.get("id"));
			String mediaUri  = ((String)map.get("mediauri"));
			if (queryId == null || mediaId.equals(queryId)) {
				Map<Object, Object> typeCounts = counts.get(mediaUri);
				map.put("typeCounts", typeCounts);
				
				List<Map<String, Object>> sceneList = scenes.get(mediaId);
				map.put("scenes", sceneList);
				result.add(map);
				
				Integer annotationsTotal = totalCounts.get(mediaUri);
				if (annotationsTotal == null) {
					map.put("annotationsTotal", 0);
				} else {
					map.put("annotationsTotal", annotationsTotal);
				}
			}
		}

		return result;
	}
	
	public List<Map<String, Object>> getOntology() {
		List<Map<String, Object>> annotationLevels = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> annotationTypes = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> annotationValues = new ArrayList<Map<String,Object>>();
		
		List<Map<String, Object>> tempResult = new ArrayList<Map<String,Object>>();
		
		CloseableHttpClient httpClient = Server.getSslReadyHttpClient();
		if (httpClient == null) {
			logger.error("getOntology - httpClient could not be created.");
			return null;
		}

		String query = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ONTOLOGY_ELEMENTS();
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query, httpClient)) {
			logger.info("getOntology - SPARQL - QUERY_ONTOLOGY_ELEMENTS");
			logger.debug("getOntology - SPARQL QUERY {}", query);
            ResultSet set = qexec.execSelect();
            tempResult = convertResultSetToListOfMaps(set);
        	qexec.close();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - QUERY_ONTOLOGY_ELEMENTS - Triplestore query failed {}", "getOntology", msg);
			return null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					String msg = e.toString().replace("\n", " ");
					logger.error("getOntology - HTTP Connection to triplestore could not be closed. {}", msg);
					return null;
				}
			}
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
        
		httpClient = Server.getSslReadyHttpClient();
		if (httpClient == null) {
			logger.error("getOntology - httpClient could not be created.");
			return null;
		}

		query = URIconstants.QUERY_PREFIXES() + MetadataQueries.QUERY_ONTOLOGY_LINKS();
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query, httpClient)) {
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
        	qexec.close();
        } catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - QUERY_ONTOLOGY_LINKS - Triplestore query failed {}", "getOntology", msg);
			return null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					String msg = e.toString().replace("\n", " ");
					logger.error("getOntology - HTTP Connection to triplestore could not be closed. {}", msg);
					return null;
				}
			}
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
