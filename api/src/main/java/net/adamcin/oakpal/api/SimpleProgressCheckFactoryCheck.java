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

import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Convenience type for simple progress checks that are identified by their factory.
 *
 * @param <FACTORY> the factory type parameter
 */
@ConsumerType
public class SimpleProgressCheckFactoryCheck<FACTORY extends ProgressCheckFactory> extends SimpleProgressCheck {

    private final Class<FACTORY> factoryClass;

    /**
     * Need to keep a handle on the concrete class.
     *
     * @param factoryClass the factory class
     */
    public SimpleProgressCheckFactoryCheck(final Class<FACTORY> factoryClass) {
        this.factoryClass = factoryClass;
    }

    @Override
    public String getCheckName() {
        return factoryClass.getSimpleName();
    }

    @Override
    public @Nullable String getResourceBundleBaseName() {
        return factoryClass.getName();
    }
}
