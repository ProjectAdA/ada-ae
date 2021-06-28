package de.ada.restapi;

public class AnnotationQueries {
	
	public static final String QUERY_ANNOTATION_COUNT = 
			"SELECT (count(?anno) as ?count) WHERE {\n"
			+ "	?anno rdf:type oa:Annotation.\n"
			+ "}";
	
	public static final String QUERY_ANNOTATIONS_TEMPLATE = 
			"DESCRIBE ?anno ?target ?body ?selector ?list ?listRest ?numlist ?numlistRest FROM <<GRAPH>> WHERE {\r\n"
			+ "	?anno oa:hasTarget ?target.\r\n"
			+ "	?target oa:hasSource <<MEDIA>>.\r\n"
			+ "	?anno oa:hasBody ?body.\r\n"
			+ "	?target oa:hasSelector ?selector.\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?body ao:annotationValueSequence ?list .\r\n"
			+ "		?list rdf:rest* ?listRest .\r\n"
			+ "		FILTER EXISTS {?listRest rdf:first ?n}\r\n"
			+ "	}\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?body ao:annotationNumericValueSequence ?numlist .\r\n"
			+ "		?numlist rdf:rest* ?numlistRest .\r\n"
			+ "		FILTER EXISTS {?numlistRest rdf:first ?n}\r\n"
			+ "	}\r\n"
			+ "	<<SCENEFILTER>>\r\n"
			+ "	<<TYPEFILTER>>\r\n"
			+ "}";
	
	public static final String TEXT_SEARCH_TEMPLATE = 
			"DESCRIBE ?annores ?targetres ?bodyres ?selector ?list ?listRest ?numlist ?numlistRest WHERE {\r\n"
			+ "	?annores oa:hasTarget ?targetres.\r\n"
			+ "	?annores oa:hasBody ?bodyres.\r\n"
			+ "	?targetres oa:hasSelector ?selector.\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?bodyres ao:annotationValueSequence ?list .\r\n"
			+ "		?list rdf:rest* ?listRest .\r\n"
			+ "		FILTER EXISTS {?listRest rdf:first ?n}\r\n"
			+ "	}\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?bodyres ao:annotationNumericValueSequence ?numlist .\r\n"
			+ "		?numlist rdf:rest* ?numlistRest .\r\n"
			+ "		FILTER EXISTS {?numlistRest rdf:first ?n}\r\n"
			+ "	}\r\n"
			+ "	{SELECT ?anno WHERE {\r\n"
			+ "			?anno oa:hasBody ?body.\r\n"
			+ "			?body rdf:value ?annotextvalue.\r\n"
			+ "			<<TEXTFILTER>>\r\n"
			+ "			<<MEDIAFILTER>>\r\n"
			+ "			<<TYPEFILTER>>\r\n"
			+ "	}}\r\n"
			+ "	FILTER (?anno = ?annores)\r\n"
			+ "}\r\n";

	public static final String VALUE_SEARCH_SELECT_TEMPLATE =
			"select ?anno ?begin ?end ?source WHERE {\r\n"
			+ "	?anno oa:hasBody ?body.\r\n"
			+ "	?anno oa:hasTarget ?target.\r\n"
			+ "	?target oa:hasSource ?source.\r\n"
			+ "	?target oa:hasSelector ?selector.\r\n"
			+ "	?selector advene:begin ?begin.\r\n"
			+ "	?selector advene:end ?end.\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?body ao:annotationValue ?annovalue.\r\n"
			+ "	}\r\n"
			+ "	OPTIONAL {\r\n"
			+ "		?body ao:annotationValueSequence ?list .\r\n"
			+ "		?list rdf:rest*/rdf:first ?seqValue.\r\n"
			+ "	}\r\n"
			+ "	<<VALUEFILTER>>\r\n"
			+ "	<<MEDIAFILTER>>\r\n"
			+ "}";
	
}
