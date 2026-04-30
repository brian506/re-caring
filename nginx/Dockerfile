FROM nginx:1.27-alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY conf.d/myapp.conf /etc/nginx/conf.d/myapp.conf

EXPOSE 80 443
