FROM ubuntu:20.04

RUN apt-get update \
        && apt-get install -y build-essential autotools-dev autoconf automake unzip wget git net-tools libtool flex bison gperf gawk m4 libssl-dev libreadline-dev openssl \
        && git clone https://github.com/openlink/virtuoso-opensource.git \
        && cd virtuoso-opensource \
        && ./autogen.sh \
        && export CFLAGS="-O2 -m64" && ./configure --disable-bpel-vad --enable-conductor-vad --enable-fct-vad --disable-dbpedia-vad --disable-demo-vad --disable-isparql-vad --disable-ods-vad --disable-sparqldemo-vad --disable-syncml-vad --disable-tutorial-vad --with-readline --program-transform-name="s/isql/isql-v/" \
        && make && make install \
        && ln -s /usr/local/virtuoso-opensource/var/lib/virtuoso/db /data \
        && cd .. \
        && rm -r /virtuoso-opensource \
        && apt remove --purge -y build-essential autotools-dev autoconf automake unzip wget net-tools libtool flex bison gperf gawk m4 libssl-dev libreadline-dev \
        && apt autoremove -y \
        && apt autoclean

RUN apt-get update && apt-get install -y libreadline-dev
ENV PATH /usr/local/virtuoso-opensource/bin/:$PATH

COPY virtuoso.ini /root/virtuoso.ini
COPY startup.sh /root/startup.sh

VOLUME /data
WORKDIR /data
EXPOSE 8890

CMD ["/bin/bash", "/root/startup.sh"]

