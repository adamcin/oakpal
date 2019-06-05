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

import static net.adamcin.oakpal.core.JsonCnd.NodeTypeDefinitionKey.PRIMARYITEM;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.json.JsonValue;

import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class JsonCndTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonCndTest.class);

    private NamePathResolver resolver = new DefaultNamePathResolver(QName.BUILTIN_MAPPINGS);

    @Test
    public void test_PRIMARYITEM_writeJson() throws Exception {
        QNodeTypeDefinition def = mock(QNodeTypeDefinition.class);
        when(def.getPrimaryItemName()).thenReturn(resolver.getQName("jcr:content"));
        JsonValue value = PRIMARYITEM.writeJson(def, resolver);
        LOGGER.info("[test_PRIMARYITEM_writeJson] value = {}", value);
        assertSame("value is a JsonString", JsonValue.ValueType.STRING, value.getValueType());
    }
}
