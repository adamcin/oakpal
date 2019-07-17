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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class PropertyIteratorFacadeTest {

    PropertyIteratorFacade<Session> getFacade(final @NotNull PropertyIterator delegate) {
        return new PropertyIteratorFacade<>(delegate, new JcrSessionFacade(mock(Session.class), true));
    }

    @Test
    public void testFacadeGetters() throws Exception {
        new FacadeGetterMapping.Tester<>(PropertyIterator.class, this::getFacade)
                .testFacadeGetter(Property.class, PropertyFacade.class, PropertyIterator::nextProperty);

        PropertyIterator delegate = mock(PropertyIterator.class);
        Property mockProperty = mock(Property.class);
        when(mockProperty.getPath()).thenReturn("/mockProperty");
        when(delegate.nextProperty()).thenReturn(mockProperty);
        PropertyIteratorFacade<Session> facade = getFacade(delegate);
        Property nextProperty = (Property) facade.next();
        assertTrue("should be instance of PropertyFacade", nextProperty instanceof PropertyFacade);
        assertEquals("should have correct path", "/mockProperty", nextProperty.getPath());
    }
}