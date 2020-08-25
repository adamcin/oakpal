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

package net.adamcin.oakpal.core.sling;

import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.core.OakpalPlan;
import org.apache.sling.installer.core.impl.InternalResource;
import org.junit.Test;

import java.util.Optional;
import java.util.function.Function;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.core.OakpalPlan.keys;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DefaultSlingSimulatorTest {

    @Test
    public void testReadInstallableResourceFromNode_noRepositoryException() throws Exception {
        OakpalPlan.fromJson(key(keys().repoInits(),
                arr().val("create path (nt:unstructured) /apps/config/Test")).get())
                .toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {

            Result<Optional<InternalResource>> result = DefaultSlingSimulator
                    .readInternalResourceFromNode(session.getNode("/apps/config/Test"));
            assertTrue("expect null config", result.isSuccess() && !result.getOrDefault(null).isPresent());
        });
    }

    @Test
    public void testReadInstallableResourceFromNode_slingOsgiConfig() throws Exception {
        OakpalPlan.fromJson(key(keys().repoInits(), arr()
                .val("register nodetypes")
                .val("<<===")
                .val("<'sling'='http://sling.apache.org/jcr/sling/1.0'>")
                .val("[sling:OsgiConfig] > nt:unstructured, nt:hierarchyNode")
                .val("===>>")
                .val("create path (nt:folder) /apps/config/Test(sling:OsgiConfig)"))
                .get()).toOakMachineBuilder(null, getClass().getClassLoader())
                .build().initAndInspect(session -> {

            InternalResource resource = DefaultSlingSimulator
                    .readInternalResourceFromNode(session.getNode("/apps/config/Test")).toOptional()
                    .flatMap(Function.identity())
                    .orElse(null);
            assertNotNull("expect not null resource", resource);
            assertNotNull("expect not null config", resource.getDictionary());
        });
    }


}