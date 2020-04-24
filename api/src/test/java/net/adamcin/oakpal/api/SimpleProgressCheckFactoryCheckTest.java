/*
 * Copyright 2020 Mark Adamcin
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

package net.adamcin.oakpal.api;

import org.junit.Test;

import javax.json.JsonObject;
import javax.json.JsonValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleProgressCheckFactoryCheckTest {

    static class TestProgressCheckFactory implements ProgressCheckFactory {
        @Override
        public ProgressCheck newInstance(final JsonObject config) throws Exception {
            return new TestProgressCheckFactoryCheck();
        }

    }

    static class TestProgressCheckFactoryCheck extends SimpleProgressCheckFactoryCheck<TestProgressCheckFactory> {
        public TestProgressCheckFactoryCheck() {
            super(TestProgressCheckFactory.class);
        }
    }

    @Test
    public void testBaseMethods() throws Exception {
        ProgressCheck progressCheck = new TestProgressCheckFactory().newInstance(JsonValue.EMPTY_JSON_OBJECT);
        assertTrue("expect instance of TestProgressCheckFactoryCheck",
                progressCheck instanceof TestProgressCheckFactoryCheck);

        assertEquals("expect checkName", TestProgressCheckFactory.class.getSimpleName(),
                progressCheck.getCheckName());
        assertEquals("expect resourceBundleName", TestProgressCheckFactory.class.getName(),
                progressCheck.getResourceBundleBaseName());

    }
}