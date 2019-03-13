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

package net.adamcin.oakpal.interactive.models;

import static org.junit.Assert.assertNotNull;

import net.adamcin.oakpal.interactive.OakpalInteractiveConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class OakpalScanInputResourceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OakpalScanInputResourceTest.class);

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testAdaptResource() throws Exception {
        Resource scanResource = context.create().resource("/content/oakpal/scan",
                "sling:resourceType", OakpalInteractiveConstants.RT_OAKPAL_SCAN_INPUT,
                "checklists", new String[]{"basic"});

        final OakpalScanInputResource adapter = context.getService(ModelFactory.class)
                .createModel(scanResource, OakpalScanInputResource.class);

        //OakpalScanResource adapter = scanResource.adaptTo(OakpalScanResource.class);
        assertNotNull("adapter is not null", adapter);
    }
}
