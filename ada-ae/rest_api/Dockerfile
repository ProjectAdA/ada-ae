ARG OPENJDK_VER=17
ARG MAVEN_VER=3.8.1

FROM maven:${MAVEN_VER}-openjdk-${OPENJDK_VER}-slim AS builder

COPY src /tmp/build/src
COPY pom.xml /tmp/build/pom.xml 
WORKDIR /tmp/build
RUN mvn clean package 


FROM openjdk:${OPENJDK_VER}-slim-buster

ARG ADAAE_HOME=/opt/ada-ae
ARG ADAAE_UID="50000"
ARG ADAAE_GID="50000"

ENV ADAAE_HOME=${ADAAE_HOME}

ENV ADAAE_USER_HOME_DIR=/home/ada-ae

RUN addgroup --gid "${ADAAE_GID}" "ada-ae" && \
    adduser --quiet "ada-ae" --uid "${ADAAE_UID}" \
        --gid "${ADAAE_GID}" \
        --home "${ADAAE_USER_HOME_DIR}"

RUN mkdir -pv ${ADAAE_HOME}/rest/

COPY --from=builder --chown=ada-ae:ada-ae /tmp/build/target/ada_rest_api.jar  ${ADAAE_HOME}/rest/ada_rest_api.jar
COPY --from=builder --chown=ada-ae:ada-ae /tmp/build/target/lib ${ADAAE_HOME}/rest/lib

USER ${ADAAE_UID}
WORKDIR ${ADAAE_HOME}/rest
EXPOSE 7002

ENTRYPOINT ["java", "-jar", "ada_rest_api.jar"]

