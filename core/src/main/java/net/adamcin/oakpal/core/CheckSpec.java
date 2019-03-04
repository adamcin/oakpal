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
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static net.adamcin.oakpal.core.Util.isEmpty;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonCollectors;

/**
 * DTO for full-featured check spec.
 */
public class CheckSpec {
    static final String KEY_IMPL = "impl";
    static final String KEY_INLINE_SCRIPT = "inlineScript";
    static final String KEY_INLINE_ENGINE = "inlineEngine";
    static final String KEY_NAME = "name";
    static final String KEY_TEMPLATE = "template";
    static final String KEY_SKIP = "skip";
    static final String KEY_CONFIG = "config";

    private String impl;
    private String inlineScript;
    private String inlineEngine;
    private String name;
    private String template;
    private boolean skip;
    private JsonObject config;

    /**
     * The direct classpath lookup name for a particular check. If not provided, indicates that a check should be
     * looked up by name from a catalog on the classpath.
     *
     * @return className or script resource name of a {@link ProgressCheck} or {@link ProgressCheckFactory}.
     */
    public String getImpl() {
        return impl;
    }

    public void setImpl(final String impl) {
        this.impl = impl;
    }

    public String getInlineScript() {
        return inlineScript;
    }

    public void setInlineScript(final String inlineScript) {
        this.inlineScript = inlineScript;
    }

    public String getInlineEngine() {
        return inlineEngine;
    }

    public void setInlineEngine(final String inlineEngine) {
        this.inlineEngine = inlineEngine;
    }

    public boolean isAbstract() {
        return isEmpty(this.impl) && this.inlineScript == null;
    }

    /**
     * The display name for the check. If "impl" is provided, and represents a script package check or a class that
     * implements {@link ProgressCheckFactory} this is treated as an alias for the check during
     * this execution.
     * <p>
     * If "impl" is not provided, this is used to lookup a catalog check.
     * </p>
     * <p>
     * If "impl" is not a {@link ProgressCheckFactory}, this value is ignored.
     * </p>
     *
     * @return the checkName
     */
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * If specified, inherit the impl and config of another {@link CheckSpec}, following the same rules of {@code name}
     * matching that apply to {@link #getName()}.
     *
     * @return the checkName to inherit from
     */
    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }

    public boolean mustInherit() {
        return this.getTemplate() != null && !this.getTemplate().trim().isEmpty();
    }

    /**
     * Whether to skip this check during a scan. Only useful to set in a parameterized context or as an override for a
     * checklist spec.
     *
     * @return skip
     */
    public boolean isSkip() {
        return skip;
    }

    public void setSkip(final boolean skip) {
        this.skip = skip;
    }

    public boolean notSkipped() {
        return !isSkip();
    }

    /**
     * If {@code impl} references a script check or a {@link ProgressCheckFactory},
     * or if the check loaded from a catalog by {@code name} is a script check or a
     * {@link ProgressCheckFactory}, this is used to configure the check.
     *
     * @return the JSONObject configuration for the package check
     */
    public JsonObject getConfig() {
        return config;
    }

    public void setConfig(final JsonObject config) {
        this.config = config;
    }

    public boolean overrides(final CheckSpec other) {
        return !this.mustInherit() && !other.mustInherit()
                && (String.valueOf(other.getName()).equals(String.valueOf(this.getName()))
                || (this.isAbstract()
                && String.valueOf(other.getName()).endsWith("/" + String.valueOf(this.getName()))));
    }

    public boolean isOverriddenBy(final CheckSpec other) {
        return other.overrides(this);
    }

    public CheckSpec overlay(final CheckSpec other) {
        final CheckSpec composite = new CheckSpec();
        composite.setName(other.getName());
        composite.setSkip(this.isSkip() || other.isSkip());
        composite.setImpl(ofNullable(this.getImpl()).orElse(other.getImpl()));
        composite.setConfig(merge(other.getConfig(), this.getConfig()));
        return composite;
    }

    public boolean inherits(final CheckSpec other) {
        return this.mustInherit() && !other.mustInherit() && !this.overrides(other)
                && !this.getTemplate().equals(this.getName())
                && other.getName() != null && !other.getName().isEmpty()
                && (String.valueOf(other.getName()).equals(String.valueOf(this.getTemplate()))
                || String.valueOf(other.getName()).endsWith("/" + String.valueOf(this.getTemplate())));
    }

    public boolean isInheritedBy(final CheckSpec other) {
        return other.inherits(this);
    }

    public CheckSpec inherit(final CheckSpec other) {
        final CheckSpec composite = new CheckSpec();
        composite.setImpl(ofNullable(this.getImpl()).orElse(other.getImpl()));
        composite.setName(ofNullable(this.getName()).orElse(other.getName()));
        composite.setSkip(this.isSkip());
        composite.setConfig(merge(other.getConfig(), this.getConfig()));
        return composite;
    }

    static JsonObject merge(final JsonObject base, final JsonObject overlay) {
        JsonObjectBuilder init = Json.createObjectBuilder();
        ofNullable(base).ifPresent(json -> json.forEach(init::add));
        ofNullable(overlay).ifPresent(json -> json.forEach(init::add));
        return init.build();
    }

    /**
     * Build a {@link CheckSpec} from a {@link Checklist} json snippet.
     *
     * @param json check spec object
     * @return a new CheckSpec
     */
    static CheckSpec fromJson(final JsonObject json) {
        final CheckSpec checkSpec = new CheckSpec();
        if (json.containsKey(KEY_IMPL)) {
            checkSpec.setImpl(json.getString(KEY_IMPL));
        }
        if (json.containsKey(KEY_INLINE_SCRIPT)) {
            checkSpec.setImpl(json.getString(KEY_INLINE_SCRIPT));
        }
        if (json.containsKey(KEY_INLINE_ENGINE)) {
            checkSpec.setImpl(json.getString(KEY_INLINE_ENGINE));
        }
        if (json.containsKey(KEY_NAME)) {
            checkSpec.setName(json.getString(KEY_NAME));
        }
        if (json.containsKey(KEY_TEMPLATE)) {
            checkSpec.setTemplate(json.getString(KEY_TEMPLATE));
        }
        if (json.containsKey(KEY_SKIP)) {
            checkSpec.setSkip(json.getBoolean(KEY_SKIP));
        }
        if (json.containsKey(KEY_CONFIG)) {
            checkSpec.setConfig(json.getJsonObject(KEY_CONFIG));
        }

        return checkSpec;
    }

    @Override
    public String toString() {
        return obj()
                .key(KEY_NAME, getName())
                .key(KEY_IMPL, getImpl())
                .key(KEY_INLINE_SCRIPT, getInlineScript())
                .key(KEY_INLINE_ENGINE, getInlineEngine())
                .key(KEY_CONFIG, getConfig())
                .key(KEY_TEMPLATE, getTemplate())
                .key(KEY_SKIP, isSkip())
                .get().toString();
    }
}
