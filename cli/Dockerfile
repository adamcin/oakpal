# prototype dockerfile
FROM adoptopenjdk/openjdk8:alpine
COPY --from=registry.hub.docker.com/adamcin/jvshim /go/bin/jvshim /usr/bin/java
RUN mkdir -p /app/oakpal-cli
COPY target/oakpal-cli-*-dist.tar.gz /app
RUN tar --strip-components 1 -C /app/oakpal-cli -zxf /app/oakpal-cli-*-dist.tar.gz \
      && rm -f /app/oakpal-cli-*-dist.tar.gz

ENV JAVA_OPTS ""
ENV OAKPAL_OPEAR "."
RUN mkdir -p /work
WORKDIR /work
ENTRYPOINT ["/app/oakpal-cli/bin/oakpal.sh"]

