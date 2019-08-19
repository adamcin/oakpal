#!/bin/sh
appHome="$(cd "$(dirname "$(dirname "$0")")" && pwd)"
oakpalCp=$(ls "${appHome}"/oakpal-cli-*.jar)
for libjar in $(find "${appHome}/lib" -name '*.jar' -type f); do
  oakpalCp="${oakpalCp}:${libjar}"
done
java ${JAVA_OPTS} -cp "${oakpalCp}" net.adamcin.oakpal.cli.Main "$@"

