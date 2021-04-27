ARG DEBIAN_VER=10.9-slim
FROM debian:${DEBIAN_VER}
LABEL maintainer="henning.agt@gmail.com"
SHELL ["/bin/bash", "-o", "pipefail", "-e", "-u", "-x", "-c"]

# Set specific advene commit from Apr 16, 2021 - Release 3.12
ARG ADVENE_COMMIT=c238e2c13f501afd0a2ed355bb81e6ad2debef51

ARG ADAVA_HOME=/opt/ada-va
ARG ADAVA_UID="50000"
ARG ADAVA_GID="50000"

ENV DEBIAN_FRONTEND noninteractive

#Install required packages for Advene. Normally more are required, but the following are sufficient to run the AdA RDF Exporter
RUN apt-get update && apt-get install -y wget unzip python3 python3-pip python3-gi gir1.2-gtk-3.0 python3-gst-1.0 python3-gi-cairo gir1.2-rsvg-2.0 && apt-get autoclean && apt-get autoremove && rm -rf /var/lib/apt/lists/*

RUN pip3 install --trusted-host pypi.python.org Flask==1.1.2 waitress==2.0.0 flask-compress==1.9.0

ENV ADAVA_HOME=${ADAVA_HOME}
ENV ADVENE_COMMIT=${ADVENE_COMMIT}

ENV ADAVA_USER_HOME_DIR=/home/ada-va

RUN addgroup --gid "${ADAVA_GID}" "ada-va" && \
    adduser --quiet "ada-va" --uid "${ADAVA_UID}" \
        --gid "${ADAVA_GID}" \
        --home "${ADAVA_USER_HOME_DIR}"

RUN mkdir -pv "${ADAVA_HOME}"; \
    mkdir -pv "${ADAVA_HOME}/cache"; \
    chown -R "ada-va:ada-va" "${ADAVA_HOME}";

RUN mkdir -p ${ADAVA_USER_HOME_DIR}/.local/
RUN wget -q --show-progress https://github.com/oaubert/advene/archive/${ADVENE_COMMIT}.zip && unzip "${ADVENE_COMMIT}.zip" && mv "advene-${ADVENE_COMMIT}" advene && mv advene ${ADAVA_USER_HOME_DIR}/.local/ && rm "${ADVENE_COMMIT}.zip"
RUN chown -R "ada-va:ada-va" "${ADAVA_USER_HOME_DIR}" 

COPY --chown=ada-va:ada-va advene_service.py ${ADAVA_USER_HOME_DIR}/.local/bin/advene_service.py
RUN chmod +x ${ADAVA_USER_HOME_DIR}/.local/bin/advene_service.py

ENV PATH="${ADAVA_USER_HOME_DIR}/.local/bin:${ADAVA_USER_HOME_DIR}/.local/advene/bin:${PATH}"

WORKDIR ${ADAVA_USER_HOME_DIR}/.local/bin
USER ${ADAVA_UID} 

ENTRYPOINT ["advene_service.py"] 
