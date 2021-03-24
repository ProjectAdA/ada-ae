package de.ada.restapi;

public class URIconstants {
	public static final String URL_PREFIX = "http://ada.filmontology.org";
	public static final String ONTOLOGY_PREFIX = URL_PREFIX + "/ontology/2020/03/17/";
	public static final String RESOURCE_PREFIX = URL_PREFIX + "/resource/2020/03/17/";
	public static final String MEDIA_PREFIX = URL_PREFIX + "/resource/media/";

	public static final String QUERY_PREFIXES = 
			"prefix advene: <http://www.advene.org/ns/webannotation/>\r\n"
			+ "prefix local: <http://www.advene.org/ns/_local/>\r\n"
			+ "prefix oa:  <http://www.w3.org/ns/oa#>\r\n"
			+ "prefix ao: <" + ONTOLOGY_PREFIX + ">\r\n"
			+ "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n"
			+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n"
			+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#>\r\n";

}
