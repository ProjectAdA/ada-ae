package de.ada.restapi;

public class AnnotationConstants {
	
	public static boolean USE_VIRTUOSO_QUERY_PAGINATION = true;
	public static int NUMBER_OF_ANNOTATIONS_PER_INSERT_QUERY = 60;
	
	public static String ANNOTATION_PREFIXES() {
		return
				"@prefix oa:    <http://www.w3.org/ns/oa#> .\r\n"
				+ "@prefix dcterms: <http://purl.org/dc/terms/> .\r\n"
				+ "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\r\n"
				+ "@prefix advu:  <http://www.advene.org/ns/_local/user/> .\r\n"
				+ "@prefix advene: <http://www.advene.org/ns/webannotation/> .\r\n"
				+ "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n"
				+ "@prefix ao:    <"+URIconstants.ONTOLOGY_PREFIX()+"> . \r\n"
				+ "@prefix ar:    <"+URIconstants.RESOURCE_PREFIX()+"> . \r\n"
				+ "@prefix art:   <"+URIconstants.RESOURCE_PREFIX()+"AnnotationType/> .\r\n"
				+ "@prefix arm:   <"+URIconstants.MEDIA_PREFIX()+"> .\r\n";
	}

	public static String ANNOTATION_TEMPLATE() {
		return
	    		"<"+URIconstants.MEDIA_PREFIX()+"XXMEDIAIDXX/XXMEDIAIDXX>\r\n" + 
	    		"        a                  oa:Annotation ;\r\n" + 
	    		"        ao:sceneId         \"XXMEDIAIDXX\" ;\r\n" + 
	    		"        dcterms:created    \"XXDATETIMEXX\"^^xsd:dateTime ;\r\n" + 
	    		"        dcterms:creator    advu:scenegen ;\r\n" + 
	    		"        advene:color       \"#dcdcdc\" ;\r\n" + 
	    		"        advene:type_color  \"#dcdcdc\" ;\r\n" + 
	    		"        advene:type        \"Scene\" ;\r\n" + 
	    		"        advene:type_title  \"Seg | Scene\" ;\r\n" + 
	    		"        oa:hasBody         [ a                  oa:TextualBody ;\r\n" + 
	    		"                             rdf:value          \"Complete movie\" ;\r\n" + 
	    		"                             ao:annotationType  art:Scene\r\n" + 
	    		"                           ] ;\r\n" + 
	    		"        oa:hasTarget       [ oa:hasSelector  [ a                   oa:FragmentSelector ;\r\n" + 
	    		"                                               rdf:value           \"t=0,XXENDFLOAT\" ;\r\n" + 
	    		"                                               dcterms:conformsTo  <http://www.w3.org/TR/media-frags/> ;\r\n" + 
	    		"                                               advene:begin        \"0\" ;\r\n" + 
	    		"                                               advene:end          \"XXENDMS\"\r\n" + 
	    		"                                             ] ;\r\n" + 
	    		"                             oa:hasSource    arm:XXMEDIAIDXX\r\n" + 
	    		"                           ] . \r\n";
	}
	
}
