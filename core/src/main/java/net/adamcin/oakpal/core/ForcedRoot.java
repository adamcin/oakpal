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

import static net.adamcin.oakpal.core.Fun.inferTest1;
import static net.adamcin.oakpal.core.Fun.streamIt;
import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfStrings;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static net.adamcin.oakpal.core.JavaxJson.optArray;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.json.JsonObject;

import org.jetbrains.annotations.NotNull;

/**
 * Encapsulation of details necessary to force creation of a particular root path.
 */
public final class ForcedRoot implements JavaxJson.ObjectConvertible, Comparable<ForcedRoot> {
    static final String KEY_PATH = "path";
    static final String KEY_PRIMARY_TYPE = "primaryType";
    static final String KEY_MIXIN_TYPES = "mixinTypes";

    private String path;

    private String primaryType;

    @NotNull
    private List<String> mixinTypes = Collections.emptyList();

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String primaryType) {
        this.primaryType = primaryType;
    }

    public List<String> getMixinTypes() {
        return mixinTypes;
    }

    public void setMixinTypes(List<String> mixinTypes) {
        this.mixinTypes = mixinTypes;
    }

    public ForcedRoot withPath(final String path) {
        this.path = path;
        return this;
    }

    public ForcedRoot withPrimaryType(final String primaryType) {
        this.primaryType = primaryType;
        return this;
    }

    public ForcedRoot withMixinTypes(final String... mixinTypes) {
        if (mixinTypes != null) {
            this.mixinTypes = Arrays.asList(mixinTypes);
        } else {
            this.mixinTypes = Collections.emptyList();
        }
        return this;
    }

    /**
     * Map a JSON object to a {@link ForcedRoot}.
     *
     * @param json JSON object
     * @return a new forced root
     */
    public static ForcedRoot fromJson(final JsonObject json) {
        final ForcedRoot forcedRoot = new ForcedRoot();
        if (json.containsKey(KEY_PATH)) {
            forcedRoot.setPath(json.getString(KEY_PATH));
        }
        if (json.containsKey(KEY_PRIMARY_TYPE)) {
            forcedRoot.setPrimaryType(json.getString(KEY_PRIMARY_TYPE));
        }
        optArray(json, KEY_MIXIN_TYPES).ifPresent(jsonArray -> {
            forcedRoot.setMixinTypes(mapArrayOfStrings(jsonArray, Function.identity()));
        });
        return forcedRoot;
    }

    /**
     * List the namespace prefixes referenced by elements of this forcedRoot.
     *
     * @return an array of namespace prefixes
     */
    public String[] getNamespacePrefixes() {
        return Stream.concat(
                streamIt(path).flatMap(path ->
                        Stream.of(path.split("/"))
                                .filter(inferTest1(String::isEmpty).negate())
                                .flatMap(JsonCnd::streamNsPrefix)),
                Stream.concat(
                        streamIt(primaryType).flatMap(JsonCnd::streamNsPrefix),
                        mixinTypes.stream().flatMap(JsonCnd::streamNsPrefix)))
                .toArray(String[]::new);
    }

    @Override
    public JsonObject toJson() {
        return obj().key(KEY_PATH, this.path)
                .key(KEY_PRIMARY_TYPE).opt(this.primaryType)
                .key(KEY_MIXIN_TYPES).opt(this.mixinTypes)
                .get();
    }

    @Override
    public int compareTo(final @NotNull ForcedRoot o) {
        return (Optional.ofNullable(this.getPath()).orElse("") + "/")
                .compareTo(Optional.ofNullable(o.getPath()).orElse("") + "/");
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
