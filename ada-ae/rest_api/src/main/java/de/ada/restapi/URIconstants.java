package de.ada.restapi;

public class URIconstants {
	public static String ONTOLOGY_BASE_URI = "http://ada.filmontology.org";
	public static String ONTOLOGY_VERSION = "2020/03/17";
	
//	public static final String ONTOLOGY_PREFIX = ONTOLOGY_BASE_URI + "/ontology/"+ONTOLOGY_VERSION+"/";
//	public static final String RESOURCE_PREFIX = ONTOLOGY_BASE_URI + "/resource/"+ONTOLOGY_VERSION+"/";
//	public static final String MEDIA_PREFIX = ONTOLOGY_BASE_URI + "/resource/media/";
//	public static final String GRAPH_PREFIX = ONTOLOGY_BASE_URI + "/graph/";
	
	public static String ONTOLOGY_PREFIX() {
		return ONTOLOGY_BASE_URI + "/ontology/"+ONTOLOGY_VERSION+"/";
	}

	public static String RESOURCE_PREFIX() {
		return ONTOLOGY_BASE_URI + "/resource/"+ONTOLOGY_VERSION+"/";
	}

	public static String MEDIA_PREFIX() {
		return ONTOLOGY_BASE_URI + "/resource/media/";
	}

	public static String GRAPH_PREFIX() {
		return ONTOLOGY_BASE_URI + "/graph/";
	}
	
	public static String QUERY_PREFIXES() {
		return
				"prefix advene: <http://www.advene.org/ns/webannotation/>\r\n"
				+ "prefix local: <http://www.advene.org/ns/_local/>\r\n"
				+ "prefix oa:  <http://www.w3.org/ns/oa#>\r\n"
				+ "prefix ao: <" + ONTOLOGY_PREFIX() + ">\r\n"
				+ "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n"
				+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n"
				+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#>\r\n";
	}

}
