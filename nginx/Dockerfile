FROM nginx:1.27-alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY conf.d/myapp.conf /etc/nginx/conf.d/myapp.conf
COPY conf.d/service-url.inc /etc/nginx/conf.d/service-url.inc

EXPOSE 80 443
