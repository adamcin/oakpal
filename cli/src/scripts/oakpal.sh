#!/bin/sh
appHome="$(cd "$(dirname "$(dirname "$0")")" && pwd)"
java ${JAVA_OPTS} -jar "${appHome}"/oakpal-cli-*.jar "$@"

