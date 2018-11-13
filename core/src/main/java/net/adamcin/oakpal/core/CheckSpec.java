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

import org.json.JSONObject;

/**
 * DTO for full-featured check spec.
 */
public class CheckSpec {
    static final String KEY_IMPL = "impl";
    static final String KEY_NAME = "name";
    static final String KEY_TEMPLATE = "template";
    static final String KEY_SKIP = "skip";
    static final String KEY_CONFIG = "config";

    private String impl;
    private String name;
    private String template;
    private boolean skip;
    private JSONObject config;

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
    public JSONObject getConfig() {
        return config;
    }

    public void setConfig(final JSONObject config) {
        this.config = config;
    }

    public boolean overrides(final CheckSpec other) {
        return !this.mustInherit() && !other.mustInherit()
                && (String.valueOf(other.getName()).equals(String.valueOf(this.getName()))
                || (this.impl == null
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

    static JSONObject merge(final JSONObject base, final JSONObject overlay) {
        JSONObject init = ofNullable(base).orElse(new JSONObject());
        ofNullable(overlay).ifPresent(ext -> ext.keySet().forEach(key -> init.put(key, ext.get(key))));
        return init;
    }

    /**
     * Build a {@link CheckSpec} from a {@link Checklist} json snippet.
     *
     * @param json check spec object
     * @return a new CheckSpec
     */
    static CheckSpec fromJSON(final JSONObject json) {
        final CheckSpec checkSpec = new CheckSpec();
        if (json.has(KEY_IMPL)) {
            checkSpec.setImpl(json.getString(KEY_IMPL));
        }
        if (json.has(KEY_NAME)) {
            checkSpec.setName(json.getString(KEY_NAME));
        }
        if (json.has(KEY_TEMPLATE)) {
            checkSpec.setTemplate(json.getString(KEY_TEMPLATE));
        }
        if (json.has(KEY_SKIP)) {
            checkSpec.setSkip(json.optBoolean(KEY_SKIP));
        }
        if (json.has(KEY_CONFIG)) {
            checkSpec.setConfig(json.getJSONObject(KEY_CONFIG));
        }

        return checkSpec;
    }

    @Override
    public String toString() {
        JSONObject obj = new JSONObject();
        obj.put(KEY_NAME, getName());
        obj.put(KEY_IMPL, getImpl());
        obj.put(KEY_CONFIG, getConfig());
        obj.put(KEY_TEMPLATE, getTemplate());
        obj.put(KEY_SKIP, isSkip());
        return obj.toString();
    }
}
