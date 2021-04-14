package de.ada.restapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

public class AnnotationManager {

	public static final String GENERATED_SCENES_SUFFIX = "/scenegen";

	private final String sparqlEndpoint;
	private static AnnotationManager instance;

	final Logger logger;

	private AnnotationManager(String endpoint) {
		this.sparqlEndpoint = endpoint;
		logger = LoggerFactory.getLogger(AnnotationManager.class);
	}

	public static AnnotationManager getInstance(String endpoint) {
		if (instance == null) {
			instance = new AnnotationManager(endpoint);
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
	
	public String insertGeneratedScene(MetadataRecord record) {
		logger.info("insertGeneratedScene for movie "+record.getId());
		
		String sceneAnnotation = AnnotationConstants.ANNOTATION_PREFIXES()+AnnotationConstants.ANNOTATION_TEMPLATE();
		
		Date date = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		String dateFormatted = df.format(date);

		sceneAnnotation = sceneAnnotation.replace("XXMEDIAIDXX", record.getId());
		sceneAnnotation = sceneAnnotation.replace("XXDATETIMEXX", dateFormatted);
		sceneAnnotation = sceneAnnotation.replace("XXENDMS", Integer.toString(record.getDuration()));
        Float durfl = ((float)record.getDuration())/1000;
        sceneAnnotation = sceneAnnotation.replace("XXENDFLOAT", Float.toString(durfl));
		
        Model sceneModel = ModelFactory.createDefaultModel();
        try {
        	sceneModel.read(IOUtils.toInputStream(sceneAnnotation, "UTF-8"), null, RDFLanguages.strLangTurtle);
		} catch (IOException e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("insertGeneratedScene - Conversion of generated scene to model failed. {}", msg);
			return "{\"error\": {\"message\": \"Conversion of generated scene to model failed.\",\"code\": 500,\"cause\": \"" + msg
					+ "\"}}";
		}
        
        String graphUri = URIconstants.GRAPH_PREFIX() + record.getId() + GENERATED_SCENES_SUFFIX;
        
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, sceneModel, RDFFormat.NQUADS_UTF8);
		String update = "INSERT { GRAPH <"+graphUri+"> {\r\n";
		update = update + sw.toString()+"\r\n";
		update = update + "} } WHERE {}";
		
		UpdateRequest request = new UpdateRequest();
		request.add("CLEAR GRAPH <"+graphUri+">;");
		UpdateFactory.parse(request, update);
		
		UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, sparqlEndpoint);
		logger.info("insertGeneratedScene - insert - {}", request.toString());

        
/*		Node graph = NodeFactory.createURI(URIconstants.GRAPH_PREFIX() + record.getId() + GENERATED_SCENES_SUFFIX);
		
		UpdateRequest request = new UpdateRequest();
		QuadDataAcc acc = new QuadDataAcc();
		acc.setGraph(graph);
		
		sceneModel.listStatements().forEachRemaining(st -> {
			Triple t = st.asTriple();
			acc.addTriple(t);
		});
		
		request.add("CLEAR GRAPH <"+graph.toString()+">;");
		request.add(new UpdateDataInsert(acc));
		
		UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, sparqlEndpoint);
		logger.info("insertGeneratedScene - insert - {}", request.toString());

*/
		try {
			processor.execute();
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("insertGeneratedScene - insert - UpdateProcessor - Exception - {}", msg);
			msg = msg.replace("\"", "");
			return "{\"error\": {\"message\": \"TripleStore scene insert failed .\",\"code\": 500,\"cause\": \"" + msg
					+ "\"}}";
		}
		
		return null;
	}
	
	public String insertAnnotations(String mediaId, InputStream content) {
		String result = null;
		
		logger.info("insertAnnotations - not implemented yet");
		
		/*
		
		Model model = ModelFactory.createDefaultModel();
		try {
			model.read(content, "", RDFLanguages.strLangTurtle);
		} catch (Exception e) {
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - RDF data could not be loaded into a model. {}", "insertAnnotations", msg);
			return null;
		}
		
		Dataset ds = DatasetFactory.create();
		ds.addNamedModel("http://test2.com", model);
		
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, model, RDFFormat.NQUADS_UTF8);
		
		try {
			RDFDataMgr.write(new BufferedOutputStream(new FileOutputStream("d:\\hagt\\googledrive\\HPI\\ada\\restapi\\test.nq")), ds.asDatasetGraph(), RDFFormat.NQUADS_UTF8);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		*/
		
/*		UpdateRequest request = new UpdateRequest();
		request.add("CLEAR GRAPH <http://test2.com>;");
		UpdateProcessor pr = UpdateExecutionFactory.createRemote(request, sparqlEndpoint);
		pr.execute();
		
		String update = "INSERT { GRAPH <http://test2.com> {\r\n";
		update = update + sw.toString()+"\r\n";
		update = update + "} } WHERE {}";
		
		System.out.println(update);
	*/
		
//		String update = 
//				"PREFIX dbpedia: <http://dbpedia.org/resource/>\r\n"
//				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\r\n"
//				+ "Insert Data \r\n"
//				+ "{ GRAPH <test> { <http://dbpedia.org/resource/life> <http://umbel.org/umbel/rc/Artist> '2'^^xsd:integer . } }";
		
/*		UpdateRequest ur = new UpdateRequest();
		UpdateFactory.parse(ur, update);
		
		UpdateProcessor processor = UpdateExecutionFactory.createRemote(ur, sparqlEndpoint);
		processor.execute();
		*/
		
/*		Node graph = NodeFactory.createURI("http://test.com");
		UpdateRequest request = new UpdateRequest();
		QuadDataAcc acc = new QuadDataAcc();
		acc.setGraph(graph);
		
		model.listStatements().forEachRemaining(st -> {
			Triple t = st.asTriple();
			acc.addTriple(t);
		});
		
		request.add(new UpdateDataInsert(acc));
		
		UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, sparqlEndpoint);
		processor.execute();
		
		*/

/*		try {
			processor.execute();
		} catch (HttpException e) {
			System.out.println(e);
			String msg = e.toString().replace("\n", " ");
			logger.error("{} - insertAnnotations - UpdateProcessor - Exception - {}", "uploadExtractorResult", msg);
//			msg = msg.replace("\"", "");
//			return "{\"error\": {\"message\": \"TripleStore metadata update failed .\",\"code\": 500,\"cause\": \"" + msg
//					+ "\"}}";
		}
*/

		
/*		System.out.println(model.getNsPrefixMap());
		
		Dataset dataset = DatasetFactory.create();
		
		dataset.addNamedModel("http://test.com", model);
		
		RDFConnection connect = RDFConnectionFactory.connect(sparqlEndpoint);
		connect.load("http://test.com", model);
		*/
		
//		try (RDFConnection conn = RDFConnectionFactory.connect(sparqlEndpoint)) {
//		    conn.begin(ReadWrite.WRITE);
//		    conn.loadDataset(dataset);
//		    conn.commit();
//		    conn.end();
//		} catch (Exception e) {
//			System.out.println(e);
//		}
//		
		
//		RDFConnectionFactory.
//		
//		UpdateFactory.read
		
		return result;
	}


}
