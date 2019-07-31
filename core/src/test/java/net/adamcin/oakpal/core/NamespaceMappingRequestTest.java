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

package net.adamcin.oakpal.core;

import org.apache.jackrabbit.oak.spi.namespace.NamespaceConstants;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.junit.Test;

import javax.jcr.NamespaceRegistry;
import java.util.Arrays;
import java.util.List;

import static net.adamcin.oakpal.core.Fun.inferTest1;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NamespaceMappingRequestTest {

    private static final String NS_FOO_PREFIX = "foo";
    private static final String NS_BAR_PREFIX = "bar";
    private static final String NS_FOO_URI = "http://foo.com";
    private static final String NS_BAR_URI = "http://bar.com";

    private final JcrNs NS_FOO = JcrNs.create(NS_FOO_PREFIX, NS_FOO_URI);
    private final JcrNs NS_BAR = JcrNs.create(NS_BAR_PREFIX, NS_BAR_URI);
    private final List<JcrNs> addNs = Arrays.asList(NS_FOO, NS_BAR);
    private final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(addNs);
    private final NamePathResolver resolver = new DefaultNamePathResolver(mapping);

    private NamespaceMappingRequest.Builder builder() {
        return new NamespaceMappingRequest.Builder();
    }

    @Test
    public void testBuildWithNoRetention() {
        final NamespaceMappingRequest request = builder().build();
        assertTrue("no retain results in empty list", request.resolveToJcrNs(mapping).isEmpty());
    }

    @Test
    public void testBuildWithRetainPrefix() {
        final NamespaceMappingRequest request = builder()
                .withRetainPrefix(NamespaceRegistry.PREFIX_JCR)
                .withRetainPrefix(NS_FOO_PREFIX).build();
        assertTrue("foo mapping is retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals)));
        assertFalse("non-foo mappings are not retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals).negate()));
    }

    @Test
    public void testBuildWithRetainUri() {
        final NamespaceMappingRequest request = builder()
                .withRetainUri(NamespaceRegistry.NAMESPACE_JCR)
                .withRetainUri(NS_FOO_URI)
                .build();
        assertTrue("foo mapping is retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals)));
        assertFalse("non-foo mappings are not retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals).negate()));
    }

    @Test
    public void testBuildWithRetainBuiltins() {
        final NamespaceMappingRequest request = builder()
                .withRetainBuiltins(true)
                .withRetainPrefix(NamespaceRegistry.PREFIX_JCR)
                .withRetainPrefix(NS_FOO_PREFIX)
                .build();
        request.resolveToJcrNs(mapping).stream().filter(Result::isFailure).findFirst().map(Result::teeLogError);
        assertTrue("jcr mapping is retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .map(JcrNs::getPrefix)
                        .anyMatch(inferTest1(NamespaceRegistry.PREFIX_JCR::equals)));
        assertTrue("foo mapping is retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals)));
        assertFalse("bar mapping is not retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_BAR::equals)));
    }

    @Test
    public void testBuildWithQName() throws Exception {
        final NamespaceMappingRequest request = builder()
                .withQName(resolver.getQName("jcr:content"))
                .withQName(resolver.getQName("foo:folder"))
                .build();
        assertTrue("foo mapping is retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals)));
        assertFalse("non-foo mappings are not retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals).negate()));
    }

    @Test
    public void testBuildWithJCRName_prefix() {
        final NamespaceMappingRequest request = builder()
                .withJCRName("jcr:content")
                .withJCRName("foo:folder")
                .build();
        assertTrue("foo mapping is retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals)));
        assertFalse("non-foo mappings are not retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals).negate()));
    }

    @Test
    public void testBuildWithJCRName_uri() {
        final NamespaceMappingRequest request = builder().withJCRName(String.format("{%s}folder", NS_FOO_URI)).build();
        assertTrue("foo mapping is retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals)));
        assertFalse("non-foo mappings are not retained",
                request.resolveToJcrNs(mapping).stream().flatMap(Result::stream)
                        .anyMatch(inferTest1(NS_FOO::equals).negate()));
    }
}