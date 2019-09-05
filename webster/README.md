OakPAL Webster
==============

> "Webster collects definitions and writes them down."
>                   -- Noah Webster Jr., 1928, whilst hulking out on the first edition of his *An American Dictionary of the English Language*

Most features of OakPAL are focused on testing FileVault content packages as part of build and deployment pipelines, but OakPAL Webster is designed as a tool to help 
manage and curate the *definitions* that compose the initial JCR state that content packages and OakPAL checklists depend on. 

Whereas OakPAL reacts to a transient built artifact, building 
a pristine Oak repository for repeatable execution with quick iteration cycles, Webster is intended for infrequent manual execution, and it reads and writes files that are normally committed 
to git with the rest of your application source code, while incorporating JCR state from the content repositories of locally-installed Oak-based applications like Sling or AEM. 

This initial JCR state encompasses these definitions:

* _Namespaces_: In JCR, namespaces are represented by both a *prefix* and a *URI*. Namespaces are a common dependency of any fully-qualified JCR definition, like a node type, 
privilege, or qualified node name. In some cases, an existing uri can be remapped to a different prefix, but this is rare and not always allowed. OakPAL attempts to 
support remapping wherever possible, but *best practice* recommends that you treat the prefix as a globally standardized 
attribute of the URI.

* _Privileges_: JCR defines some basic privileges for use in Access Control, like `jcr:read` and `rep:write`. Some applications, like AEM, define additional privileges, 
like `crx:replicate`. OakPAL doesn't currently evaluate Access Control restrictions, but it does require that any privileges referenced by `rep:policy` nodes are registered by a checklist 
or by a content package containing a `META-INF/vault/privileges.xml` file.

* _Node Types_: Every node in a JCR repository must have a primary type (a subtype of `nt:base`) and zero-or-more mixin types. The node type is a critical definition to reproduce accurately 
during an OakPAL execution, because it restricts where a node can be installed in the repository, and it can restrict the property names and values set on the node when a package import
attempts to save the change. Content packages can provide required node type definitions in a `META-INF/vault/nodetypes.cnd` file that will be used only to register new definitions, without 
redefining existing definitions.

* _Root Nodes_: Also known as "Structure Nodes", root nodes, when appropriately typed, may contribute to initial JCR state required for particular checks. OakPAL Checklists can arbitrarily 
declare that a node of any defined type can exist at any arbitrary path. This allows for testing a content package that might attempt to add children to that node, in order to enforce node
type constraints during a dry run.

OakPAL assumes that built-in Oak definitions are present in all execution contexts, and therefore not exported to sidecar definition formats like 
`nodetypes.cnd`, `privileges.xml`, and `checklist.json`.

