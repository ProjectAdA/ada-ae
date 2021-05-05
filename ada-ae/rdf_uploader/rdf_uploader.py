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

# secrets
API_TOKEN_HEADER_FIELD = ''
API_TOKEN = '' 

# Hostname and port of the Virtuoso instance for connection via isql-v
TRIPLESTORE_HOST = ''
TRIPLESTORE_PORT = ''

# Uploaded files must be stored on a docker volume so that the virtuoso container can access them for rdf load
TEMP_DIR_PREFIX = '/import'

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.DEBUG)

ch = logging.StreamHandler()
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)
logger.addHandler(ch)
logger.propagate = False    # prevent log messages from appearing twice

app = Flask(__name__)
Compress(app)

def run_import(sql_file):
    logger.info('Running isql-v import with {0}'.format(sql_file))

    process = ['isql-v', 
                TRIPLESTORE_HOST+":"+TRIPLESTORE_PORT,
                '-U', 'dba',
                '-P', '${DBA_PASSWORD}',
                '<'+str(sql_file)]
    
    command = ' '.join(map(str,process))
    logger.debug('Subprocess run: {0}'.format(command))
    p = subprocess.run(command,
                       shell=True,
                       stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)

    logger.debug('stderr: {0}\n'.format(p.stderr))
    logger.debug('stdout: {0}'.format(p.stdout))
    
    return p.returncode


@app.route('/uploadrdf', methods=['POST'])
def upload_rdf():

    logger.debug('{0}'.format(request))
    logger.debug('{0}'.format(request.headers))
    logger.debug('{0}'.format(request.form))
    
    if not request.headers or request.headers.get(API_TOKEN_HEADER_FIELD) != API_TOKEN:
        logger.error('Function requires authorization.')
        return jsonify(error={'message': 'Function requires authorization.'}), 403
    
    if not request.form.get('graph'):
        logger.error('"graph" field is missing in request.')
        return jsonify(error={'message': 'Graph field is missing in request'}), 400
    
    # TODO: check if valid url
    graph = request.form.get('graph')
    
    temp_dir = tempfile.TemporaryDirectory(dir=TEMP_DIR_PREFIX)
    import_dir = os.path.join(temp_dir.name, "import")
    try:
        os.makedirs(import_dir) 
    except Exception as e:
        logger.error('Temp dir {0} could not be created'.format(import_dir))
        return jsonify(error={'message': 'Temp dir '+str(import_dir)+' could not be created', 'reason': str(e)}), 500
    
    files = request.files.getlist("file")
    logger.debug('POST /uploadrdf {0}'.format(files))
    logger.debug('Graph {0}'.format(graph))
    
    if len(files) == 0:
        logger.error('Request does not contain a "file".')
        return jsonify(error={'message': 'Request does not contain a file.'}), 400
    
    if len(files) > 1:
        logger.error('More than one file received')
        return jsonify(error={'message': 'Only one file upload is supported at a time'}), 400

    file = files[0]
    filename = secure_filename(file.filename)
    rdf_path = os.path.join(import_dir, filename)

    try:
        logger.debug('Saving uploaded file {0}'.format(rdf_path))
        file.save(rdf_path)
    except Exception as e:
        logger.error('RDF File {0} could not be saved'.format(rdf_path))
        return jsonify(error={'message': 'RDF File '+str(rdf_path)+'could not be saved', 'reason': str(e)}), 500

    sql_file = os.path.join(temp_dir.name, "import.sql")
    file = open(sql_file, "wt")
    file.write("ld_dir('"+str(import_dir)+"', '*', '"+graph+"');\n")
    file.write("rdf_loader_run();\n")
    file.write("exec('checkpoint');\n")
    file.write("WAIT_FOR_CHILDREN;\n")
    file.close()
    
    ret = run_import(sql_file)
    
    if ret != 0:
        logger.error('isql-v load failed {0}'.format(sql_file))
        return jsonify(error={'message': 'RDF load to triplestore failed '+str(sql_file)}), 500
    
    return jsonify(success=""), 200


if __name__ == '__main__':

    if "API_TOKEN" in os.environ:
        API_TOKEN = os.environ.get('API_TOKEN')
    else:
        print("API_TOKEN NOT SET", file=sys.stderr)
        sys.exit(1) 

    if "API_TOKEN_HEADER_FIELD" in os.environ:
        API_TOKEN_HEADER_FIELD = os.environ.get('API_TOKEN_HEADER_FIELD')
    else:
        print("API_TOKEN_HEADER_FIELD NOT SET", file=sys.stderr)
        sys.exit(1) 

    if "TRIPLESTORE_HOST" in os.environ:
        TRIPLESTORE_HOST = os.environ.get('TRIPLESTORE_HOST')
    else:
        print("TRIPLESTORE_HOST NOT SET", file=sys.stderr)
        sys.exit(1) 

    if "TRIPLESTORE_PORT" in os.environ:
        TRIPLESTORE_PORT = os.environ.get('TRIPLESTORE_PORT')
    else:
        print("TRIPLESTORE_PORT NOT SET", file=sys.stderr)
        sys.exit(1) 

    app.run(host='0.0.0.0', port=5003)
    #serve(app, host='0.0.0.0', port=5003)
