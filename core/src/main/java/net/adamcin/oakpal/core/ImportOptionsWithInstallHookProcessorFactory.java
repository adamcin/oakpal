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

import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.InstallHookProcessor;
import org.apache.jackrabbit.vault.packaging.InstallHookProcessorFactory;

class ImportOptionsWithInstallHookProcessorFactory extends ImportOptions implements InstallHookProcessorFactory {

    private final ImportOptions optionsDelegate;
    private final InstallHookProcessorFactory factoryDelegate;

    ImportOptionsWithInstallHookProcessorFactory(ImportOptions optionsDelegate, InstallHookProcessorFactory factoryDelegate) {
        this.optionsDelegate = optionsDelegate;
        this.factoryDelegate = factoryDelegate;
    }

    @Override
    public InstallHookProcessor createInstallHookProcessor() {
        return factoryDelegate.createInstallHookProcessor();
    }

    @Override
    public ImportOptions copy() {
        return new ImportOptionsWithInstallHookProcessorFactory(optionsDelegate.copy(), factoryDelegate);
    }
}
