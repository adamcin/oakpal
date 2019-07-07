package net.adamcin.oakpal.maven.mojo;

import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;

abstract class AbstractCommonMojo extends AbstractMojo implements MojoWithCommonParams, MojoWithRepositoryParams {
    /**
     * package private for tests.
     */
    @Component
    RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    protected MojoExecution execution;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    @Parameter(defaultValue = "${project}", readonly = true, required = false)
    protected MavenProject project;

    @Override
    public Optional<MavenProject> getProject() {
        return Optional.ofNullable(project);
    }

    @Override
    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    @Override
    public MojoExecution getExecution() {
        return execution;
    }

    @Override
    public MavenSession getSession() {
        return session;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }
}
