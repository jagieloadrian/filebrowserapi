FROM nginxinc/nginx-unprivileged:1.30.6-alpine
ENV NGINX_ENVSUBST_OUTPUT_DIR=/tmp
COPY nginx.conf.template /etc/nginx/templates/nginx.conf.template
COPY build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/
USER 10001:10001
CMD ["nginx", "-c", "/tmp/nginx.conf", "-g", "daemon off;"]
