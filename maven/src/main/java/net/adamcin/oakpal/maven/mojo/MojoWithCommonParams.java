package net.adamcin.oakpal.maven.mojo;

import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

public interface MojoWithCommonParams extends Mojo {
    MojoExecution getExecution();

    MavenSession getSession();

    Settings getSettings();

    Optional<MavenProject> getProject();


}
