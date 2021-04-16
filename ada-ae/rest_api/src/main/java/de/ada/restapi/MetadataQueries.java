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
			"select ?mediauri ?id ?shortId ?title ?year ?runtime ?duration ?category ?playoutUrl ?director ?abstract ?releaseDate ?writer ?language ?broadcaster ?imdbId ?actors ?filmversion (count(?target) as ?annotationsTotal) WHERE {\r\n"
			+ "	?mediauri rdf:type <http://schema.org/VideoObject>.\r\n"
			+ "	?mediauri rdfs:label ?title.\r\n"
			+ "	?mediauri <http://dbpedia.org/ontology/year> ?year.\r\n"
			+ "	?mediauri <http://dbpedia.org/ontology/Work/runtime> ?runtime.\r\n"
			+ "	?mediauri <http://schema.org/duration> ?duration.\r\n"
			+ "	?mediauri <http://schema.org/genre> ?category.\r\n"
			+ "	?mediauri <http://schema.org/url> ?playoutUrl.\r\n"
			+ "	?mediauri <https://w3id.org/idsa/core/checkSum> ?id.\r\n"
			+ "	OPTIONAL {?mediauri <http://purl.org/dc/elements/1.1/identifier> ?shortId}.\r\n" // TODO remove optional
			+ "	OPTIONAL {?target oa:hasSource ?mediauri}.\r\n"
			+ "	OPTIONAL {?mediauri <http://schema.org/director> ?director.}\r\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/abstract> ?abstract.}\r\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/releaseDate> ?releaseDate.}\r\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/writer> ?writer.}\r\n"
			+ "	OPTIONAL {?mediauri <http://purl.org/dc/terms/language> ?language.}\r\n"
			+ "	OPTIONAL {?mediauri <http://schema.org/broadcaster> ?broadcaster.}\r\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/imdbId> ?imdbId.}\r\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/actor> ?actors.}\r\n"
			+ "	OPTIONAL {?mediauri <http://dbpedia.org/ontology/filmVersion> ?filmversion.}\r\n"
			+ "}\r\n"
			+ "GROUP BY ?mediauri ?id ?shortId ?title ?year ?runtime ?duration ?category ?playoutUrl ?director ?abstract ?releaseDate ?writer ?language ?broadcaster ?imdbId ?actors ?filmversion";
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
