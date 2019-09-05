CLI architecture
================

Main-Class: net.adamcin.oakpal.cli.Main

see [`Main --help`](src/main/resources/net/adamcin/oakpal/cli/help.txt)

## Docker Image

* Docker-oriented: the Dockerfile will live in the root of the oakpal repo, and it will run a multi-stage build to execute maven, then extract the cli distributable.

The oakpal docker image is built on adoptopenjdk11 / OpenJ9. 

    # pull the latest
    docker pull adamcin/oakpal:latest
    
    # print the help text
    docker run -v $(pwd):/work adamcin/oakpal --help
    
    # scan a file using the basic checklist
    cp <somepackage> my-content-package.zip
    docker run -v $(pwd):/work adamcin/oakpal my-content-package.zip
    
    
