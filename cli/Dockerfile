# prototype dockerfile for faster iterations
FROM adoptopenjdk/openjdk11-openj9:alpine-slim
RUN mkdir -p /app/oakpal-cli
COPY target/oakpal-cli-*-dist.tar.gz /app
RUN tar --strip-components 1 -C /app/oakpal-cli -zxf /app/oakpal-cli-*-dist.tar.gz \
      && rm -f /app/oakpal-cli-*-dist.tar.gz

ENV JAVA_OPTS ""
ENV OAKPAL_OPEAR "."
RUN mkdir -p /work
WORKDIR /work
ENTRYPOINT ["/app/oakpal-cli/bin/oakpal.sh"]

