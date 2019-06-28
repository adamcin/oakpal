package net.adamcin.oakpal.maven.mojo;

import java.io.File;
import java.util.List;

import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.JcrNs;

public final class DefaultPlanBuilderParams implements PlanBuilderParams {
    private List<DependencyFilter> preInstallArtifacts;
    private List<File> preInstallFiles;
    private List<String> cndNames;
    private boolean slingNodeTypes;
    private List<JcrNs> jcrNamespaces;
    private List<String> jcrPrivileges;
    private List<ForcedRoot> forcedRoots;
    private List<CheckSpec> checks;
    private List<String> checklists;

    @Override
    public List<DependencyFilter> getPreInstallArtifacts() {
        return preInstallArtifacts;
    }

    public void setPreInstallArtifacts(final List<DependencyFilter> preInstallArtifacts) {
        this.preInstallArtifacts = preInstallArtifacts;
    }

    @Override
    public List<File> getPreInstallFiles() {
        return preInstallFiles;
    }

    public void setPreInstallFiles(final List<File> preInstallFiles) {
        this.preInstallFiles = preInstallFiles;
    }

    @Override
    public List<String> getCndNames() {
        return cndNames;
    }

    public void setCndNames(final List<String> cndNames) {
        this.cndNames = cndNames;
    }

    @Override
    public boolean isSlingNodeTypes() {
        return slingNodeTypes;
    }

    public void setSlingNodeTypes(final boolean slingNodeTypes) {
        this.slingNodeTypes = slingNodeTypes;
    }

    @Override
    public List<JcrNs> getJcrNamespaces() {
        return jcrNamespaces;
    }

    public void setJcrNamespaces(final List<JcrNs> jcrNamespaces) {
        this.jcrNamespaces = jcrNamespaces;
    }

    @Override
    public List<String> getJcrPrivileges() {
        return jcrPrivileges;
    }

    public void setJcrPrivileges(final List<String> jcrPrivileges) {
        this.jcrPrivileges = jcrPrivileges;
    }

    @Override
    public List<ForcedRoot> getForcedRoots() {
        return forcedRoots;
    }

    public void setForcedRoots(final List<ForcedRoot> forcedRoots) {
        this.forcedRoots = forcedRoots;
    }

    @Override
    public List<CheckSpec> getChecks() {
        return checks;
    }

    public void setChecks(final List<CheckSpec> checks) {
        this.checks = checks;
    }

    @Override
    public List<String> getChecklists() {
        return checklists;
    }

    public void setChecklists(final List<String> checklists) {
        this.checklists = checklists;
    }
}
