ARG NGINX_VER=stable

FROM nginx:${NGINX_VER}-alpine

RUN chown -R nginx:nginx /var/cache/nginx && \
        chown -R nginx:nginx /var/log/nginx && \
        chown -R nginx:nginx /etc/nginx/conf.d
RUN touch /var/run/nginx.pid && \
        chown -R nginx:nginx /var/run/nginx.pid

COPY --chown=nginx:nginx corpus_overview /usr/share/nginx/html/corpus
COPY --chown=nginx:nginx default.conf /etc/nginx/conf.d/default.conf
COPY --chown=nginx:nginx nginx.conf /etc/nginx/nginx.conf

EXPOSE 8080
