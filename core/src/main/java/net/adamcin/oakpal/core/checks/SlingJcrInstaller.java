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

package net.adamcin.oakpal.core.checks;

import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import net.adamcin.oakpal.api.SlingSimulator;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class SlingJcrInstaller implements ProgressCheckFactory {
    static final String DEFAULT_INSTALL_PATH_PATTERN = "^(/[^/]*)*/(install|config)$";

    @ProviderType
    public interface JsonKeys {
        String rootPaths();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String rootPaths() {
            return "rootPaths";
        }
    };

    public static JsonKeys keys() {
        return KEYS;
    }

    @Override
    public ProgressCheck newInstance(final JsonObject config) throws Exception {
        return new Check(Arrays.asList("/apps", "/libs"));
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<SlingJcrInstaller> {
        private final List<String> rootPaths;

        private SlingSimulator slingSimulator;
        private Pattern installPattern = Pattern.compile(DEFAULT_INSTALL_PATH_PATTERN);

        private List<String> parentPaths = new ArrayList<>();

        Check(final @NotNull List<String> rootPaths) {
            super(SlingJcrInstaller.class);
            this.rootPaths = rootPaths;
        }

        @Override
        public void startedScan() {
            super.startedScan();
            this.parentPaths.clear();
        }

        @Override
        public void simulateSling(final SlingSimulator slingSimulator, final Set<String> runModes) {
            this.slingSimulator = slingSimulator;
        }

        Pattern compileInstallPattern(final @NotNull Set<String> runModes) {
            return Pattern.compile(String.format("^(/[^/]*)*/(install|config)(.(%s))*$",
                    String.join("|", runModes)));
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node,
                                 final PathAction action) throws RepositoryException {
            if (this.slingSimulator == null) {
                return;
            }



        }
    }

}
