# Software Stack of the AdA Annotation Explorer

In this folder we open source the complete software stack of the Annotation Explorer web application and backend components. Each of the folders contains the source code and a Dockerfile to build a docker image and run a docker container with the respective service.

## Triplestore

The folder [triplestore](triplestore/) contains a configured RDF store (we are using Openlink Virtuoso Open-Source Edition) for management of the ontology, the corpus metadata, the manual annotation and the automatically generated annotations.

## REST-API

The folder [rest_api](rest_api/) contains a web service (implemented in Java) that offers a range of functions to access and query the data contained in the triplestore. It delivers metadata and annotation data in JSON and JSON-LD formats for further processing in the frontend.

