ARG NGINX_VER=stable

FROM nginx:${NGINX_VER}-alpine
LABEL maintainer="henning.agt@gmail.com"

ARG ONTOLOGY_BASE_URI=http://ada.cinepoetics.org

RUN chown -R nginx:nginx /var/cache/nginx && \
        chown -R nginx:nginx /var/log/nginx && \
        chown -R nginx:nginx /etc/nginx/conf.d
RUN touch /var/run/nginx.pid && \
        chown -R nginx:nginx /var/run/nginx.pid

COPY --chown=nginx:nginx explorer /usr/share/nginx/html/explorer
COPY --chown=nginx:nginx default.conf /etc/nginx/conf.d/default.conf
COPY --chown=nginx:nginx nginx.conf /etc/nginx/nginx.conf

RUN echo "const baseURL = '${ONTOLOGY_BASE_URI}';" >/usr/share/nginx/html/explorer/url_config.js

EXPOSE 8080
