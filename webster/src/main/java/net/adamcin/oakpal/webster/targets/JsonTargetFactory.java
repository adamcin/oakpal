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

package net.adamcin.oakpal.webster.targets;

import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.Fun.uncheck2;
import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfObjects;
import static net.adamcin.oakpal.core.JavaxJson.mapObjectValues;
import static net.adamcin.oakpal.core.JavaxJson.obj;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.json.JsonObject;
import javax.json.JsonValue;

import net.adamcin.oakpal.webster.WebsterTarget;
import net.adamcin.oakpal.webster.WebsterTargetFactory;
import org.jetbrains.annotations.NotNull;

/**
 * TODO Examples...
 */
public enum JsonTargetFactory implements WebsterTargetFactory {
    /**
     * The nodetypes target maintains nodetypes.cnd files in a filevault-package archive root, at the
     * {@code src/main/content/META-INF/vault/nodetypes.cnd} path by default.
     */
    NODETYPES(WebsterNodetypesTarget::fromJson),

    /**
     * The privileges target maintains a privileges.xml file in a filevault-package archive root, at the
     * {@code src/main/content/META-INF/vault/privileges.xml} path by default.
     */
    PRIVILEGES((target, config) -> {
        return new WebsterPrivilegesTarget(target);
    }),

    /**
     * The checklist target helps maintain checklist json files in an oakpal module. There is no default path, so it
     * must be specified.
     */
    CHECKLIST(WebsterChecklistTarget::fromJson);

    JsonTargetFactory(final WebsterTargetFactory actionFactory) {
        this.actionFactory = actionFactory;
    }

    private WebsterTargetFactory actionFactory;

    public static final String KEY_TYPE = "type";
    public static final String KEY_FILE = "file";
    public static final String KEY_CONFIG = "config";
    public static final String HINT_KEY_MORE_TARGETS = "moreTargets";

    public static JsonTargetFactory byType(final String type) {
        for (JsonTargetFactory value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown JsonActionFactory type: " + type);
    }

    public static boolean isTargetType(final String type) {
        for (JsonTargetFactory value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    public static WebsterTarget fromJson(final File baseDir, final JsonObject json, final String hint) throws Exception {
        final String action = json.getString(KEY_TYPE, hint);
        final File target = json.containsKey(KEY_FILE) ? new File(baseDir, json.getString(KEY_FILE)) : null;
        final JsonObject config = json.containsKey(KEY_CONFIG) ? json.getJsonObject(KEY_CONFIG) : obj().get();
        return byType(action).createTarget(target, config);
    }

    /**
     * For convenience, one target of each type is supported at the root of the targets configuration object, with each
     * key referencing the type of the target. This is referred to as the hint map. A fourth key, {@link #HINT_KEY_MORE_TARGETS},
     * defines a list of additional targets of any of the supported types, but the {@link #KEY_TYPE} attribute must be
     * specified for each entry.
     *
     * @param baseDir     the base directory for resolving relative paths
     * @param jsonHintMap the root webster targets configuration object
     * @return a list of constructed webster targets
     * @throws Exception if configuration syntax is invalid
     */
    public static List<WebsterTarget> fromJsonHintMap(@NotNull final File baseDir,
                                                      @NotNull final JsonObject jsonHintMap) throws Exception {
        List<WebsterTarget> targets = new ArrayList<>();
        for (Map.Entry<String, JsonValue> entry : jsonHintMap.entrySet()) {
            if (isTargetType(entry.getKey())) {
                if (entry.getValue().getValueType() == JsonValue.ValueType.OBJECT) {
                    targets.add(fromJson(baseDir, entry.getValue().asJsonObject(), entry.getKey()));
                } else {
                    targets.add(fromJson(baseDir, obj().get(), entry.getKey()));
                }
            } else if (HINT_KEY_MORE_TARGETS.equals(entry.getKey())) {
                if (entry.getValue().getValueType() == JsonValue.ValueType.ARRAY) {
                    targets.addAll(mapArrayOfObjects(entry.getValue().asJsonArray(),
                            uncheck1(json -> fromJson(baseDir, json, null))));
                } else if (entry.getValue().getValueType() == JsonValue.ValueType.OBJECT) {
                    targets.addAll(mapObjectValues(entry.getValue().asJsonObject(),
                            uncheck2((label, json) -> fromJson(baseDir, json, label)), true));
                }
            }
        }
        return targets;
    }

    @Override
    public WebsterTarget createTarget(final File targetFile, final JsonObject config) throws Exception {
        return actionFactory.createTarget(targetFile, config);
    }
}
