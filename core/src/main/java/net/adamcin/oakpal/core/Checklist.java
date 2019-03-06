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

package net.adamcin.oakpal.core;

import static java.util.Optional.ofNullable;
import static net.adamcin.oakpal.core.JavaxJson.hasNonNull;
import static net.adamcin.oakpal.core.JavaxJson.optArray;
import static net.adamcin.oakpal.core.JavaxJson.parseFromArray;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It's a list of checks, along with initStage properties allowing jars to share CNDs, JCR namespaces and privileges,
 * and forced roots.
 */
public final class Checklist {
    private static final Logger LOGGER = LoggerFactory.getLogger(Checklist.class);

    static final String KEY_NAME = "name";
    static final String KEY_CND_URLS = "cndUrls";
    static final String KEY_CND_NAMES = "cndNames";
    static final String KEY_JCR_NAMESPACES = "jcrNamespaces";
    static final String KEY_JCR_PRIVILEGES = "jcrPrivileges";
    static final String KEY_FORCED_ROOTS = "forcedRoots";
    static final String KEY_CHECKS = "checks";

    private final String moduleName;
    private final String name;
    private final List<URL> cndUrls;
    private final List<JcrNs> jcrNamespaces;
    private final List<String> jcrPrivileges;
    private final List<ForcedRoot> forcedRoots;
    private final List<CheckSpec> checks;

    Checklist(final String moduleName,
              final String name,
              final List<URL> cndUrls,
              final List<JcrNs> jcrNamespaces,
              final List<String> jcrPrivileges,
              final List<ForcedRoot> forcedRoots,
              final List<CheckSpec> checks) {
        this.moduleName = moduleName;
        this.name = name;
        this.cndUrls = cndUrls;
        this.jcrNamespaces = jcrNamespaces;
        this.jcrPrivileges = jcrPrivileges;
        this.forcedRoots = forcedRoots;
        this.checks = checks;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getName() {
        return name;
    }

    public List<URL> getCndUrls() {
        return cndUrls;
    }

    public List<JcrNs> getJcrNamespaces() {
        return jcrNamespaces;
    }

    public List<String> getJcrPrivileges() {
        return jcrPrivileges;
    }

    public List<ForcedRoot> getForcedRoots() {
        return forcedRoots;
    }

    public List<CheckSpec> getChecks() {
        return checks;
    }

    public InitStage asInitStage() {
        InitStage.Builder builder = new InitStage.Builder()
                .withOrderedCndUrls(getCndUrls())
                .withForcedRoots(getForcedRoots())
                .withPrivileges(getJcrPrivileges())
                .withNs(getJcrNamespaces());

        return builder.build();
    }

    static class Builder {
        private final String moduleName;
        private String name;
        private List<URL> cndUrls = new ArrayList<>();
        private List<JcrNs> jcrNamespaces = new ArrayList<>();
        private List<String> jcrPrivileges = new ArrayList<>();
        private List<ForcedRoot> forcedRoots = new ArrayList<>();
        private List<CheckSpec> checks = new ArrayList<>();

        Builder(final String moduleName) {
            this.moduleName = moduleName;
        }

        Builder withName(final String name) {
            this.name = name;
            return this;
        }

        Builder withCndUrls(final List<URL> cndUrls) {
            this.cndUrls.addAll(cndUrls);
            return this;
        }

        Builder withJcrNamespaces(final List<JcrNs> jcrNamespaces) {
            this.jcrNamespaces.addAll(jcrNamespaces);
            return this;
        }

        Builder withJcrPrivileges(final List<String> jcrPrivileges) {
            this.jcrPrivileges.addAll(jcrPrivileges);
            return this;
        }

        Builder withForcedRoots(final List<ForcedRoot> forcedRoots) {
            this.forcedRoots.addAll(forcedRoots);
            return this;
        }

        Builder withChecks(final List<CheckSpec> checks) {
            this.checks.addAll(checks.stream()
                    .filter(this::isValidCheckspec)
                    .collect(Collectors.toList()));
            return this;
        }

        boolean isValidCheckspec(final CheckSpec check) {
            return !check.isAbstract()
                    && check.getName() != null
                    && !check.getName().isEmpty()
                    && !check.getName().contains("/");
        }

        private String getCheckPrefix() {
            final String modulePrefix = ofNullable(this.moduleName)
                    .filter(name -> !name.isEmpty())
                    .map(name -> name + "/")
                    .orElse("");
            return ofNullable(this.name)
                    .filter(name -> !name.isEmpty() && !name.equals(moduleName))
                    .map(name -> modulePrefix + name + "/")
                    .orElse(modulePrefix);
        }

        private void insertPrefix(final CheckSpec checkSpec, final String prefix) {
            checkSpec.setName(prefix + checkSpec.getName());
        }

        Checklist build() {
            final String prefix = getCheckPrefix();
            checks.forEach(checkSpec -> insertPrefix(checkSpec, prefix));
            return new Checklist(moduleName,
                    name != null ? name : moduleName,
                    Collections.unmodifiableList(cndUrls),
                    Collections.unmodifiableList(jcrNamespaces),
                    Collections.unmodifiableList(jcrPrivileges),
                    Collections.unmodifiableList(forcedRoots),
                    Collections.unmodifiableList(checks));
        }
    }

    @Override
    public String toString() {
        return "Checklist{" +
                "moduleName='" + moduleName + '\'' +
                ", name='" + name + '\'' +
                ", cndUrls=" + cndUrls +
                ", jcrNamespaces=" + jcrNamespaces +
                ", jcrPrivileges=" + jcrPrivileges +
                ", forcedRoots=" + forcedRoots +
                ", checks=" + checks +
                '}';
    }

    /**
     * Create a Checklist from a JsonObject.
     *
     * @param moduleName  module name is required at this point.
     * @param manifestUrl manifest resource url
     * @param json        check list blob
     * @return the new package checklist
     */
    public static Checklist fromJson(final String moduleName, final URL manifestUrl, final JsonObject json) {
        LOGGER.trace("[fromJson] module: {}, manifestUrl: {}, json: {}", moduleName, manifestUrl, json);
        Builder builder = new Builder(moduleName);
        if (hasNonNull(json, KEY_NAME)) {
            builder.withName(json.getString(KEY_NAME));
        }

        if (manifestUrl != null && manifestUrl.toExternalForm().endsWith(JarFile.MANIFEST_NAME)) {
            ofNullable(json.getJsonArray(KEY_CND_NAMES))
                    .filter(Util.traceFilter(LOGGER, "[fromJson#cndNames] cndNames: {}"))
                    .map(cndNames -> JavaxJson.unwrapArray(cndNames).stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList()))
                    .map(names -> Util.resolveManifestResources(manifestUrl, names))
                    .ifPresent(builder::withCndUrls);
        }

        builder.withCndUrls(parseFromArray(
                optArray(json, KEY_CND_URLS).orElse(JsonValue.EMPTY_JSON_ARRAY), URL::new,
                (element, error) -> LOGGER.debug("[fromJson#cndUrls] ignoring error", error)));

        optArray(json, KEY_JCR_NAMESPACES).ifPresent(jsonArray -> {
            builder.withJcrNamespaces(JavaxJson.mapArrayOfObjects(jsonArray, JcrNs::fromJson));
        });
        optArray(json, KEY_JCR_PRIVILEGES).ifPresent(jsonArray -> {
            builder.withJcrPrivileges(JavaxJson.mapArrayOfStrings(jsonArray));
        });
        optArray(json, KEY_FORCED_ROOTS).ifPresent(jsonArray -> {
            builder.withForcedRoots(JavaxJson.mapArrayOfObjects(jsonArray, ForcedRoot::fromJson));
        });
        optArray(json, KEY_CHECKS).ifPresent(jsonArray -> {
            builder.withChecks(JavaxJson.mapArrayOfObjects(jsonArray, CheckSpec::fromJson));
        });
        final Checklist checklist = builder.build();
        LOGGER.trace("[fromJson] builder.build(): {}", checklist);
        return checklist;
    }
}
