package de.ada.restapi;

public class JSONLDconstants {
	
	public static final String ANNOTATIONS_FULL_CONTEXT = 
			"{\r\n"
			+ "  \"@context\" : {\r\n"
			+ "    \"oa\" : \"http://www.w3.org/ns/oa#\",\r\n"
			+ "    \"dc\" : \"http://purl.org/dc/elements/1.1/\",\r\n"
			+ "    \"dcterms\" : \"http://purl.org/dc/terms/\",\r\n"
			+ "    \"dctypes\" : \"http://purl.org/dc/dcmitype/\",\r\n"
			+ "    \"foaf\" : \"http://xmlns.com/foaf/0.1/\",\r\n"
			+ "    \"rdf\" : \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\r\n"
			+ "    \"rdfs\" : \"http://www.w3.org/2000/01/rdf-schema#\",\r\n"
			+ "    \"skos\" : \"http://www.w3.org/2004/02/skos/core#\",\r\n"
			+ "    \"xsd\" : \"http://www.w3.org/2001/XMLSchema#\",\r\n"
			+ "    \"iana\" : \"http://www.iana.org/assignments/relation/\",\r\n"
			+ "    \"owl\" : \"http://www.w3.org/2002/07/owl#\",\r\n"
			+ "    \"as\" : \"http://www.w3.org/ns/activitystreams#\",\r\n"
			+ "    \"schema\" : \"http://schema.org/\",\r\n"
			+ "    \"id\" : {\r\n"
			+ "      \"@id\" : \"@id\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"type\" : {\r\n"
			+ "      \"@id\" : \"@type\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"Annotation\" : \"oa:Annotation\",\r\n"
			+ "    \"Dataset\" : \"dctypes:Dataset\",\r\n"
			+ "    \"Image\" : \"dctypes:StillImage\",\r\n"
			+ "    \"Video\" : \"dctypes:MovingImage\",\r\n"
			+ "    \"Audio\" : \"dctypes:Sound\",\r\n"
			+ "    \"Text\" : \"dctypes:Text\",\r\n"
			+ "    \"TextualBody\" : \"oa:TextualBody\",\r\n"
			+ "    \"ResourceSelection\" : \"oa:ResourceSelection\",\r\n"
			+ "    \"SpecificResource\" : \"oa:SpecificResource\",\r\n"
			+ "    \"FragmentSelector\" : \"oa:FragmentSelector\",\r\n"
			+ "    \"CssSelector\" : \"oa:CssSelector\",\r\n"
			+ "    \"XPathSelector\" : \"oa:XPathSelector\",\r\n"
			+ "    \"TextQuoteSelector\" : \"oa:TextQuoteSelector\",\r\n"
			+ "    \"TextPositionSelector\" : \"oa:TextPositionSelector\",\r\n"
			+ "    \"DataPositionSelector\" : \"oa:DataPositionSelector\",\r\n"
			+ "    \"SvgSelector\" : \"oa:SvgSelector\",\r\n"
			+ "    \"RangeSelector\" : \"oa:RangeSelector\",\r\n"
			+ "    \"TimeState\" : \"oa:TimeState\",\r\n"
			+ "    \"HttpRequestState\" : \"oa:HttpRequestState\",\r\n"
			+ "    \"CssStylesheet\" : \"oa:CssStyle\",\r\n"
			+ "    \"Choice\" : \"oa:Choice\",\r\n"
			+ "    \"Person\" : \"foaf:Person\",\r\n"
			+ "    \"Software\" : \"as:Application\",\r\n"
			+ "    \"Organization\" : \"foaf:Organization\",\r\n"
			+ "    \"AnnotationCollection\" : \"as:OrderedCollection\",\r\n"
			+ "    \"AnnotationPage\" : \"as:OrderedCollectionPage\",\r\n"
			+ "    \"Audience\" : \"schema:Audience\",\r\n"
			+ "    \"Motivation\" : \"oa:Motivation\",\r\n"
			+ "    \"bookmarking\" : \"oa:bookmarking\",\r\n"
			+ "    \"classifying\" : \"oa:classifying\",\r\n"
			+ "    \"commenting\" : \"oa:commenting\",\r\n"
			+ "    \"describing\" : \"oa:describing\",\r\n"
			+ "    \"editing\" : \"oa:editing\",\r\n"
			+ "    \"highlighting\" : \"oa:highlighting\",\r\n"
			+ "    \"identifying\" : \"oa:identifying\",\r\n"
			+ "    \"linking\" : \"oa:linking\",\r\n"
			+ "    \"moderating\" : \"oa:moderating\",\r\n"
			+ "    \"questioning\" : \"oa:questioning\",\r\n"
			+ "    \"replying\" : \"oa:replying\",\r\n"
			+ "    \"reviewing\" : \"oa:reviewing\",\r\n"
			+ "    \"tagging\" : \"oa:tagging\",\r\n"
			+ "    \"auto\" : \"oa:autoDirection\",\r\n"
			+ "    \"ltr\" : \"oa:ltrDirection\",\r\n"
			+ "    \"rtl\" : \"oa:rtlDirection\",\r\n"
			+ "    \"body\" : {\r\n"
			+ "      \"@id\" : \"oa:hasBody\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"target\" : {\r\n"
			+ "      \"@id\" : \"oa:hasTarget\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"source\" : {\r\n"
			+ "      \"@id\" : \"oa:hasSource\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"selector\" : {\r\n"
			+ "      \"@id\" : \"oa:hasSelector\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"state\" : {\r\n"
			+ "      \"@id\" : \"oa:hasState\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"scope\" : {\r\n"
			+ "      \"@id\" : \"oa:hasScope\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"refinedBy\" : {\r\n"
			+ "      \"@id\" : \"oa:refinedBy\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"startSelector\" : {\r\n"
			+ "      \"@id\" : \"oa:hasStartSelector\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"endSelector\" : {\r\n"
			+ "      \"@id\" : \"oa:hasEndSelector\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"renderedVia\" : {\r\n"
			+ "      \"@id\" : \"oa:renderedVia\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"creator\" : {\r\n"
			+ "      \"@id\" : \"dcterms:creator\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"generator\" : {\r\n"
			+ "      \"@id\" : \"as:generator\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"rights\" : {\r\n"
			+ "      \"@id\" : \"dcterms:rights\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"homepage\" : {\r\n"
			+ "      \"@id\" : \"foaf:homepage\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"via\" : {\r\n"
			+ "      \"@id\" : \"oa:via\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"canonical\" : {\r\n"
			+ "      \"@id\" : \"oa:canonical\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"stylesheet\" : {\r\n"
			+ "      \"@id\" : \"oa:styledBy\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"cached\" : {\r\n"
			+ "      \"@id\" : \"oa:cachedSource\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"conformsTo\" : {\r\n"
			+ "      \"@id\" : \"dcterms:conformsTo\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"items\" : {\r\n"
			+ "      \"@id\" : \"as:items\",\r\n"
			+ "      \"@type\" : \"@id\",\r\n"
			+ "      \"@container\" : \"@list\"\r\n"
			+ "    },\r\n"
			+ "    \"partOf\" : {\r\n"
			+ "      \"@id\" : \"as:partOf\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"first\" : {\r\n"
			+ "      \"@id\" : \"as:first\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"last\" : {\r\n"
			+ "      \"@id\" : \"as:last\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"next\" : {\r\n"
			+ "      \"@id\" : \"as:next\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"prev\" : {\r\n"
			+ "      \"@id\" : \"as:prev\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"audience\" : {\r\n"
			+ "      \"@id\" : \"schema:audience\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"motivation\" : {\r\n"
			+ "      \"@id\" : \"oa:motivatedBy\",\r\n"
			+ "      \"@type\" : \"@vocab\"\r\n"
			+ "    },\r\n"
			+ "    \"purpose\" : {\r\n"
			+ "      \"@id\" : \"oa:hasPurpose\",\r\n"
			+ "      \"@type\" : \"@vocab\"\r\n"
			+ "    },\r\n"
			+ "    \"textDirection\" : {\r\n"
			+ "      \"@id\" : \"oa:textDirection\",\r\n"
			+ "      \"@type\" : \"@vocab\"\r\n"
			+ "    },\r\n"
			+ "    \"accessibility\" : \"schema:accessibilityFeature\",\r\n"
			+ "    \"bodyValue\" : \"oa:bodyValue\",\r\n"
			+ "    \"format\" : \"dc:format\",\r\n"
			+ "    \"language\" : \"dc:language\",\r\n"
			+ "    \"processingLanguage\" : \"oa:processingLanguage\",\r\n"
			+ "    \"value\" : \"rdf:value\",\r\n"
			+ "    \"exact\" : \"oa:exact\",\r\n"
			+ "    \"prefix\" : \"oa:prefix\",\r\n"
			+ "    \"suffix\" : \"oa:suffix\",\r\n"
			+ "    \"styleClass\" : \"oa:styleClass\",\r\n"
			+ "    \"name\" : \"foaf:name\",\r\n"
			+ "    \"email\" : \"foaf:mbox\",\r\n"
			+ "    \"email_sha1\" : \"email:_sha1sum\",\r\n"
			+ "    \"nickname\" : \"foaf:nick\",\r\n"
			+ "    \"label\" : \"rdfs:label\",\r\n"
			+ "    \"created\" : {\r\n"
			+ "      \"@id\" : \"dcterms:created\",\r\n"
			+ "      \"@type\" : \"xsd:dateTime\"\r\n"
			+ "    },\r\n"
			+ "    \"modified\" : {\r\n"
			+ "      \"@id\" : \"dcterms:modified\",\r\n"
			+ "      \"@type\" : \"xsd:dateTime\"\r\n"
			+ "    },\r\n"
			+ "    \"generated\" : {\r\n"
			+ "      \"@id\" : \"dcterms:issued\",\r\n"
			+ "      \"@type\" : \"xsd:dateTime\"\r\n"
			+ "    },\r\n"
			+ "    \"sourceDate\" : {\r\n"
			+ "      \"@id\" : \"oa:sourceDate\",\r\n"
			+ "      \"@type\" : \"xsd:dateTime\"\r\n"
			+ "    },\r\n"
			+ "    \"sourceDateStart\" : {\r\n"
			+ "      \"@id\" : \"sourceDate:Start\",\r\n"
			+ "      \"@type\" : \"xsd:dateTime\"\r\n"
			+ "    },\r\n"
			+ "    \"sourceDateEnd\" : {\r\n"
			+ "      \"@id\" : \"sourceDate:End\",\r\n"
			+ "      \"@type\" : \"xsd:dateTime\"\r\n"
			+ "    },\r\n"
			+ "    \"start\" : {\r\n"
			+ "      \"@id\" : \"oa:start\",\r\n"
			+ "      \"@type\" : \"xsd:nonNegativeInteger\"\r\n"
			+ "    },\r\n"
			+ "    \"end\" : {\r\n"
			+ "      \"@id\" : \"oa:end\",\r\n"
			+ "      \"@type\" : \"xsd:nonNegativeInteger\"\r\n"
			+ "    },\r\n"
			+ "    \"total\" : {\r\n"
			+ "      \"@id\" : \"as:totalItems\",\r\n"
			+ "      \"@type\" : \"xsd:nonNegativeInteger\"\r\n"
			+ "    },\r\n"
			+ "    \"startIndex\" : {\r\n"
			+ "      \"@id\" : \"as:startIndex\",\r\n"
			+ "      \"@type\" : \"xsd:nonNegativeInteger\"\r\n"
			+ "    },\r\n"
			+ "    \"advene\" : \"http://www.advene.org/ns/webannotation/\",\r\n"
			+ "    \"local\" : \"http://www.advene.org/ns/_local/\",\r\n"
			+ "    \"ao\" : \""+URIconstants.ONTOLOGY_PREFIX()+"\",\r\n"
			+ "    \"annotationType\" : {\r\n"
			+ "      \"@id\" : \"ao:annotationType\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"annotationNumericValueSequence\" : {\r\n"
			+ "      \"@id\" : \"ao:annotationNumericValueSequence\",\r\n"
			+ "      \"@type\" : \"xsd:decimal\",\r\n"
			+ "      \"@container\" : \"@list\"\r\n"
			+ "    },\r\n"
			+ "    \"annotationValueSequence\" : {\r\n"
			+ "      \"@id\" : \"ao:annotationValueSequence\",\r\n"
			+ "      \"@type\" : \"@id\",\r\n"
			+ "      \"@container\" : \"@list\"\r\n"
			+ "    },\r\n"
			+ "    \"annotationValue\" : {\r\n"
			+ "      \"@id\" : \"ao:annotationValue\",\r\n"
			+ "      \"@type\" : \"@id\"\r\n"
			+ "    },\r\n"
			+ "    \"annotationNumericValue\" : {\r\n"
			+ "      \"@id\" : \"ao:annotationNumericValue\",\r\n"
			+ "      \"@type\" : \"xsd:decimal\"\r\n"
			+ "    },\r\n"
			+ "    \"sceneId\" : \"ao:sceneId\"\r\n"
			+ "  }\r\n"
			+ "}";


	public static final String ANNOTATIONS_FULL_FRAME = ANNOTATIONS_FULL_CONTEXT.substring(0, ANNOTATIONS_FULL_CONTEXT.length()-1) +
			",\r\n"
			+ "  \"@type\": \"oa:Annotation\"\r\n"
			+ "}\r\n"
			+ "";

}
