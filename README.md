OakPAL: Oak Package Acceptance Library
======================================

[![Build Status](https://travis-ci.org/adamcin/oakpal.png)](https://travis-ci.org/adamcin/oakpal)

[![Coverage Status](https://coveralls.io/repos/github/adamcin/oakpal/badge.svg?branch=master)](https://coveralls.io/github/adamcin/oakpal?branch=master)

## Docker Image

The oakpal docker image is built on adoptopenjdk11 / OpenJ9. 

    # pull the latest
    docker pull adamcin/oakpal:latest
    
    # print the help text
    docker run -v $(pwd):/work adamcin/oakpal --help
    
    # scan a file using the basic checklist
    cp <somepackage> my-content-package.zip
    docker run -v $(pwd):/work adamcin/oakpal my-content-package.zip

## [Maven Site Docs](http://adamcin.net/oakpal/index.html)

## [oakpal-maven-plugin](maven/README.md)


