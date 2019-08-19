#!/bin/sh
appHome="$(cd "$(dirname "$(dirname "$0")")" && pwd)"
oakpalCp=$(ls "${appHome}"/oakpal-cli-*.jar)
while IFS= read -d $'\0' -r libjar ; do
  oakpalCp="${oakpalCp}:${libjar}"
done < <(find "${appHome}/lib" -name '*.jar' -type f -print0)
java ${JAVA_OPTS} -cp "${oakpalCp}" net.adamcin.oakpal.cli.Main "$@"

