package de.ada.restapi;

public class MetadataQueries {
	
	public static String QUERY_ALL_SCENES() {
		return
		"select ?id ?name ?startTime ?endTime {\r\n"
		+ "	?sceneuri oa:hasTarget ?target.\r\n"
		+ "	?sceneuri oa:hasBody ?body.\r\n"
		+ "	?body ao:annotationType <"+URIconstants.RESOURCE_PREFIX()+"AnnotationType/Scene>.\r\n"
		+ "	?body rdf:value ?name.\r\n"
		+ "	?target oa:hasSelector ?selector.\r\n"
		+ "	?selector advene:begin ?startTime.\r\n"
		+ "	?selector advene:end ?endTime.\r\n"
		+ "	BIND ( REPLACE(str(?sceneuri),\""+URIconstants.MEDIA_PREFIX()+"\",\"\") as ?id )\r\n"
		+ "}\r\n";
	}
	
	public static final String QUERY_SCENES_TEMPLATE() {
		return
			"select ?id ?name ?startTime ?endTime {\r\n"
			+ "	?target oa:hasSource <<MEDIA>>.\r\n"
			+ "	?sceneuri oa:hasTarget ?target.\r\n"
			+ "	?sceneuri oa:hasBody ?body.\r\n"
			+ "	?body ao:annotationType <"+URIconstants.RESOURCE_PREFIX()+"AnnotationType/Scene>.\r\n"
			+ "	?body rdf:value ?name.\r\n"
			+ "	?target oa:hasSelector ?selector.\r\n"
			+ "	?selector advene:begin ?startTime.\r\n"
			+ "	?selector advene:end ?endTime.\r\n"
			+ "	BIND ( REPLACE(str(?sceneuri),\"^.*\\\\/\",\"\") as ?id )\r\n"
			+ "}";
	}
	
	public static final String QUERY_ANNOTATION_TYPE_COUNTS() {
		return
			"select ?id ?annotype (Count(?annotype) as ?count) where {\r\n"
			+ "	?anno advene:type ?annotype.\r\n"
			+ "	BIND ( REPLACE(REPLACE(str(?anno),\""+URIconstants.MEDIA_PREFIX()+"\",\"\"),\"\\\\/(.*)\",\"\") as ?id )\r\n"
			+ "}\r\n"
			+ "GROUP by ?id ?annotype\r\n"
			+ "";
	}
	
	public static final String QUERY_METADATA() {
		return
			"select ?mediauri ?title ?year ?runtime ?duration ?category ?playoutUrl ?id ?shortId ?director ?abstract ?releaseDate ?writer ?language ?broadcaster ?imdbId ?actors ?filmversion WHERE {\n"
			+ "	?mediauri rdf:type <http://schema.org/VideoObject>.\n"
			+ "	?mediauri rdfs:label ?title.\n"
			+ "	?mediauri <http://dbpedia.org/ontology/year> ?year.\n"
			+ "	?mediauri <http://dbpedia.org/ontology/Work/runtime> ?runtime.\n"
			+ "	?mediauri <http://schema.org/duration> ?duration.\n"
			+ "	?mediauri <http://schema.org/genre> ?category.\n"
			+ "	?mediauri <http://schema.org/url> ?playoutUrl.\n"
			+ "	?mediauri <https://w3id.org/idsa/core/checkSum> ?id.\n"
			+ "	?mediauri <http://purl.org/dc/elements/1.1/identifier> ?shortId.\n"
			+ "	OPTIONAL {?mediauri <http://schema.org/director> ?director.}\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/abstract> ?abstract.}\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/releaseDate> ?releaseDate.}\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/writer> ?writer.}\n"
			+ "	OPTIONAL {?mediauri <http://purl.org/dc/terms/language> ?language.}\n"
			+ "	OPTIONAL {?mediauri <http://schema.org/broadcaster> ?broadcaster.}\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/imdbId> ?imdbId.}\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/actor> ?actors.}\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/filmVersion> ?filmversion.}\n"
			+ "}\n";
	}
			
	public static final String QUERY_ONTOLOGY_ELEMENTS() {
		return
			"SELECT ?elementUri ?id ?elementName ?elementFullName ?elementDescription ?sequentialNumber ?elementColor ?elementNumericValue WHERE {\r\n"
			+ "	?elementUri rdf:type ?elemtype.\r\n"
			+ "	BIND ( REPLACE(STR(?elementUri), \""+URIconstants.RESOURCE_PREFIX()+"\", \"\") as ?id)\r\n"
			+ "	?elementUri rdfs:label ?label.\r\n"
			+ "	FILTER(LANG(?label) = \"en\")\r\n"
			+ "	BIND (STR(?label )  AS ?elementName )\r\n"
			+ "	?elementUri ao:sequentialNumber ?seq.\r\n"
			+ "	BIND (xsd:integer(?seq)  AS ?sequentialNumber )\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?elementUri rdfs:comment ?desc.\r\n"
			+ "		FILTER(LANG(?desc) = \"en\")\r\n"
			+ "		BIND (STR(?desc)  AS ?elementDescription )\r\n"
			+ "	}\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?elementUri ao:adveneColorCode ?elementColor.\r\n"
			+ "	}\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?elementUri ao:prefixedLabel ?elementFullName.\r\n"
			+ "	}\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?elementUri ao:annotationNumericValue ?elementNumericValue.\r\n"
			+ "	}\r\n"
			+ "	FILTER (?elemtype = ao:AnnotationLevel || ?elemtype = ao:AnnotationType || ?elemtype = ao:AnnotationValue)\r\n"
			+ "}\r\n"
			+ "ORDER BY ASC(?sequentialNumber)";
	}
	

	public static final String QUERY_ONTOLOGY_LINKS() {
		return
			"SELECT ?elementUri (GROUP_CONCAT(DISTINCT ?subelement; SEPARATOR=\";\") AS ?subElements) WHERE {\r\n"
			+ "	?elementUri ao:hasAnnotationType|ao:hasPredefinedValue ?subelement.\r\n"
			+ "}\r\n"
			+ "GROUP BY ?elementUri";
	}

	public static final String QUERY_MAX_MOVIE_SHORTID() {
		return
			"SELECT (MAX(?shortid) as ?maxShortid) WHERE {\r\n"
			+ "	?mediauri rdf:type <http://schema.org/VideoObject>.\r\n"
			+ "	?mediauri <http://purl.org/dc/elements/1.1/identifier> ?shortid.\r\n"
			+ "}\r\n";
	}
}
