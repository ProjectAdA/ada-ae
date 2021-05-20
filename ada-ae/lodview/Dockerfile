# WAR compile of LodView does not work with Openjdk 16 or 17!
ARG OPENJDK_VER=15
ARG MAVEN_VER=3.8.1
# LodView does not run on Tomcat 10
ARG TOMCAT_VER=9.0.46
FROM maven:${MAVEN_VER}-openjdk-${OPENJDK_VER}-slim AS builder
LABEL maintainer="henning.agt@gmail.com"
SHELL ["/bin/bash", "-o", "pipefail", "-e", "-u", "-x", "-c"] 

ARG ONTOLOGY_BASE_URI=http://ada.cinepoetics.org

# Setting the public URL of the sparql endpoint allows to use external services, such as LodLive
ARG PUBLIC_SPARQL_URL=http://ada.cinepoetics.org:9090/sparql

# Set specific LodView commit from Apr 12, 2020 (latest known stable version)
ARG LODVIEW_COMMIT=b06e12683c77caf99cf4e4033085e9ec5750033b

RUN apt-get update && apt-get install -y wget unzip && apt-get autoclean && apt-get autoremove && rm -rf /var/lib/apt/lists/*

RUN cd /tmp
RUN wget -q --show-progress https://github.com/LodLive/LodView/archive/${LODVIEW_COMMIT}.zip && unzip "${LODVIEW_COMMIT}.zip" && mv "LodView-${LODVIEW_COMMIT}" /tmp/LodView
WORKDIR /tmp/LodView

# Set namespace in conf.ttl - note extra slash after ONTOLOGY_BASE_URI
RUN ESCAPED_REPLACE=$(printf '%s\n' "${ONTOLOGY_BASE_URI}/" | sed -e 's/[\/&]/\\&/g') && sed -i "s/conf:IRInamespace <http:\/\/dbpedia.org\/resource\/>/conf:IRInamespace <${ESCAPED_REPLACE}>/" /tmp/LodView/src/main/webapp/WEB-INF/conf.ttl

# Set sparql endpoint in conf.ttl
RUN ESCAPED_REPLACE=$(printf '%s\n' "${PUBLIC_SPARQL_URL}" | sed -e 's/[\/&]/\\&/g') && sed -i "s/conf:endpoint <http:\/\/dbpedia.org\/sparql>/conf:endpoint <${ESCAPED_REPLACE}>/" /tmp/LodView/src/main/webapp/WEB-INF/conf.ttl
RUN mvn clean compile war:war

# Unfortunately, there is no jdk15 version of Tomcat 9
FROM tomcat:${TOMCAT_VER}-jdk16-openjdk-slim-buster
COPY --from=builder /tmp/LodView/target/lodview.war /usr/local/tomcat/webapps/lodview.war
RUN ln -s /usr/local/tomcat/webapps/lodview  /usr/local/tomcat/webapps/ROOT

