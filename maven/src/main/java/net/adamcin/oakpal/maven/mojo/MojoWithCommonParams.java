package net.adamcin.oakpal.maven.mojo;

import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * Common getters for Mojo context objects.
 */
public interface MojoWithCommonParams extends Mojo {
    /**
     * Get the current mojo execution.
     *
     * @return the mojo execution
     */
    MojoExecution getExecution();

    /**
     * Get the current maven session.
     *
     * @return the maven session
     */
    MavenSession getSession();

    /**
     * Get the maven settings.
     *
     * @return the maven settings
     */
    Settings getSettings();

    /**
     * Get the maven project if available.
     *
     * @return the maven project if available
     */
    Optional<MavenProject> getProject();
}
