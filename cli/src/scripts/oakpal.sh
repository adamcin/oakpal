#!/bin/sh
appHome="$(cd "$(dirname "$(dirname "$0")")" && pwd)"
java -jar "${appHome}"/oakpal-cli-*.jar "$@"

