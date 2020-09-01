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
import net.adamcin.oakpal.api.SilenceableCheck;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import net.adamcin.oakpal.api.SlingSimulator;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The {@code SlingJcrInstaller} is a check implementation that watches for Sling installable nodes and registers them
 * with the {@link SlingSimulator} when sling simulation is active.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code rootPaths} (Default: {@code [/apps, /libs]})</dt>
 * <dd>The root paths that the listener watches for install and config nodes.</dd>
 * <dt>{@code maxDepth} (Default: {@code 4})</dt>
 * <dd>Represents the maximum depth of a watched folder path relative to the repository root. Depth of a path is
 * calculated as {@code path.split("/").length}. When a path's depth is less than or equal to {@code maxDepth},
 * its children are candidates to be watched folders, but not its grandchildren.</dd>
 * </dl>
 * @since 2.2.0
 */
public final class SlingJcrInstaller implements ProgressCheckFactory {
    static final List<String> DEFAULT_ROOT_PATHS = Arrays.asList("/apps", "/libs");
    static final int DEFAULT_MAX_DEPTH = 4;

    @ProviderType
    public interface JsonKeys {
        String rootPaths();

        /**
         * maxDepth represents the depth of a watched folder path relative to the repository root. Depth of a path is
         * calculated as {@code path.split("/").length}. When a path's depth is less than or equal to {@code maxDepth},
         * its children are candidates to be watched folders, but not its grandchildren.
         *
         * @return the json key for {@code maxDepth}
         */
        String maxDepth();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String rootPaths() {
            return "rootPaths";
        }

        public String maxDepth() {
            return "maxDepth";
        }
    };

    public static JsonKeys keys() {
        return KEYS;
    }

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        final List<String> rootPaths = JavaxJson.optArray(config, keys().rootPaths())
                .map(JavaxJson::mapArrayOfStrings)
                .orElse(DEFAULT_ROOT_PATHS);

        final int maxDepth = config.getInt(keys().maxDepth(), DEFAULT_MAX_DEPTH);

        return new Check(rootPaths, maxDepth);
    }

    /**
     * This check implements {@link SilenceableCheck} specifically to avoid be wrapped with a silencing facade.
     */
    static final class Check extends SimpleProgressCheckFactoryCheck<SlingJcrInstaller> implements SilenceableCheck {
        private final List<String> rootPaths;
        private final int maxDepth;

        private SlingSimulator slingSimulator;
        private Pattern installPattern;

        Check(final @NotNull List<String> rootPaths, final int maxDepth) {
            super(SlingJcrInstaller.class);
            this.rootPaths = rootPaths;
            this.maxDepth = maxDepth;
            this.installPattern = compileInstallPattern(Collections.emptySet(), maxDepth);
        }

        @Override
        public void startedScan() {
            super.startedScan();
        }

        @Override
        public void setSilenced(final boolean silenced) {
            /* do nothing, because we currently collect no violations in this check */
        }

        @Override
        public void simulateSling(final SlingSimulator slingSimulator, final Set<String> runModes) {
            this.slingSimulator = slingSimulator;
            installPattern = compileInstallPattern(runModes, maxDepth);
        }

        Pattern compileInstallPattern(final @NotNull Set<String> runModes, final int maxDepth) {
            String patternSuffix = runModes.isEmpty()
                    ? "/"
                    : String.format("(\\.(%s))*/", String.join("|", runModes));
            return Pattern.compile("^" + depthPrefix(maxDepth) + "/(install|config)" + patternSuffix);
        }

        String depthPrefix(final int maxDepth) {
            if (maxDepth < 0) {
                return "(/[^/]*)*";
            } else if (maxDepth == 0) {
                return "";
            } else {
                return "(" + depthPrefix(maxDepth - 1) + "/[^/]+)?";
            }
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node,
                                 final PathAction action) throws RepositoryException {

            if (isPathIgnored(path)) {
                return;
            }

            if (installPattern.matcher(path).find()) {
                this.slingSimulator.addInstallableNode(packageId, node);
            }
        }

        boolean isPathIgnored(final String path) {
            return this.slingSimulator == null
                    || path.startsWith("/etc/packages/")
                    || rootPaths.stream().noneMatch(path::startsWith)
                    || Stream.of("/install", "/config").noneMatch(path::contains);
        }
    }
}
