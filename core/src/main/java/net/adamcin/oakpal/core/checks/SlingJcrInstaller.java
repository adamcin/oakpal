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

import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import net.adamcin.oakpal.api.SlingSimulator;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The {@code SlingJcrInstaller} is a check implementation that watches for Sling installable nodes and registers them
 * with the {@link SlingSimulator} when sling simulation is active.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code rootPaths} (Default: {@code [/apps, /libs]})</dt>
 * <dd>The root paths that the listener watches for install and config nodes.</dd>
 * </dl>
 */
public final class SlingJcrInstaller implements ProgressCheckFactory {
    static final String DEFAULT_INSTALL_PATH_PATTERN = "^(/[^/]*)*/(install|config)$";
    static final List<String> DEFAULT_ROOT_PATHS = Arrays.asList("/apps", "/libs");

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
        final List<String> rootPaths = JavaxJson.optArray(config, keys().rootPaths()).map(JavaxJson::mapArrayOfStrings)
                .orElse(DEFAULT_ROOT_PATHS);
        return new Check(rootPaths);
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<SlingJcrInstaller> {
        private final List<String> rootPaths;

        private SlingSimulator slingSimulator;
        private Pattern installPattern = Pattern.compile(DEFAULT_INSTALL_PATH_PATTERN);

        Check(final @NotNull List<String> rootPaths) {
            super(SlingJcrInstaller.class);
            this.rootPaths = rootPaths;
        }

        @Override
        public void startedScan() {
            super.startedScan();
        }

        @Override
        public void simulateSling(final SlingSimulator slingSimulator, final Set<String> runModes) {
            this.slingSimulator = slingSimulator;
            installPattern = compileInstallPattern(runModes);
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

            if (path.startsWith("/etc/packages/")) {
                return;
            }

            if (rootPaths.stream().noneMatch(path::startsWith)) {
                return;
            }

            if (installPattern.matcher(node.getParent().getPath()).matches()) {
                this.slingSimulator.prepareInstallableNode(packageId, node);
            }
        }
    }

}
