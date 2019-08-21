/*
 * Copyright 2019 Mark Adamcin
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

import org.apache.maven.model.Dependency;
import org.junit.Test;

import static org.junit.Assert.*;

public class DependencyFilterTest {

    @Test
    public void testSetters() {
        DependencyFilter filter = new DependencyFilter();
        assertNull("groupId is null", filter.getGroupId());
        assertNull("artifactId is null", filter.getArtifactId());
        assertNull("version is null", filter.getVersion());
        assertNull("classifier is null", filter.getClassifier());
        assertNull("type is null", filter.getType());
        filter.setGroupId("groupId");
        assertEquals("groupId is", "groupId", filter.getGroupId());
        filter.setArtifactId("artifactId");
        assertEquals("artifactId is", "artifactId", filter.getArtifactId());
        filter.setVersion("version");
        assertEquals("version is", "version", filter.getVersion());
        filter.setType("type");
        assertEquals("type is", "type", filter.getType());
        filter.setClassifier("classifier");
        assertEquals("classifier is", "classifier", filter.getClassifier());

        filter
                .withGroupId("dIpuorg")
                .withArtifactId("dItcafitra")
                .withVersion("noisrev")
                .withType("epyt")
                .withClassifier("reifissalc");

        assertEquals("groupId is", "dIpuorg", filter.getGroupId());
        assertEquals("artifactId is", "dItcafitra", filter.getArtifactId());
        assertEquals("version is", "noisrev", filter.getVersion());
        assertEquals("type is", "epyt", filter.getType());
        assertEquals("classifier is", "reifissalc", filter.getClassifier());

        final Dependency dependency = filter.toDependency();

        assertEquals("groupId is", "dIpuorg", dependency.getGroupId());
        assertEquals("artifactId is", "dItcafitra", dependency.getArtifactId());
        assertEquals("version is", "noisrev", dependency.getVersion());
        assertEquals("type is", "epyt", dependency.getType());
        assertEquals("classifier is", "reifissalc", dependency.getClassifier());
    }

    DependencyFilter newFilter() {
        return new DependencyFilter();
    }

    @Test
    public void testTest() {
        assertFalse("test null false", newFilter().test(null));
        assertTrue("empty match on empty", newFilter().test(newFilter().toDependency()));
        assertFalse("groupId no match on empty", newFilter().withGroupId("groupId").test(newFilter().toDependency()));
        assertTrue("groupId match on groupId", newFilter().withGroupId("groupId").test(newFilter().withGroupId("groupId").toDependency()));
        assertFalse("artifactId no match on empty", newFilter().withArtifactId("artifactId").test(newFilter().toDependency()));
        assertTrue("artifactId match on artifactId", newFilter().withArtifactId("artifactId").test(newFilter().withArtifactId("artifactId").toDependency()));
        assertFalse("version no match on empty", newFilter().withVersion("version").test(newFilter().toDependency()));
        assertTrue("version match on version", newFilter().withVersion("version").test(newFilter().withVersion("version").toDependency()));
        assertFalse("type no match on empty", newFilter().withType("type").test(newFilter().toDependency()));
        assertTrue("type match on type", newFilter().withType("type").test(newFilter().withType("type").toDependency()));
        assertFalse("classifier no match on empty", newFilter().withClassifier("classifier").test(newFilter().toDependency()));
        assertTrue("classifier match on classifier", newFilter().withClassifier("classifier").test(newFilter().withClassifier("classifier").toDependency()));
    }
}