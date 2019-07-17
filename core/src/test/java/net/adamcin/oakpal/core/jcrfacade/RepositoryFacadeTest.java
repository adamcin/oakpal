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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.Value;

import org.apache.jackrabbit.commons.SimpleValueFactory;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class RepositoryFacadeTest {

    RepositoryFacade getFacade(final @Nullable Repository mockRepository) {
        return new RepositoryFacade(mockRepository);
    }

    @Test
    public void testDescriptorKeys() {
        final Repository mockRepository = mock(Repository.class);
        final String[] keys = new String[]{"SOME_KEY"};
        when(mockRepository.getDescriptorKeys()).thenReturn(keys);
        final RepositoryFacade facade = getFacade(mockRepository);
        assertSame("should be same descriptorKeys",
                keys, facade.getDescriptorKeys());
        final RepositoryFacade facadeNull = getFacade(null);
        assertEquals("should be empty", 0,
                facadeNull.getDescriptorKeys().length);
    }

    @Test
    public void testIsStandardDescriptor() {
        final Repository mockRepository = mock(Repository.class);
        when(mockRepository.isStandardDescriptor(anyString())).thenReturn(false);
        when(mockRepository.isStandardDescriptor("SOME_KEY")).thenReturn(true);
        final RepositoryFacade facade = getFacade(mockRepository);
        assertTrue("SOME_KEY should be true",
                facade.isStandardDescriptor("SOME_KEY"));
        assertFalse("anyString key should be false",
                facade.isStandardDescriptor("ANOTHER_KEY"));
        final RepositoryFacade facadeNull = getFacade(null);
        assertFalse("null should return false",
                facadeNull.isStandardDescriptor("SOME_KEY"));
    }

    @Test
    public void testIsSingleValueDescriptor() {
        final Repository mockRepository = mock(Repository.class);
        when(mockRepository.isSingleValueDescriptor(anyString())).thenReturn(false);
        when(mockRepository.isSingleValueDescriptor("SOME_KEY")).thenReturn(true);
        final RepositoryFacade facade = getFacade(mockRepository);
        assertTrue("SOME_KEY should be true",
                facade.isSingleValueDescriptor("SOME_KEY"));
        assertFalse("anyString key should be false",
                facade.isSingleValueDescriptor("ANOTHER_KEY"));
        final RepositoryFacade facadeNull = getFacade(null);
        assertFalse("null should return false",
                facadeNull.isSingleValueDescriptor("SOME_KEY"));
    }

    @Test
    public void testGetDescriptor() {
        final Repository mockRepository = mock(Repository.class);
        final String key = "SOME_KEY";
        final String value = "SOME_VALUE";
        when(mockRepository.getDescriptor(anyString())).thenReturn(null);
        when(mockRepository.getDescriptor(key)).thenReturn(value);
        final RepositoryFacade facade = getFacade(mockRepository);
        assertSame("should be same value",
                value, facade.getDescriptor(key));
        assertNull("should be null",
                facade.getDescriptor(""));
        final RepositoryFacade facadeNull = getFacade(null);
        assertNull("should be null for null",
                facadeNull.getDescriptor(key));
    }

    @Test
    public void testGetDescriptorValue() {
        final Repository mockRepository = mock(Repository.class);
        final String key = "SOME_KEY";
        final Value value = new SimpleValueFactory().createValue("SOME_VALUE");
        when(mockRepository.getDescriptorValue(anyString())).thenReturn(null);
        when(mockRepository.getDescriptorValue(key)).thenReturn(value);
        final RepositoryFacade facade = getFacade(mockRepository);
        assertSame("should be same value",
                value, facade.getDescriptorValue(key));
        assertNull("should be null",
                facade.getDescriptorValues(""));
        final RepositoryFacade facadeNull = getFacade(null);
        assertNull("should be null for null",
                facadeNull.getDescriptorValue(key));
    }

    @Test
    public void testGetDescriptorValues() {
        final Repository mockRepository = mock(Repository.class);
        final String key = "SOME_KEY";
        final Value[] values = new Value[]{new SimpleValueFactory().createValue("SOME_VALUE")};
        when(mockRepository.getDescriptorValues(anyString())).thenReturn(null);
        when(mockRepository.getDescriptorValues(key)).thenReturn(values);
        final RepositoryFacade facade = getFacade(mockRepository);
        assertSame("should be same values",
                values, facade.getDescriptorValues(key));
        assertNull("should be null",
                facade.getDescriptorValues(""));
        final RepositoryFacade facadeNull = getFacade(null);
        assertNull("should be null for null",
                facadeNull.getDescriptorValues(key));
    }

    @Test(expected = LoginException.class)
    public void testNoLogin() throws Exception {
        RepositoryFacade facade = getFacade(mock(Repository.class));
        facade.login();
    }

    @Test(expected = LoginException.class)
    public void testNoLoginWorkspace() throws Exception {
        RepositoryFacade facade = getFacade(mock(Repository.class));
        facade.login("");
    }

    @Test(expected = LoginException.class)
    public void testNoLoginCredentials() throws Exception {
        RepositoryFacade facade = getFacade(mock(Repository.class));
        facade.login((Credentials) null);
    }

    @Test(expected = LoginException.class)
    public void testNoLoginCredentialsWorkspace() throws Exception {
        RepositoryFacade facade = getFacade(mock(Repository.class));
        facade.login((Credentials) null, "");
    }
}