FROM alpine:latest AS builder

RUN apk add -u openjdk21-jdk maven

COPY . /app
WORKDIR /app

RUN mvn package

FROM alpine:latest
RUN apk add -u openjdk21-jre-headless

RUN mkdir /app
COPY --from=builder /app/app/target/bowlby /app/bowlby

VOLUME /data
CMD ["/app/bowlby", "-d", "/data"]
