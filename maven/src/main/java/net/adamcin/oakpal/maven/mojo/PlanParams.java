package net.adamcin.oakpal.maven.mojo;

import java.io.File;
import java.util.Collections;
import java.util.List;

import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.InstallHookPolicy;
import net.adamcin.oakpal.core.JcrNs;

public final class PlanParams implements PlanBuilderParams {
    private List<DependencyFilter> preInstallArtifacts = Collections.emptyList();
    private List<File> preInstallFiles = Collections.emptyList();
    private List<String> cndNames = Collections.emptyList();
    private boolean slingNodeTypes;
    private List<JcrNs> jcrNamespaces = Collections.emptyList();
    private List<String> jcrPrivileges = Collections.emptyList();
    private List<ForcedRoot> forcedRoots = Collections.emptyList();
    private List<CheckSpec> checks = Collections.emptyList();
    private List<String> checklists = Collections.emptyList();
    private boolean enablePreInstallHooks;
    private InstallHookPolicy installHookPolicy;

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

    @Override
    public boolean isEnablePreInstallHooks() {
        return enablePreInstallHooks;
    }

    public void setEnablePreInstallHooks(final boolean skipInstallHooks) {
        this.enablePreInstallHooks = skipInstallHooks;
    }

    @Override
    public InstallHookPolicy getInstallHookPolicy() {
        return installHookPolicy;
    }

    public void setInstallHookPolicy(final InstallHookPolicy installHookPolicy) {
        this.installHookPolicy = installHookPolicy;
    }

    @Override
    public String toString() {
        return "PlanParams{" +
                "preInstallArtifacts=" + preInstallArtifacts +
                ", preInstallFiles=" + preInstallFiles +
                ", cndNames=" + cndNames +
                ", slingNodeTypes=" + slingNodeTypes +
                ", jcrNamespaces=" + jcrNamespaces +
                ", jcrPrivileges=" + jcrPrivileges +
                ", forcedRoots=" + forcedRoots +
                ", checks=" + checks +
                ", checklists=" + checklists +
                ", enablePreInstallHooks=" + enablePreInstallHooks +
                ", installHookPolicy=" + installHookPolicy +
                '}';
    }
}
