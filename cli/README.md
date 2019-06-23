CLI architecture
================

* Docker-oriented: the Dockerfile will live in the root of the oakpal repo, and it will run a multi-stage build to execute maven, then extract the cli distributable.
* Multi-stage, and multi-space (base command home dir + machine workspace + scan workspace)

## Environment Variables

`OAKPAL_HOME`: defines base command directory. `${OAKPAL_HOME}/bin/oakpal` should execute the tool with standard classpath behavior of listing `${OAKPAL_HOME}/lib/*.jar`. 
The contents of `OAKPAL_HOME` should be established in the base docker image. This base image should also set the `ENTRYPOINT` to properly execute the `bin/oakpal` command
with the appropriate `OAKPAL_PLAN` directory referenced

`OAKPAL_PLAN`: defines the directory where additional modules, checklists, configurations, and pre-install packages are downloaded prior to a scan. Downstream Dockerfiles 
should use the base oakpal command to prepare the OAKPAL_PLAN directory using `RUN` directives.



