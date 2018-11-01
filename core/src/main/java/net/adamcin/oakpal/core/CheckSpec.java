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

import java.util.Map;

import org.json.JSONObject;

/**
 * DTO for full-featured check spec.
 */
public class CheckSpec {
    static final String KEY_IMPL = "impl";
    static final String KEY_NAME = "name";
    static final String KEY_SKIP = "skip";
    static final String KEY_CONFIG = "config";

    private String impl;
    private String name;
    private boolean skip;
    private JSONObject config;

    /**
     * The direct classpath lookup name for a particular check. If not provided, indicates that a check should be
     * looked up by name from a catalog on the classpath.
     *
     * @return className or script resource name of a {@link PackageCheck} or {@link PackageCheckFactory}.
     */
    public String getImpl() {
        return impl;
    }

    public void setImpl(final String impl) {
        this.impl = impl;
    }

    /**
     * The display name for the check. If "impl" is provided, and represents a script package check or a class that
     * implements {@link PackageCheckFactory} this is treated as an alias for the check during
     * this execution.
     * <p>
     * If "impl" is not provided, this is used to lookup a catalog check.
     * </p>
     * <p>
     * If "impl" is not a {@link PackageCheckFactory}, this value is ignored.
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
     * If {@code impl} references a script check or a {@link PackageCheckFactory},
     * or if the check loaded from a catalog by {@code name} is a script check or a
     * {@link PackageCheckFactory}, this is used to configure the check.
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
        return String.valueOf(other.getName()).equals(String.valueOf(this.getName()))
                || (this.impl == null && String.valueOf(other.getName()).endsWith("/" + String.valueOf(this.getName())));
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

    static JSONObject merge(final JSONObject base, final JSONObject overlay) {
        JSONObject init = ofNullable(base).orElse(new JSONObject());
        ofNullable(overlay).map(JSONObject::toMap).ifPresent(entries -> entries.forEach(init::accumulate));
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
        ofNullable(json.optString(KEY_IMPL)).ifPresent(checkSpec::setImpl);
        ofNullable(json.optString(KEY_NAME)).ifPresent(checkSpec::setName);
        if (json.has(KEY_SKIP)) {
            checkSpec.setSkip(json.optBoolean(KEY_SKIP));
        }
        ofNullable(json.optJSONObject(KEY_CONFIG)).ifPresent(checkSpec::setConfig);
        return checkSpec;
    }
}
