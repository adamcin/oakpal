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

package net.adamcin.oakpal.core.jcrfacade;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.NamespaceRegistry;

import net.adamcin.oakpal.core.ListenerReadOnlyException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class NamespaceRegistryFacadeTest {

    NamespaceRegistryFacade getFacade(final @NotNull NamespaceRegistry mockRegistry) {
        return new NamespaceRegistryFacade(mockRegistry);
    }

    @Test
    public void testGetPrefixes() throws Exception {
        NamespaceRegistry delegate = mock(NamespaceRegistry.class);
        NamespaceRegistryFacade facade = getFacade(delegate);
        final String[] prefixes = new String[]{"jcr", "nt", "oak"};
        when(delegate.getPrefixes()).thenReturn(prefixes);
        assertSame("should be same", prefixes, facade.getPrefixes());
    }

    @Test
    public void testGetURIs() throws Exception {
        NamespaceRegistry delegate = mock(NamespaceRegistry.class);
        NamespaceRegistryFacade facade = getFacade(delegate);
        final String[] uris = new String[]{"jcr", "nt", "oak"};
        when(delegate.getURIs()).thenReturn(uris);
        assertSame("should be same", uris, facade.getURIs());
    }

    @Test
    public void testGetPrefix() throws Exception {
        NamespaceRegistry delegate = mock(NamespaceRegistry.class);
        NamespaceRegistryFacade facade = getFacade(delegate);
        final String prefix = NamespaceRegistry.PREFIX_JCR;
        final String uri = NamespaceRegistry.NAMESPACE_JCR;
        when(delegate.getPrefix(anyString())).thenReturn(null);
        when(delegate.getPrefix(uri)).thenReturn(prefix);
        assertNull("should be null", facade.getPrefix(""));
        assertSame("should be same", prefix, facade.getPrefix(uri));
    }

    @Test
    public void testGetURI() throws Exception {
        NamespaceRegistry delegate = mock(NamespaceRegistry.class);
        NamespaceRegistryFacade facade = getFacade(delegate);
        final String prefix = NamespaceRegistry.PREFIX_JCR;
        final String uri = NamespaceRegistry.NAMESPACE_JCR;
        when(delegate.getURI(anyString())).thenReturn(null);
        when(delegate.getURI(prefix)).thenReturn(uri);
        assertNull("should be null", facade.getURI(""));
        assertSame("should be same", uri, facade.getURI(prefix));
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testRORegisterNamespace() throws Exception {
        NamespaceRegistryFacade facade = getFacade(mock(NamespaceRegistry.class));
        facade.registerNamespace("", "");
    }

    @Test(expected = ListenerReadOnlyException.class)
    public void testROUnregisterNamespace() throws Exception {
        NamespaceRegistryFacade facade = getFacade(mock(NamespaceRegistry.class));
        facade.unregisterNamespace("");
    }
}