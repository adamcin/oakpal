package net.adamcin.oakpal.maven.mojo;

import java.io.File;
import java.util.List;

import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.ForcedRoot;
import net.adamcin.oakpal.core.InstallHookPolicy;
import net.adamcin.oakpal.core.JcrNs;

public interface PlanBuilderParams {
    List<DependencyFilter> getPreInstallArtifacts();

    List<File> getPreInstallFiles();

    List<String> getCndNames();

    boolean isSlingNodeTypes();

    List<JcrNs> getJcrNamespaces();

    List<String> getJcrPrivileges();

    List<ForcedRoot> getForcedRoots();

    List<CheckSpec> getChecks();

    List<String> getChecklists();

    boolean isEnablePreInstallHooks();

    InstallHookPolicy getInstallHookPolicy();
}
