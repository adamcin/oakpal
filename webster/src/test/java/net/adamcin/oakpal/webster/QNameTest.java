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

package net.adamcin.oakpal.webster;

import net.adamcin.oakpal.core.JsonCnd;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.junit.Test;

import javax.jcr.NamespaceRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class QNameTest {

    @Test(expected = QName.UnqualifiedNameException.class)
    public void testParseQName_unqualified() {
        QName.parseQName(JsonCnd.BUILTIN_MAPPINGS, QName.Type.NODETYPE, "aType");
    }

    @Test
    public void testParseQName() {
        final QName ntPrefixName = QName.parseQName(JsonCnd.BUILTIN_MAPPINGS, QName.Type.NODETYPE, "nt:aType");
        assertEquals("type is ", QName.Type.NODETYPE, ntPrefixName.getType());
        assertEquals("prefix is ", "nt", ntPrefixName.getPrefix());
        assertEquals("localName is ", "aType", ntPrefixName.getLocalName());
        assertEquals("uri is ", NamespaceRegistry.NAMESPACE_NT, ntPrefixName.getUri());
        assertEquals("toString is ",
                "nt:aType", ntPrefixName.toString());
        assertEquals("expanded is ",
                "{" + NamespaceRegistry.NAMESPACE_NT + "}aType", ntPrefixName.toExpandedForm());

        final QName ntUriName = QName.parseQName(JsonCnd.BUILTIN_MAPPINGS, QName.Type.NODETYPE,
                "{http://foo.com}anotherType");
        assertEquals("type is ", QName.Type.NODETYPE, ntUriName.getType());
        assertNull("prefix is null", ntUriName.getPrefix());
        assertEquals("localName is ", "anotherType", ntUriName.getLocalName());
        assertEquals("uri is ", "http://foo.com", ntUriName.getUri());
        assertEquals("toString is ",
                "{http://foo.com}anotherType", ntUriName.toString());
        assertEquals("expanded is ",
                "{http://foo.com}anotherType", ntUriName.toExpandedForm());
    }

    @Test
    public void testEqualsHashCode() {
        final QName ntPrefixName = QName.parseQName(JsonCnd.BUILTIN_MAPPINGS, QName.Type.NODETYPE, "nt:aType");
        final QName ntUriName = QName.parseQName(JsonCnd.BUILTIN_MAPPINGS, QName.Type.NODETYPE,
                "{" + NamespaceRegistry.NAMESPACE_NT + "}" + "aType");
        assertEquals("by prefix is same as by uri", ntUriName, ntPrefixName);
        assertEquals("hashCode by prefix is same as by uri", ntUriName.hashCode(), ntPrefixName.hashCode());
    }

    @Test
    public void testAdaptName() throws Exception {
        final NamePathResolver resolver = new DefaultNamePathResolver(JsonCnd.BUILTIN_MAPPINGS);
        final QName ntPrefixName = QName.adaptName(JsonCnd.BUILTIN_MAPPINGS,
                QName.Type.NODETYPE, resolver.getQName("nt:aType"));
        final QName ntUriName = QName.adaptName(JsonCnd.BUILTIN_MAPPINGS, QName.Type.NODETYPE,
                resolver.getQName("{" + NamespaceRegistry.NAMESPACE_NT + "}" + "aType"));
        assertEquals("adapt equals", ntPrefixName, ntUriName);

    }
}