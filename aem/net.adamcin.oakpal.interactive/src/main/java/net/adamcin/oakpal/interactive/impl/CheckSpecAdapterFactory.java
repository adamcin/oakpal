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

package net.adamcin.oakpal.interactive.impl;

import static java.util.Optional.ofNullable;

import java.io.StringReader;
import java.util.Calendar;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.JavaxJson;
import net.adamcin.oakpal.interactive.OakpalInteractiveConstants;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;

/**
 * Adapts Resources to CheckSpecs.
 */
@Component(property = {
        "adaptables=" + OakpalInteractiveConstants.ADAPTABLE_RESOURCE,
        "adapters=" + OakpalInteractiveConstants.ADAPTER_CHECK_SPEC
})
class CheckSpecAdapterFactory implements AdapterFactory {
    static final String KEY_IMPL = "impl";
    static final String KEY_INLINE_SCRIPT = "inlineScript";
    static final String KEY_INLINE_ENGINE = "inlineEngine";
    static final String KEY_NAME = "name";
    static final String KEY_TEMPLATE = "template";
    static final String KEY_SKIP = "skip";
    static final String KEY_INLINE_CONFIG = "inlineConfig";
    // named differently, specifically to avoid getting hidden by */config rep:ACE glob restrictions
    static final String KEY_CONFIG = "checkConfig";

    static final String KEY_OAKPAL_CONFIG_ARRAY = "oakpalConfigArray";

    @SuppressWarnings("uncheckVoid")
    @Override
    public <AdapterType> AdapterType getAdapter(final Object adaptable, final Class<AdapterType> type) {
        if (adaptable instanceof Resource && type == CheckSpec.class) {
            return (AdapterType) getAdapter((Resource) adaptable);
        }
        return null;
    }

    CheckSpec getAdapter(final Resource resource) {
        CheckSpec check = new CheckSpec();

        ValueMap props = resource.getValueMap();
        ofNullable(props.get(KEY_NAME, String.class)).ifPresent(check::setName);
        ofNullable(props.get(KEY_IMPL, String.class)).ifPresent(check::setImpl);
        ofNullable(props.get(KEY_TEMPLATE, String.class)).ifPresent(check::setTemplate);
        ofNullable(props.get(KEY_SKIP, Boolean.class)).ifPresent(check::setSkip);
        ofNullable(props.get(KEY_INLINE_SCRIPT, String.class)).ifPresent(check::setInlineScript);
        ofNullable(props.get(KEY_INLINE_ENGINE, String.class)).ifPresent(check::setInlineEngine);
        ofNullable(props.get(KEY_INLINE_CONFIG, String.class)).ifPresent(inlineConfig -> {
            try (JsonReader reader = Json.createReader(new StringReader(inlineConfig))) {
                check.setConfig(reader.readObject());
            }
        });
        ofNullable(resource.getChild(KEY_CONFIG)).map(this::toJsonObject).ifPresent(check::setConfig);

        return check;
    }

    static boolean isJsonScalar(final Object value) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Calendar
                || value instanceof JavaxJson.ArrayConvertible
                || value instanceof JavaxJson.ObjectConvertible
                || value.getClass().isArray();
    }

    JsonArray toJsonArray(final Resource configResource) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Resource child : configResource.getChildren()) {
            if (child.getValueMap().get(KEY_OAKPAL_CONFIG_ARRAY, false)) {
                builder.add(toJsonArray(child));
            } else {
                builder.add(toJsonObject(child));
            }
        }
        return builder.build();
    }

    JsonObject toJsonObject(final Resource configResource) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        configResource.getValueMap().entrySet().stream()
                .filter(entry -> isJsonScalar(entry.getValue()))
                .forEachOrdered(entry -> builder.add(entry.getKey(), JavaxJson.wrap(entry.getValue())));

        for (Resource child : configResource.getChildren()) {
            if (child.getValueMap().get(KEY_OAKPAL_CONFIG_ARRAY, false)) {
                builder.add(child.getName(), toJsonArray(child));
            } else {
                builder.add(child.getName(), toJsonObject(child));
            }
        }

        return builder.build();
    }
}
