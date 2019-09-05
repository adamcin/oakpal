OakPAL: Oak Package Acceptance Library
======================================

[![Build Status](https://travis-ci.org/adamcin/oakpal.png)](https://travis-ci.org/adamcin/oakpal)
[![Coverage Status](https://coveralls.io/repos/github/adamcin/oakpal/badge.svg?branch=master)](https://coveralls.io/github/adamcin/oakpal?branch=master)

## About OakPAL

OakPAL was inspired by my continuing attempts to improve the validation features of the [CRX Content Package Deployer Plugin for Jenkins](https://wiki.jenkins-ci.org/display/JENKINS/CRX+Content+Package+Deployer+Plugin). 
It relies on another library I created called granite-client-packman. 
The [validation features](http://adamcin.net/granite-client-packman/apidocs/net/adamcin/granite/client/packman/validation/PackageValidator.html)
                
I added to that library began to hit significant
limitations because I was approaching the task from a static analysis perspective. It's easy to
statically examine a package for the content that it will create in a repository, because this content
exists in the artifact itself, and is predictably shaped by the package's workspace filter. It is also
relatively easy to check package metadata for completeness and conformance to CI assumptions and to
parse DocView XML files to ensure well-formedness.

However, static package analysis leaves some major gaps and edge cases unaddressed:

1. Inability to make deterministic assertions about what existing content will be deleted or changed upon package installation.

2. Inability to account for NodeType constraints.

3. Inability to perform analysis of multiple packages that will be installed in sequence.

After ultimately failing to completely address the above issues with heuristics and broad and/or
high-level restrictions on ACHandling and FilterSet import modes, I finally realized that the only way
to properly test a package is by installing it, and watching (listening?) for what happens to the target
repository.

The CRX Jenkins plugin (and the content-package-maven-plugin) make it possible to install packages in
AEM servers over HTTP, but the simple protocol returns only limited information about errors, you have
to account for transport errors and authentication, and the heavyweight nature of the application makes
it painful to implement a CI process that can reset an AEM server to an exact precondition state.

OakPAL was designed to fill this gap, by providing:

1. A model for repeatable repository state initialization using InitStages and preinstall packages.

2. An OakMachine class with a fluent Builder API that encapsulates the creation of a fresh Oak
repository, state initialization, and package installation for every set of package files.

3. A pluggable listener API with classpath discovery of third-party [Checklists](https://github.com/adamcin/oakpal/blob/master/core/src/main/resources/OAKPAL-INF/checklists/basic.json), 
[ProgressChecks](oakpal-core/apidocs/net/adamcin/oakpal/core/ProgressCheck.html), and [ScriptProgressChecks](oakpal-maven-plugin/writing-a-script-check.html), 
which receive progress tracker events along with read-only access to incremental repository state, and which can report Violations at the end of a scan.


## Docker Image

The oakpal docker image is built on adoptopenjdk11 / OpenJ9. 

    # pull the latest
    docker pull adamcin/oakpal:latest
    
    # print the help text
    docker run -v $(pwd):/work adamcin/oakpal --help
    
    # scan a file using the basic checklist
    cp <somepackage> my-content-package.zip
    docker run -v $(pwd):/work adamcin/oakpal my-content-package.zip



