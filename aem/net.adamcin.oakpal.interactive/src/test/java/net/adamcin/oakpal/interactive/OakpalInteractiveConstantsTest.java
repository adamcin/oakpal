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

package net.adamcin.oakpal.interactive;

import static org.junit.Assert.assertEquals;

import net.adamcin.oakpal.core.CheckSpec;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;

public class OakpalInteractiveConstantsTest {

    @Test
    public void testConstants() {
        assertEquals("ADAPTABLE_RESOURCE should be the Resource class name",
                OakpalInteractiveConstants.ADAPTABLE_RESOURCE, Resource.class.getName());

        assertEquals("ADAPTER_CHECK_SPEC",
                OakpalInteractiveConstants.ADAPTER_CHECK_SPEC, CheckSpec.class.getName());
    }
}
