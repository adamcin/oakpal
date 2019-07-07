CLI architecture
================

* Docker-oriented: the Dockerfile will live in the root of the oakpal repo, and it will run a multi-stage build to execute maven, then extract the cli distributable.
* Multi-stage, and multi-space (base command home dir + machine workspace + scan workspace)

see [`Main --help`](src/main/resources/net/adamcin/oakpal/cli/help.txt)

