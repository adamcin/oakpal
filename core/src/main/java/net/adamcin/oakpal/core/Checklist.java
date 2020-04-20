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

import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.JsonObjectConvertible;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static net.adamcin.oakpal.api.Fun.compose;
import static net.adamcin.oakpal.api.Fun.uncheck1;
import static net.adamcin.oakpal.api.JavaxJson.hasNonNull;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static net.adamcin.oakpal.api.JavaxJson.optArray;
import static net.adamcin.oakpal.api.JavaxJson.optObject;
import static net.adamcin.oakpal.api.JavaxJson.parseFromArray;

/**
 * It's a list of checks, along with initStage properties allowing jars to share CNDs, JCR namespaces and privileges,
 * and forced roots.
 */
public final class Checklist implements JsonObjectConvertible {

    /**
     * Json keys for Checklist. Use {@link #keys()} to access singleton.
     */
    public interface JsonKeys {
        String name();

        String cndUrls();

        String cndNames();

        String jcrNodetypes();

        String jcrNamespaces();

        String jcrPrivileges();

        String forcedRoots();

        String checks();

        List<String> orderedKeys();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        private final List<String> allKeys = Arrays.asList(
                name(),
                checks(),
                forcedRoots(),
                cndNames(),
                cndUrls(),
                jcrNodetypes(),
                jcrPrivileges(),
                jcrNamespaces());

        @Override
        public String name() {
            return "name";
        }

        @Override
        public String cndUrls() {
            return "cndUrls";
        }

        @Override
        public String cndNames() {
            return "cndNames";
        }

        @Override
        public String jcrNodetypes() {
            return "jcrNodetypes";
        }

        @Override
        public String jcrNamespaces() {
            return "jcrNamespaces";
        }

        @Override
        public String jcrPrivileges() {
            return "jcrPrivileges";
        }

        @Override
        public String forcedRoots() {
            return "forcedRoots";
        }

        @Override
        public String checks() {
            return "checks";
        }

        @Override
        public List<String> orderedKeys() {
            return allKeys;
        }
    };

    @NotNull
    public static Checklist.JsonKeys keys() {
        return KEYS;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Checklist.class);

    static final Comparator<String> checklistKeyComparator = (s1, s2) -> {
        final List<String> keyOrder = keys().orderedKeys();
        if (keyOrder.contains(s1) && keyOrder.contains(s2)) {
            return Integer.compare(keyOrder.indexOf(s1), keyOrder.indexOf(s2));
        } else if (keyOrder.contains(s1)) {
            return -1;
        } else if (keyOrder.contains(s2)) {
            return 1;
        } else {
            return s1.compareTo(s2);
        }
    };

    public static <T> Comparator<T> comparingJsonKeys(final @NotNull Function<T, String> jsonKeyExtractor) {
        return (t1, t2) -> checklistKeyComparator.compare(jsonKeyExtractor.apply(t1), jsonKeyExtractor.apply(t2));
    }

    private final JsonObject originalJson;
    private final String moduleName;
    private final String name;
    private final List<URL> cndUrls;
    private final List<JcrNs> jcrNamespaces;
    private final List<QNodeTypeDefinition> jcrNodetypes;
    private final List<PrivilegeDefinition> jcrPrivileges;
    private final List<ForcedRoot> forcedRoots;
    private final List<CheckSpec.ImmutableSpec> checks;

    Checklist(final @Nullable JsonObject originalJson,
              final @Nullable String moduleName,
              final @Nullable String name,
              final @NotNull List<URL> cndUrls,
              final @NotNull List<JcrNs> jcrNamespaces,
              final @NotNull List<QNodeTypeDefinition> jcrNodetypes,
              final @NotNull List<PrivilegeDefinition> jcrPrivileges,
              final @NotNull List<ForcedRoot> forcedRoots,
              final @NotNull List<CheckSpec.ImmutableSpec> checks) {
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

    public List<String> getJcrPrivilegeNames() {
        final NamePathResolver resolver = new DefaultNamePathResolver(JsonCnd.toNamespaceMapping(jcrNamespaces));
        return jcrPrivileges.stream()
                .map(compose(PrivilegeDefinition::getName, uncheck1(resolver::getJCRName)))
                .collect(Collectors.toList());
    }

    public List<PrivilegeDefinition> getJcrPrivileges() {
        return jcrPrivileges;
    }

    public List<ForcedRoot> getForcedRoots() {
        return forcedRoots;
    }

    public List<CheckSpec> getChecks() {
        final String prefix = getCheckPrefix(this.moduleName, this.name);
        return checks.stream()
                .map(spec -> insertPrefix(spec, prefix))
                .collect(Collectors.toList());
    }

    public InitStage asInitStage() {
        InitStage.Builder builder = new InitStage.Builder()
                .withOrderedCndUrls(getCndUrls())
                .withForcedRoots(getForcedRoots())
                .withPrivileges(getJcrPrivilegeNames())
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
        private List<PrivilegeDefinition> jcrPrivileges = new ArrayList<>();
        private List<ForcedRoot> forcedRoots = new ArrayList<>();
        private List<CheckSpec> checks = new ArrayList<>();

        Builder(final @NotNull String moduleName) {
            this.moduleName = moduleName;
        }

        Builder withName(final @Nullable String name) {
            this.name = name;
            return this;
        }

        Builder withCndUrls(final @NotNull List<URL> cndUrls) {
            this.cndUrls.addAll(cndUrls);
            return this;
        }

        Builder withJcrNamespaces(final @NotNull List<JcrNs> jcrNamespaces) {
            this.jcrNamespaces.addAll(jcrNamespaces);
            return this;
        }

        Builder withJcrNodetypes(final @NotNull List<QNodeTypeDefinition> jcrNodetypes) {
            this.jcrNodetypes.addAll(jcrNodetypes);
            return this;
        }

        Builder withJcrPrivileges(final @NotNull List<PrivilegeDefinition> jcrPrivileges) {
            this.jcrPrivileges.addAll(jcrPrivileges);
            return this;
        }

        Builder withForcedRoots(final @NotNull List<ForcedRoot> forcedRoots) {
            this.forcedRoots.addAll(forcedRoots);
            return this;
        }

        Builder withChecks(final @NotNull List<CheckSpec> checks) {
            this.checks.addAll(checks.stream()
                    .filter(this::isValidCheckspec)
                    .collect(Collectors.toList()));
            return this;
        }

        boolean isValidCheckspec(final @NotNull CheckSpec check) {
            return !check.isAbstract()
                    && check.getName() != null
                    && !check.getName().isEmpty()
                    && !check.getName().contains("/");
        }


        public Checklist build() {
            return build(null);
        }

        Checklist build(final @Nullable JsonObject originalJson) {
            return new Checklist(originalJson, moduleName,
                    name != null ? name : moduleName,
                    Collections.unmodifiableList(cndUrls),
                    Collections.unmodifiableList(jcrNamespaces),
                    Collections.unmodifiableList(jcrNodetypes),
                    Collections.unmodifiableList(jcrPrivileges),
                    Collections.unmodifiableList(forcedRoots),
                    Collections.unmodifiableList(checks.stream()
                            .map(CheckSpec::immutableCopyOf)
                            .collect(Collectors.toList())));
        }
    }

    private CheckSpec insertPrefix(final @NotNull CheckSpec checkSpec, final String prefix) {
        final CheckSpec copy = CheckSpec.copyOf(checkSpec);
        copy.setName(prefix + checkSpec.getName());
        return copy;
    }

    static String getCheckPrefix(final @Nullable String moduleName,
                                 final @Nullable String checklistName) {
        final String modulePrefix = ofNullable(moduleName)
                .filter(name -> !name.isEmpty())
                .map(name -> name + "/")
                .orElse("");
        return ofNullable(checklistName)
                .filter(name -> !name.isEmpty() && !name.equals(moduleName))
                .map(name -> modulePrefix + name + "/")
                .orElse(modulePrefix);
    }

    @Override
    public String toString() {
        return "Checklist{" +
                "moduleName='" + moduleName + '\'' +
                ", json='" + toJson().toString() + "'}";
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
            final NamespaceMapping mapping = JsonCnd.toNamespaceMapping(getJcrNamespaces());
            final JsonKeys jsonKeys = keys();
            return obj()
                    .key(jsonKeys.name()).opt(getName())
                    .key(jsonKeys.checks()).opt(checks)
                    .key(jsonKeys.forcedRoots()).opt(getForcedRoots())
                    .key(jsonKeys.cndUrls()).opt(getCndUrls())
                    .key(jsonKeys.jcrNodetypes()).opt(JsonCnd.toJson(getJcrNodetypes(), mapping))
                    .key(jsonKeys.jcrPrivileges()).opt(JsonCnd.privilegesToJson(getJcrPrivileges(), mapping))
                    .key(jsonKeys.jcrNamespaces()).opt(getJcrNamespaces())
                    .get();
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
    public static Checklist fromJson(final @NotNull String moduleName,
                                     final @Nullable URL manifestUrl,
                                     final @NotNull JsonObject json) {
        LOGGER.trace("[fromJson] module: {}, manifestUrl: {}, json: {}", moduleName, manifestUrl, json);
        Builder builder = new Builder(moduleName);
        final JsonKeys jsonKeys = keys();
        if (hasNonNull(json, jsonKeys.name())) {
            builder.withName(json.getString(jsonKeys.name()));
        }

        if (manifestUrl != null && manifestUrl.toExternalForm().endsWith(JarFile.MANIFEST_NAME)) {
            ofNullable(json.getJsonArray(jsonKeys.cndNames()))
                    .filter(Util.traceFilter(LOGGER, "[fromJson#cndNames] cndNames: {}"))
                    .map(cndNames -> JavaxJson.unwrapArray(cndNames).stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList()))
                    .map(names -> Util.resolveManifestResources(manifestUrl, names))
                    .ifPresent(builder::withCndUrls);
        }

        builder.withCndUrls(parseFromArray(
                optArray(json, jsonKeys.cndUrls()).orElse(JsonValue.EMPTY_JSON_ARRAY), URL::new,
                (element, error) -> LOGGER.debug("[fromJson#cndUrls] (URL ERROR) {}", error.getMessage())));

        final List<JcrNs> jcrNsList = new ArrayList<>();
        optArray(json, jsonKeys.jcrNamespaces()).ifPresent(jsonArray -> {
            jcrNsList.addAll(JavaxJson.mapArrayOfObjects(jsonArray, JcrNs::fromJson));
            builder.withJcrNamespaces(jcrNsList);
        });
        optObject(json, jsonKeys.jcrNodetypes()).ifPresent(jsonObject -> {
            builder.withJcrNodetypes(
                    JsonCnd.getQTypesFromJson(jsonObject,
                            JsonCnd.toNamespaceMapping(jcrNsList)));
        });
        if (json.containsKey(jsonKeys.jcrPrivileges())) {
            builder.withJcrPrivileges(
                    JsonCnd.getPrivilegesFromJson(json.get(jsonKeys.jcrPrivileges()),
                            JsonCnd.toNamespaceMapping(jcrNsList)));
        }
        optArray(json, jsonKeys.forcedRoots()).ifPresent(jsonArray -> {
            builder.withForcedRoots(JavaxJson.mapArrayOfObjects(jsonArray, ForcedRoot::fromJson));
        });
        optArray(json, jsonKeys.checks()).ifPresent(jsonArray -> {
            builder.withChecks(JavaxJson.mapArrayOfObjects(jsonArray, CheckSpec::fromJson));
        });
        final Checklist checklist = builder.build(json);
        LOGGER.trace("[fromJson] builder.build(): {}", checklist);
        return checklist;
    }


}
