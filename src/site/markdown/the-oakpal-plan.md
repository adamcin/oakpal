The OakPAL Plan
===============

Behind every Oakpal execution there is a _plan_, even if it is an implicit
one. 

Prior to release 1.4.0, the plan was tightly-coupled to the `oakpal-maven-plugin`'s
`scan` and `scan-many` goals, and was defined by the following parameters:

  * `preInstallArtifacts`: a list of `content-package` artifacts, 
  resolved by maven, to install prior to the scan. A `preInstallFiles` 
  parameter is also supported, to reference content package files downloaded
  or created locally through other means.
  * `forcedRoots`: a list of root paths to create prior to the scan, with optional
  primary type and mixin types specified for each.
  * `jcrPrivileges`: a list of JCR privilege names to define prior to the scan
  in order to satisfy named references in `rep:privileges` properties.
  * `jcrNamespaces`: a list of JCR namespace prefix to URI mappings to register
  in order to support the included forcedRoots and jcrPrivileges parameters, as
  well as any dependencies that might be involved in any adhoc checks.
  * `cndNames`: a list of relative paths to CND files located on the scan 
  classpath. 
  * `slingNodeTypes`: a boolean specifying (for convenience) whether to 
  install any CND files referenced by `Sling-Nodetypes` manifest headers 
  in maven dependency jars.
  * `checklists`: the list of reusable checklists to enforce, which must be defined
  in jar files on the classpath.
  * `checks`: a list of adhoc progress check definitions that may implement new 
  assertions, or override details of checks defined in selected checklists.
  
The _plan_ definition, importantly, *does not* include the list of packages 
that are the subject of the scan.

While it shares many of the same traits as a `checklist`, the _plan_ has 
a couple important differences:

  * There can be only one plan selected for any particular scan execution, 
  whereas zero-to-many checklists can be active for a particular scan.
  * Because of its proximity to the point of execution, the presence of, and 
  alternatives for some _plan_ parameters trade determinism for developer 
  flexibility. Because checklists are designed to be reusable and shareable,
  scan-time discovery and alternative parameters have been largely disallowed.
  * Plans can specify pre-install packages, which provide much greater power
  over the definition of initial repository state, but which carry a lot 
  more overhead that hinders reusability, in the form of download size and 
  lack of visibilty on changes to encapsulated content over time.

## Enter *OPEAR* and the reusable _plan_

In release 1.4.0, the _plan_ has become a reusable artifact in its own right, in
the form of an OPEAR file, which stands for *OakPal Encapsulated Archive*. It is
pronounced _oh-pair_.

Like the implicit _plan_ definition already supported by the `scan` goals, the
OPEAR plan will continue to support pre-install packages and will enforce the
assumption that it is the sole arbiter of parameters for any particular 
scan execution.

Unlike the `scan` goals, however, it does not support scan-time discovery
and resolution of artifacts.

The primary design goal of the OPEAR format is the _deterministic reporting 
of check violations_. 

As in, given the following constraints, the number 
and severity of reported violations should be the same across any number 
of scans of the same sequence of content packages.

  * The JVM environment.
  * The version of oakpal-core.
  * The specified OPEAR file.
  
To this end, oakpal execution tools will attempt to isolate the scan 
classloader wherever possible to include only the oakpal runtime 
dependencies and whatever libraries are bundled within the OPEAR file 
itself, as referenced by its `Bundle-ClassPath` Manifest header.

To build such an artifact, two new goals have been added to the 
`oakpal-maven-plugin`, [`opear-plan`](oakpal-maven-plugin/opear-plan-mojo.html) 
and [`opear-package`](oakpal-maven-plugin/opear-package-mojo.html), as well
as a new lifecycle mapping for `<packaging>opear</packaging>`.

The simplest way to start is to add the `opear-plan` and `opear-package` goals
to an existing oakpal checklist module pom. The opear file will be attached
to the project with a `.opear` extension and will include the project artifact
in its `Bundle-ClassPath` by default.
