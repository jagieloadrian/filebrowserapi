# syntax=docker/dockerfile:1
# Build: docker build -f devops/Dockerfile -t filebrowserapi .
# ponytail: glibc JDK image for build stage, protoc binary from Maven does not run on musl/alpine
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew :FilebrowserBE:bootJar --no-daemon --console=plain

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app -h /home/app app \
    && mkdir /data && chown app:app /data
USER app
COPY --from=build /src/FilebrowserBE/build/libs/filebrowserapi.jar /home/app/filebrowserapi.jar
# web = REST on 8080, server = gRPC on 9090
ENV SPRING_PROFILES_ACTIVE=web \
    APPLICATION_PATH=/data
VOLUME /data
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "/home/app/filebrowserapi.jar"]
