/*
 * Copyright 2018 Mark Adamcin
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

package net.adamcin.oakpal.maven.component;

import static net.adamcin.oakpal.maven.component.OakpalComponentConfigurator.HINT;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;

/**
 * Adds a {@link JSONObjectConverter} to the {@link BasicComponentConfigurator} to support mapping arbitrarily nested
 * {@link org.codehaus.plexus.configuration.PlexusConfiguration} elements to a single {@link org.json.JSONObject} graph.
 */
@Component(role = ComponentConfigurator.class, hint = HINT)
public class OakpalComponentConfigurator extends BasicComponentConfigurator {
    public static final String HINT = "oakpal";

    public OakpalComponentConfigurator() {
        converterLookup.registerConverter(new JSONObjectConverter());
    }
}
