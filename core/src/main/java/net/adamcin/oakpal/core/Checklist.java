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
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static net.adamcin.oakpal.core.JavaxJson.optArray;
import static net.adamcin.oakpal.core.JavaxJson.optObject;
import static net.adamcin.oakpal.core.JavaxJson.parseFromArray;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It's a list of checks, along with initStage properties allowing jars to share CNDs, JCR namespaces and privileges,
 * and forced roots.
 */
public final class Checklist implements JavaxJson.ObjectConvertible {

    private static final Logger LOGGER = LoggerFactory.getLogger(Checklist.class);

    static final String KEY_NAME = "name";
    static final String KEY_CND_URLS = "cndUrls";
    static final String KEY_CND_NAMES = "cndNames";
    public static final String KEY_JCR_NODETYPES = "jcrNodetypes";
    public static final String KEY_JCR_NAMESPACES = "jcrNamespaces";
    public static final String KEY_JCR_PRIVILEGES = "jcrPrivileges";
    public static final String KEY_FORCED_ROOTS = "forcedRoots";
    static final String KEY_CHECKS = "checks";

    private static final List<String> KEY_ORDER = Arrays.asList(
            Checklist.KEY_NAME,
            Checklist.KEY_CHECKS,
            Checklist.KEY_FORCED_ROOTS,
            Checklist.KEY_CND_NAMES,
            Checklist.KEY_CND_URLS,
            Checklist.KEY_JCR_NODETYPES,
            Checklist.KEY_JCR_PRIVILEGES,
            Checklist.KEY_JCR_NAMESPACES);

    private static final Comparator<String> checklistKeyComparator = (s1, s2) -> {
        if (KEY_ORDER.contains(s1) && KEY_ORDER.contains(s2)) {
            return Integer.compare(KEY_ORDER.indexOf(s1), KEY_ORDER.indexOf(s2));
        } else if (KEY_ORDER.contains(s1)) {
            return -1;
        } else if (KEY_ORDER.contains(s2)) {
            return 1;
        } else {
            return s1.compareTo(s2);
        }
    };

    public static <T> Comparator<T> comparingJsonKeys(@NotNull final Function<T, String> jsonKeyExtractor) {
        return (t1, t2) -> checklistKeyComparator.compare(jsonKeyExtractor.apply(t1), jsonKeyExtractor.apply(t2));
    }

    private final JsonObject originalJson;
    private final String moduleName;
    private final String name;
    private final List<URL> cndUrls;
    private final List<JcrNs> jcrNamespaces;
    private final List<QNodeTypeDefinition> jcrNodetypes;
    private final List<String> jcrPrivileges;
    private final List<ForcedRoot> forcedRoots;
    private final List<CheckSpec> checks;

    Checklist(@Nullable final JsonObject originalJson,
              @Nullable final String moduleName,
              @Nullable final String name,
              @NotNull final List<URL> cndUrls,
              @NotNull final List<JcrNs> jcrNamespaces,
              @NotNull final List<QNodeTypeDefinition> jcrNodetypes,
              @NotNull final List<String> jcrPrivileges,
              @NotNull final List<ForcedRoot> forcedRoots,
              @NotNull final List<CheckSpec> checks) {
        this.originalJson = originalJson;
        this.moduleName = moduleName;
        this.name = name;
        this.cndUrls = cndUrls;
        this.jcrNamespaces = jcrNamespaces;
        this.jcrNodetypes = jcrNodetypes;
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

    public List<QNodeTypeDefinition> getJcrNodetypes() {
        return jcrNodetypes;
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
                .withQNodeTypes(getJcrNodetypes())
                .withNs(getJcrNamespaces());

        return builder.build();
    }

    static class Builder {
        private final String moduleName;
        private String name;
        private List<QNodeTypeDefinition> jcrNodetypes = new ArrayList<>();
        private List<URL> cndUrls = new ArrayList<>();
        private List<JcrNs> jcrNamespaces = new ArrayList<>();
        private List<String> jcrPrivileges = new ArrayList<>();
        private List<ForcedRoot> forcedRoots = new ArrayList<>();
        private List<CheckSpec> checks = new ArrayList<>();

        Builder(@NotNull final String moduleName) {
            this.moduleName = moduleName;
        }

        Builder withName(@Nullable final String name) {
            this.name = name;
            return this;
        }

        Builder withCndUrls(@NotNull final List<URL> cndUrls) {
            this.cndUrls.addAll(cndUrls);
            return this;
        }

        Builder withJcrNamespaces(@NotNull final List<JcrNs> jcrNamespaces) {
            this.jcrNamespaces.addAll(jcrNamespaces);
            return this;
        }

        Builder withJcrNodetypes(@NotNull final List<QNodeTypeDefinition> jcrNodetypes) {
            this.jcrNodetypes.addAll(jcrNodetypes);
            return this;
        }

        Builder withJcrPrivileges(@NotNull final List<String> jcrPrivileges) {
            this.jcrPrivileges.addAll(jcrPrivileges);
            return this;
        }

        Builder withForcedRoots(@NotNull final List<ForcedRoot> forcedRoots) {
            this.forcedRoots.addAll(forcedRoots);
            return this;
        }

        Builder withChecks(@NotNull final List<CheckSpec> checks) {
            this.checks.addAll(checks.stream()
                    .filter(this::isValidCheckspec)
                    .collect(Collectors.toList()));
            return this;
        }

        boolean isValidCheckspec(@NotNull final CheckSpec check) {
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

        private void insertPrefix(@NotNull final CheckSpec checkSpec, final String prefix) {
            checkSpec.setName(prefix + checkSpec.getName());
        }

        public Checklist build() {
            return build(null);
        }

        Checklist build(@Nullable final JsonObject originalJson) {
            final String prefix = getCheckPrefix();
            checks.forEach(checkSpec -> insertPrefix(checkSpec, prefix));
            return new Checklist(originalJson, moduleName,
                    name != null ? name : moduleName,
                    Collections.unmodifiableList(cndUrls),
                    Collections.unmodifiableList(jcrNamespaces),
                    Collections.unmodifiableList(jcrNodetypes),
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
                ", jcrNodetypes=" + jcrNodetypes +
                ", jcrPrivileges=" + jcrPrivileges +
                ", forcedRoots=" + forcedRoots +
                ", checks=" + checks +
                '}';
    }

    /**
     * Serialize the Checklist to a JsonObject for writing.
     *
     * @return the checklist as a JSON-serializable object.
     */
    @Override
    public JsonObject toJson() {
        if (this.originalJson != null) {
            return this.originalJson;
        } else {
            JavaxJson.Obj json = obj().key(KEY_NAME, getName());
            if (!getJcrNamespaces().isEmpty()) {
                json.key(KEY_JCR_NAMESPACES, JavaxJson.wrap(getJcrNamespaces()));
            }
            if (!getJcrNodetypes().isEmpty()) {
                json.key(KEY_JCR_NODETYPES,
                        JavaxJson.wrap(JsonCnd
                                .toJson(getJcrNodetypes(),
                                        JsonCnd.toNamespaceMapping(getJcrNamespaces()))));
            }
            if (!getJcrPrivileges().isEmpty()) {
                json.key(KEY_JCR_PRIVILEGES, JavaxJson.wrap(getJcrPrivileges()));
            }
            if (!getCndUrls().isEmpty()) {
                json.key(KEY_CND_URLS, JavaxJson.wrap(getCndUrls()));
            }
            if (!getForcedRoots().isEmpty()) {
                json.key(KEY_FORCED_ROOTS, JavaxJson.wrap(getForcedRoots()));
            }
            if (!getChecks().isEmpty()) {
                json.key(KEY_CHECKS, JavaxJson.wrap(getChecks()));
            }
            return json.get();
        }
    }

    /**
     * Create a Checklist from a JsonObject.
     *
     * @param moduleName  module name is required at this point.
     * @param manifestUrl manifest resource url
     * @param json        check list blob
     * @return the new package checklist
     */
    public static Checklist fromJson(@NotNull final String moduleName,
                                     @Nullable final URL manifestUrl,
                                     @NotNull final JsonObject json) {
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

        final List<JcrNs> jcrNsList = new ArrayList<>();
        optArray(json, KEY_JCR_NAMESPACES).ifPresent(jsonArray -> {
            jcrNsList.addAll(JavaxJson.mapArrayOfObjects(jsonArray, JcrNs::fromJson));
            builder.withJcrNamespaces(jcrNsList);
        });
        optObject(json, KEY_JCR_NODETYPES).ifPresent(jsonObject -> {
            builder.withJcrNodetypes(JsonCnd.getQTypesFromJson(jsonObject, JsonCnd.toNamespaceMapping(jcrNsList)));
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
        final Checklist checklist = builder.build(json);
        LOGGER.trace("[fromJson] builder.build(): {}", checklist);
        return checklist;
    }


}
