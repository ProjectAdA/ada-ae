ARG VIRTUOSO_HOME=/opt/virtuoso-opensource

FROM ubuntu:20.04 AS builder

# Set a commit in Virtuoso develop/7 tree (Feb 8, 2021) that was tested during development
ARG VIRTUOSO_COMMIT=3ff1d4b3de3977337baf909c264968b9f70b7d2c 

# Alternative: Set a commit in Virtuoso stable/7 tree: Release 7.2.5.1 Aug 16, 2018
#ARG VIRTUOSO_COMMIT=17c4ba1d5825822d8380ef8d9f978e2d57f4768f 

ENV VIRTUOSO_COMMIT=${VIRTUOSO_COMMIT}

ARG VIRTUOSO_HOME
ENV VIRTUOSO_HOME=${VIRTUOSO_HOME}

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update \
        && apt-get install -y build-essential autotools-dev autoconf automake unzip wget git net-tools libtool flex bison gperf gawk m4 libssl-dev libreadline-dev openssl \
		&& cd /tmp  && wget -q --show-progress https://github.com/openlink/virtuoso-opensource/archive/${VIRTUOSO_COMMIT}.zip \
		&& unzip "${VIRTUOSO_COMMIT}.zip" \
		&& cd "virtuoso-opensource-${VIRTUOSO_COMMIT}" \
		&& ./autogen.sh \
		&& export CFLAGS="-O2 -m64" && ./configure --prefix=${VIRTUOSO_HOME} --disable-bpel-vad --enable-conductor-vad --enable-fct-vad --disable-dbpedia-vad --disable-demo-vad --disable-isparql-vad --disable-ods-vad --disable-sparqldemo-vad --disable-syncml-vad --disable-tutorial-vad --with-readline --program-transform-name="s/isql/isql-v/" \
		&& make && make install \
		&& cd /tmp && rm "${VIRTUOSO_COMMIT}.zip" && rm -rf "virtuoso-opensource-${VIRTUOSO_COMMIT}"

FROM ubuntu:20.04
ARG VIRTUOSO_HOME
ENV VIRTUOSO_HOME=${VIRTUOSO_HOME}

RUN apt-get update \
        && apt-get install -y libssl1.1 wget libreadline8 python3 python3-pip \
        && apt-get clean \
        && rm -rf /var/lib/apt/lists/*

RUN pip3 install --trusted-host pypi.python.org Flask==1.1.2 waitress==2.0.0 flask-compress==1.9.0

COPY --from=builder ${VIRTUOSO_HOME} ${VIRTUOSO_HOME}

ARG ADAAE_UID="50000"
ARG ADAAE_GID="50000"
ENV ADAAE_USER_HOME_DIR=/home/ada-ae

RUN addgroup --gid "${ADAAE_GID}" "ada-ae" && \
    adduser --quiet "ada-ae" --uid "${ADAAE_UID}" \
        --gid "${ADAAE_GID}" \
        --home "${ADAAE_USER_HOME_DIR}"

RUN mkdir /import
RUN chown -R "ada-ae:ada-ae" /import

RUN mkdir -p ${ADAAE_USER_HOME_DIR}/.local/
COPY --chown=ada-ae:ada-ae rdf_uploader.py ${ADAAE_USER_HOME_DIR}/.local/bin/rdf_uploader.py
RUN chmod +x ${ADAAE_USER_HOME_DIR}/.local/bin/rdf_uploader.py

ENV PATH="${ADAAE_USER_HOME_DIR}/.local/bin:${VIRTUOSO_HOME}/bin:${PATH}" 

USER ${ADAAE_UID}
WORKDIR ${ADAAE_USER_HOME_DIR}/.local/bin

VOLUME /import

EXPOSE 5003

ENTRYPOINT ["rdf_uploader.py"]
