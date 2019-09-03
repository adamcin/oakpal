/*
 * Copyright 2018 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.oakpal.maven.mojo;

import java.util.function.Predicate;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.StringUtils;

/**
 * Simple pojo for matching resolved dependencies based on limited coordinate specification.
 */
public class DependencyFilter implements Predicate<Dependency> {
    private String groupId;
    private String artifactId;
    private String version;
    private String type;
    private String classifier;

    /**
     * Predicate function for finding first matching dependency in a stream.
     *
     * @param dependency the dependency to test.
     * @return true if this filter matches the dependency coordinates.
     */
    @Override
    public boolean test(final Dependency dependency) {
        return dependency != null &&
                (StringUtils.isEmpty(groupId) || StringUtils.equals(groupId, dependency.getGroupId())) &&
                (StringUtils.isEmpty(artifactId) || StringUtils.equals(artifactId, dependency.getArtifactId())) &&
                (StringUtils.isEmpty(version) || StringUtils.equals(version, dependency.getVersion())) &&
                (StringUtils.isEmpty(type) || StringUtils.equals(type, dependency.getType())) &&
                (StringUtils.isEmpty(classifier) || StringUtils.equals(classifier, dependency.getClassifier()));
    }

    /**
     * If this filter does not match a dependency, try creating one from the provided parameters and resolving it.
     *
     * @return a dependency from scratch
     */
    public Dependency toDependency() {
        return toDependency(null);
    }

    /**
     * If this filter does not match a dependency, try creating one from the provided parameters and resolving it.
     *
     * @param scope set the dependency scope
     * @return a dependency from scratch
     */
    public Dependency toDependency(final String scope) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setType(type);
        dependency.setClassifier(classifier);
        if (scope != null) {
            dependency.setScope(scope);
        }
        return dependency;
    }

    public DependencyFilter withGroupId(final String groupId) {
        this.setGroupId(groupId);
        return this;
    }

    public DependencyFilter withArtifactId(final String artifactId) {
        this.setArtifactId(artifactId);
        return this;
    }

    public DependencyFilter withVersion(final String version) {
        this.setVersion(version);
        return this;
    }

    public DependencyFilter withType(final String type) {
        this.setType(type);
        return this;
    }

    public DependencyFilter withClassifier(final String classifier) {
        this.setClassifier(classifier);
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(final String classifier) {
        this.classifier = classifier;
    }
}
