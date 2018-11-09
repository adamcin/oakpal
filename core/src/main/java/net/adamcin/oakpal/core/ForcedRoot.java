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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

/**
 * Encapsulation of details necessary to force creation of a particular root path.
 */
public final class ForcedRoot {
    static final String KEY_PATH = "path";
    static final String KEY_PRIMARY_TYPE = "primaryType";
    static final String KEY_MIXIN_TYPES = "mixinTypes";

    private String path;

    private String primaryType;

    private List<String> mixinTypes;

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
            this.mixinTypes = null;
        }
        return this;
    }

    /**
     * Map a JSON object to a {@link ForcedRoot}.
     *
     * @param json JSON object
     * @return a new forced root
     */
    static ForcedRoot fromJSON(final JSONObject json) {
        final ForcedRoot forcedRoot = new ForcedRoot();
        ofNullable(json.optString(KEY_PATH)).ifPresent(forcedRoot::setPath);
        ofNullable(json.optString(KEY_PRIMARY_TYPE)).ifPresent(forcedRoot::setPrimaryType);
        ofNullable(json.optJSONArray(KEY_MIXIN_TYPES))
                .map(types -> types.toList().stream().map(String::valueOf).collect(Collectors.toList()))
                .ifPresent(forcedRoot::setMixinTypes);
        return forcedRoot;
    }
}
