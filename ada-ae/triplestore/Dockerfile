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
        && apt-get install -y libssl1.1 wget libreadline8 \
        && apt-get clean \
        && rm -rf /var/lib/apt/lists/*

COPY --from=builder ${VIRTUOSO_HOME} ${VIRTUOSO_HOME}
RUN mkdir ${VIRTUOSO_HOME}/initdb.d
COPY load-ada-ontology.sh ${VIRTUOSO_HOME}/initdb.d/
COPY setup-user-rights.sh ${VIRTUOSO_HOME}/initdb.d/
COPY virtuoso-entrypoint.sh /virtuoso-entrypoint.sh
COPY virtuoso.ini /virtuoso.ini

RUN mkdir /database
RUN mkdir /import
RUN mkdir /settings

ARG ADAAE_UID="50000"
ARG ADAAE_GID="50000"
ENV ADAAE_USER_HOME_DIR=/home/ada-ae

RUN addgroup --gid "${ADAAE_GID}" "ada-ae" && \
    adduser --quiet "ada-ae" --uid "${ADAAE_UID}" \
        --gid "${ADAAE_GID}" \
        --home "${ADAAE_USER_HOME_DIR}"

RUN chown -R "ada-ae:ada-ae" /database
RUN chown -R "ada-ae:ada-ae" /import
RUN chown -R "ada-ae:ada-ae" /settings
RUN chmod +x /virtuoso-entrypoint.sh

VOLUME /database
VOLUME /import

ENV PATH ${VIRTUOSO_HOME}/bin:$PATH

USER ${ADAAE_UID}
WORKDIR /database

EXPOSE 8890
EXPOSE 1111

ENTRYPOINT ["/virtuoso-entrypoint.sh"]
CMD ["start"]

