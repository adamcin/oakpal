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

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

public class ProgressCheckTest {

    @Test
    public void testDefaultMethods() throws Exception {
        ProgressCheck mock = new ProgressCheck() {
            @Override
            public Collection<Violation> getReportedViolations() {
                return Collections.emptyList();
            }
        };

        Assert.assertNotNull("expect nonnull checkName", mock.getCheckName());
        mock.startedScan();
        mock.identifyPackage(null, null);
        mock.identifySubpackage(null, null);
        mock.readManifest(null, null);
        mock.beforeExtract(null, null, null, null, null);
        mock.importedPath(null, null, null);
        mock.deletedPath(null, null, null);
        mock.afterExtract(null, null);
        mock.finishedScan();
    }
}
