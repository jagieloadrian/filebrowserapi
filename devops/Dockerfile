FROM bellsoft/liberica-openjdk-alpine:17.0.5
ARG PATH
ENV DISK_PATH=$PATH
RUN addgroup app && adduser -D -G app -h /home/app -s /sbin/nologin app
RUN mkdir /var/app
USER app
COPY --chown=app:app ./filebrowserapi.jar ./application.yml /home/app/
EXPOSE 8080
ENTRYPOINT java -jar home/app/filebrowserapi.jar --spring.config.location=/home/app/application.yml --port 8080 --DISK_PATH=$DISK_PATH
