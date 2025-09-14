FROM debian:13 AS builder

RUN apt-get update && \
  apt-get install -y openjdk-25-jdk-headless maven

COPY . /app
WORKDIR /app

RUN mvn package

FROM debian:13
RUN apt-get update && \
  apt-get install -y openjdk-25-jre-headless

RUN mkdir /app
COPY --from=builder /app/app/target/bowlby /app/bowlby

VOLUME /data
CMD ["/app/bowlby", "-d", "/data"]
