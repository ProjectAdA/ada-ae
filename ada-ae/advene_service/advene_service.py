#!/usr/bin/env python3

import os
import tempfile
import subprocess
import logging
import json
import posixpath
from flask import Flask, request, jsonify
from flask_compress import Compress
from waitress import serve
from werkzeug.utils import secure_filename

# config params - must be set via environment variables
ONTOLOGY_BASE_URI = ''
ONTOLOGY_VERSION = ''

# will be constructed from ONTOLOGY_BASE_URI and ONTOLOGY_VERSION
ONTOLOGY_PREFIX = ''
RESOURCE_PREFIX = ''
MEDIA_PREFIX = ''

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.DEBUG)

ch = logging.StreamHandler()
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)
logger.addHandler(ch)
logger.propagate = False    # prevent log messages from appearing twice

app = Flask(__name__)
Compress(app)

def advene_call(azp_filename, output_filename):
    logger.info('Running Advene AdARDFExporter with {0}'.format(azp_filename))
    
    process = ['advene', 
                '-p', 'dummy',
                '-f', 'AdARDFExporter',
                '-o', 'output="'+output_filename+'"',
                azp_filename]
    
    # For some reason Advene will not run and accept the parameter if 'process' is directly passed to subprocess.run
    command = ' '.join(map(str,process))
    logger.debug('Subprocess run: {0}'.format(command))
    p = subprocess.run(command,
                       shell=True,
                       stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)

    logger.debug('stderr: {0}\n'.format(p.stderr))
    logger.debug('stdout: {0}'.format(p.stdout))
    
    return p.returncode
    
def update_jsonld(data):
    logger.debug('Update json-ld')
    udata = data
    
    if not "@context" in data or not "first" in data:
        logger.error('field "@context" or "first" not found in root')
        return ""
    if not "items" in data["first"]:
        logger.error('field "items" not found in "first"')
        return ""
    
    # Move annotations to @graph field to conform to standard json-ld and delete unnessecary information
    items = data["first"]["items"]
    udata["@graph"] = items
    # Replace context generated by Advene with a static context that does not rely on retrieving external urls
    udata["@context"] = json.loads(GET_JSONLD_CONTEXT())
    del data["first"]
    if "id" in data:
        del data["id"]
    if "label" in data:
        del data["label"]
    if "totalItems" in data:
        del data["totalItems"]
    if "type" in data:
        del data["type"]
    
    return udata

@app.route('/convertazp', methods=['POST'])
def convert_azp():
    temp_dir = tempfile.TemporaryDirectory()
    logger.debug('{0}'.format(request))
    files = request.files.getlist("file")
    logger.debug('POST /convertazp {0}'.format(files))
    if len(files) == 0:
        logger.error('Request does not contain a "file".')
        return jsonify(error={'message': 'Request does not contain a file.'}), 400
    
    if len(files) > 1:
        logger.error('More than one file received')
        return jsonify(error={'message': 'Only one file upload is supported at a time'}), 400

    file = files[0]
    filename = secure_filename(file.filename)
    
    # More sophisticatd checks could be implemented but file is already checked by the ingester
    if not filename.endswith('.azp'):
        logger.error('Uploaded file extension is not .azp ({0})'.format(filename))
        return jsonify(error={'message': 'Uploaded file extension is not .azp '+filename}), 400
    
    azp_path = os.path.join(temp_dir.name, filename)
    try:
        logger.debug('Saving uploaded file {0}'.format(azp_path))
        file.save(azp_path)
    except Exception as e:
        logger.error('AZP File {0} could not be saved'.format(azp_path))
        return jsonify(error={'message': 'AZP File '+str(azp_path)+'could not be saved', 'reason': str(e)}), 500
    
    jsonld_filename = str(azp_path)+'.jsonld'
    ret = advene_call(str(azp_path), jsonld_filename)
    logger.debug('Advene exit code: {0}'.format(ret))
    if ret != 0:
        logger.error('Advene conversion failed on {0}'.format(azp_path))
        return jsonify(error={'message': 'Advene conversion failed on '+str(azp_path)}), 500
    
    try:
        with open(jsonld_filename, 'r') as f:
            data = json.load(f) 
    except Exception as e:
        logger.error('Converted json-ld file {0} could not be loaded'.format(jsonld_filename))
        return jsonify(error={'message': 'Converted json-ld file '+jsonld_filename+' could not be loaded', 'reason': str(e)}), 500
    
    updated_data = update_jsonld(data)
    if not updated_data:
        logger.error('Converted json-ld file {0} is corrupt and cannot be updated'.format(jsonld_filename))
        return jsonify(error={'message': 'Converted json-ld file '+str(jsonld_filename)+' is corrupt and cannot be updated'}), 500

    logger.debug('Send updated json-ld content')
    return json.dumps(updated_data), 200


def GET_JSONLD_CONTEXT():
    return '''
{
    "oa":      "http://www.w3.org/ns/oa#",
    "dc":      "http://purl.org/dc/elements/1.1/",
    "dcterms": "http://purl.org/dc/terms/",
    "dctypes": "http://purl.org/dc/dcmitype/",
    "foaf":    "http://xmlns.com/foaf/0.1/",
    "rdf":     "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs":    "http://www.w3.org/2000/01/rdf-schema#",
    "skos":    "http://www.w3.org/2004/02/skos/core#",
    "xsd":     "http://www.w3.org/2001/XMLSchema#",
    "iana":    "http://www.iana.org/assignments/relation/",
    "owl":     "http://www.w3.org/2002/07/owl#",
    "as":      "http://www.w3.org/ns/activitystreams#",
    "schema":  "http://schema.org/",

    "id":      {"@type": "@id", "@id": "@id"},
    "type":    {"@type": "@id", "@id": "@type"},

    "Annotation":           "oa:Annotation",
    "Dataset":              "dctypes:Dataset",
    "Image":                "dctypes:StillImage",
    "Video":                "dctypes:MovingImage",
    "Audio":                "dctypes:Sound",
    "Text":                 "dctypes:Text",
    "TextualBody":          "oa:TextualBody",
    "ResourceSelection":    "oa:ResourceSelection",
    "SpecificResource":     "oa:SpecificResource",
    "FragmentSelector":     "oa:FragmentSelector",
    "CssSelector":          "oa:CssSelector",
    "XPathSelector":        "oa:XPathSelector",
    "TextQuoteSelector":    "oa:TextQuoteSelector",
    "TextPositionSelector": "oa:TextPositionSelector",
    "DataPositionSelector": "oa:DataPositionSelector",
    "SvgSelector":          "oa:SvgSelector",
    "RangeSelector":        "oa:RangeSelector",
    "TimeState":            "oa:TimeState",
    "HttpRequestState":     "oa:HttpRequestState",
    "CssStylesheet":        "oa:CssStyle",
    "Choice":               "oa:Choice",
    "Person":               "foaf:Person",
    "Software":             "as:Application",
    "Organization":         "foaf:Organization",
    "AnnotationCollection": "as:OrderedCollection",
    "AnnotationPage":       "as:OrderedCollectionPage",
    "Audience":             "schema:Audience", 

    "Motivation":    "oa:Motivation",
    "bookmarking":   "oa:bookmarking",
    "classifying":   "oa:classifying",
    "commenting":    "oa:commenting",
    "describing":    "oa:describing",
    "editing":       "oa:editing",
    "highlighting":  "oa:highlighting",
    "identifying":   "oa:identifying",
    "linking":       "oa:linking",
    "moderating":    "oa:moderating",
    "questioning":   "oa:questioning",
    "replying":      "oa:replying",
    "reviewing":     "oa:reviewing",
    "tagging":       "oa:tagging",

    "auto":          "oa:autoDirection",
    "ltr":           "oa:ltrDirection",
    "rtl":           "oa:rtlDirection",

    "body":          {"@type": "@id", "@id": "oa:hasBody"},
    "target":        {"@type": "@id", "@id": "oa:hasTarget"},
    "source":        {"@type": "@id", "@id": "oa:hasSource"},
    "selector":      {"@type": "@id", "@id": "oa:hasSelector"},
    "state":         {"@type": "@id", "@id": "oa:hasState"},
    "scope":         {"@type": "@id", "@id": "oa:hasScope"},
    "refinedBy":     {"@type": "@id", "@id": "oa:refinedBy"},
    "startSelector": {"@type": "@id", "@id": "oa:hasStartSelector"},
    "endSelector":   {"@type": "@id", "@id": "oa:hasEndSelector"},
    "renderedVia":   {"@type": "@id", "@id": "oa:renderedVia"},
    "creator":       {"@type": "@id", "@id": "dcterms:creator"},
    "generator":     {"@type": "@id", "@id": "as:generator"},
    "rights":        {"@type": "@id", "@id": "dcterms:rights"},
    "homepage":      {"@type": "@id", "@id": "foaf:homepage"},
    "via":           {"@type": "@id", "@id": "oa:via"},
    "canonical":     {"@type": "@id", "@id": "oa:canonical"},
    "stylesheet":    {"@type": "@id", "@id": "oa:styledBy"},
    "cached":        {"@type": "@id", "@id": "oa:cachedSource"},
    "conformsTo":    {"@type": "@id", "@id": "dcterms:conformsTo"},
    "items":         {"@type": "@id", "@id": "as:items", "@container": "@list"},
    "partOf":        {"@type": "@id", "@id": "as:partOf"},
    "first":         {"@type": "@id", "@id": "as:first"},
    "last":          {"@type": "@id", "@id": "as:last"},
    "next":          {"@type": "@id", "@id": "as:next"},
    "prev":          {"@type": "@id", "@id": "as:prev"},
    "audience":      {"@type": "@id", "@id": "schema:audience"},
    "motivation":    {"@type": "@vocab", "@id": "oa:motivatedBy"},
    "purpose":       {"@type": "@vocab", "@id": "oa:hasPurpose"},
    "textDirection": {"@type": "@vocab", "@id": "oa:textDirection"},

    "accessibility": "schema:accessibilityFeature",
    "bodyValue":     "oa:bodyValue",
    "format":        "dc:format",
    "language":      "dc:language",
    "processingLanguage": "oa:processingLanguage",
    "value":         "rdf:value",
    "exact":         "oa:exact",
    "prefix":        "oa:prefix",
    "suffix":        "oa:suffix",
    "styleClass":    "oa:styleClass",
    "name":          "foaf:name",
    "email":         "foaf:mbox",
    "email_sha1":    "foaf:mbox_sha1sum",
    "nickname":      "foaf:nick",
    "label":         "rdfs:label",

    "created":       {"@id": "dcterms:created", "@type": "xsd:dateTime"},
    "modified":      {"@id": "dcterms:modified", "@type": "xsd:dateTime"},
    "generated":     {"@id": "dcterms:issued", "@type": "xsd:dateTime"},
    "sourceDate":    {"@id": "oa:sourceDate", "@type": "xsd:dateTime"},
    "sourceDateStart": {"@id": "oa:sourceDateStart", "@type": "xsd:dateTime"},
    "sourceDateEnd": {"@id": "oa:sourceDateEnd", "@type": "xsd:dateTime"},

    "start":         {"@id": "oa:start", "@type": "xsd:nonNegativeInteger"},
    "end":           {"@id": "oa:end", "@type": "xsd:nonNegativeInteger"},
    "total":         {"@id": "as:totalItems", "@type": "xsd:nonNegativeInteger"},
    "startIndex":    {"@id": "as:startIndex", "@type": "xsd:nonNegativeInteger"},

    "advene" : "http://www.advene.org/ns/webannotation/",
    "advu" : "http://www.advene.org/ns/_local/user/",
    "local" : "http://www.advene.org/ns/_local/",
    "ao" : "'''+ONTOLOGY_PREFIX+'''",
    "art" : "'''+RESOURCE_PREFIX+'''AnnotationType/",
    "arv" : "'''+RESOURCE_PREFIX+'''AnnotationValue/",
    "arm" : "'''+MEDIA_PREFIX+'''",
    "ao:annotationType": {
        "@type": "@id"
    },
    "ao:annotationValue": {
        "@type": "@id"
    },
    "ao:annotationValueSequence": {
        "@type": "@id"
    }
  }
'''

if __name__ == '__main__':

    if "ONTOLOGY_BASE_URI" in os.environ:
        ONTOLOGY_BASE_URI = os.environ.get('ONTOLOGY_BASE_URI')

    if "ONTOLOGY_VERSION" in os.environ:
        ONTOLOGY_VERSION = os.environ.get('ONTOLOGY_VERSION')

    if not ONTOLOGY_BASE_URI or not ONTOLOGY_VERSION:
        logger.error("Environment variables ONTOLOGY_BASE_URI and ONTOLOGY_VERSION have to be set to run the Advene Service.")
    else:
        ONTOLOGY_PREFIX = posixpath.join(ONTOLOGY_BASE_URI, 'ontology', ONTOLOGY_VERSION, '')
        RESOURCE_PREFIX = posixpath.join(ONTOLOGY_BASE_URI, 'resource', ONTOLOGY_VERSION, '')
        MEDIA_PREFIX = posixpath.join(ONTOLOGY_BASE_URI, 'resource', 'media', '')
        #app.run(host='0.0.0.0', port=5002)
        serve(app, host='0.0.0.0', port=5002)
