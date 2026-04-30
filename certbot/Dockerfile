FROM certbot/certbot:latest

RUN apk add --no-cache aws-cli

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]