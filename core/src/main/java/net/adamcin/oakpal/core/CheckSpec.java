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
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static net.adamcin.oakpal.api.JavaxJson.hasNonNull;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static net.adamcin.oakpal.core.Util.isEmpty;

/**
 * DTO for full-featured check spec.
 */
@SuppressWarnings("WeakerAccess")
@ProviderType
public class CheckSpec implements JsonObjectConvertible {
    /**
     * Json keys for CheckSpec. Use {@link #keys()} to access singleton.
     */
    @ProviderType
    public interface JsonKeys {
        String impl();

        String inlineScript();

        String inlineEngine();

        String name();

        String template();

        String skip();

        String config();
    }

    private static final JsonKeys KEYS = new JsonKeys() {
        @Override
        public String impl() {
            return "impl";
        }

        @Override
        public String inlineScript() {
            return "inlineScript";
        }

        @Override
        public String inlineEngine() {
            return "inlineEngine";
        }

        @Override
        public String name() {
            return "name";
        }

        @Override
        public String template() {
            return "template";
        }

        @Override
        public String skip() {
            return "skip";
        }

        @Override
        public String config() {
            return "config";
        }
    };

    @NotNull
    public static CheckSpec.JsonKeys keys() {
        return KEYS;
    }

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

    /**
     * Set a new impl value. This is considered unspecified if the value is null or empty.
     *
     * @param impl the new value
     */
    public void setImpl(final String impl) {
        this.impl = impl;
    }

    /**
     * As an alternative to {@link #getImpl()}, a CheckSpec may specify script check source code inline, i.e., as a
     * javascript string. This pattern can make checklists more portable, at the expense of readability.
     * This overrides {@link #getImpl()} if this returns non-null.
     *
     * @return a string, invokable as a script, or null, if this field is unspecified.
     * @see #getInlineEngine()
     */
    public String getInlineScript() {
        return inlineScript;
    }

    /**
     * Set a new invokable script check source value. If this is not invokable using the "javascript" engine, be sure to
     * specify the engine name using {@link #setInlineEngine(String)}.
     *
     * @param inlineScript the new value, or null if unspecified
     * @see #getInlineEngine()
     */
    public void setInlineScript(final String inlineScript) {
        this.inlineScript = inlineScript;
    }

    /**
     * Specifies a ScriptEngineFactory name, or null to use the default of "javascript", in conjunction with a non-null
     * value for {@link #getInlineScript()}.
     *
     * @return a specific {@link javax.script.ScriptEngineFactory} name or null to use "javascript"
     * @see #getInlineScript()
     */
    public String getInlineEngine() {
        return inlineEngine;
    }

    /**
     * Set a new {@link javax.script.ScriptEngineFactory} name, or null for the default.
     *
     * @param inlineEngine the new value
     * @see #setInlineScript(String)
     */
    public void setInlineEngine(final String inlineEngine) {
        this.inlineEngine = inlineEngine;
    }

    /**
     * An abstract check spec has neither a non-empty {@link #getImpl()} nor a non-null {@link #getInlineScript()}, and
     * is therefore not invokable on its own. It must overlay another check spec specified in a checklist by matching its
     * name exactly, or matching this name as a suffix of the other the name.
     *
     * @return true if this check spec fails to specify {@link #getImpl()} or {@link #getInlineScript()}
     */
    public final boolean isAbstract() {
        return isEmpty(this.impl) && this.inlineScript == null;
    }

    /**
     * Inverse of {@link #isAbstract()}. Filter for selecting specs that DO have impl or inline attributes.
     *
     * @return true if has implementation
     */
    public final boolean notAbstract() {
        return !isAbstract();
    }

    /**
     * The display name for the check. If "impl" is provided, and represents a script package check or a class that
     * implements {@link ProgressCheck} or {@link ProgressCheckFactory} this is treated as an alias for the check during
     * this execution.
     * <p>
     * If "impl" is not provided, this is used to lookup a checklist check to overlay.
     * </p>
     * <p>
     * If "impl" is not resolved to a progress check of some kind, this value is otherwise ignored.
     * </p>
     *
     * @return the checkName
     */
    public String getName() {
        return name;
    }

    /**
     * Set a new display name for the check.
     *
     * @param name the new value
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * If specified, inherit the {@link #getConfig()} and either the {@link #getImpl()} or {@link #getInlineScript()}
     * and {@link #getInlineEngine()} of another {@link CheckSpec}, following the same rules of {@code name}
     * matching that apply to {@link #getName()}.
     *
     * @return the checkName to inherit from
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Set a new template value.
     *
     * @param template the new value
     */
    public void setTemplate(final String template) {
        this.template = template;
    }

    /**
     * A CheckSpec with a non-empty template MUST inherit from another non-abstract CheckSpec, and therefore, cannot
     * OVERRIDE another CheckSpec.
     *
     * @return true if template is specified
     */
    final boolean mustInherit() {
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

    /**
     * Set to true to skip the check.
     *
     * @param skip the new value
     */
    public void setSkip(final boolean skip) {
        this.skip = skip;
    }

    /**
     * The inverse of {@link #isSkip()}.
     *
     * @return true if not skipped
     */
    public final boolean notSkipped() {
        return !isSkip();
    }

    /**
     * If {@code impl} references a script check or a {@link ProgressCheckFactory},
     * or if the check loaded from a checklist by {@code name} is a script check or a
     * {@link ProgressCheckFactory}, this is used to configure the check.
     *
     * @return the JsonObject configuration for the package check
     */
    public JsonObject getConfig() {
        return config;
    }

    /**
     * Set a new config object value.
     *
     * @param config the new value
     */
    public void setConfig(final JsonObject config) {
        this.config = config;
    }

    /**
     * Returns true if this overrides that.
     * <p>
     * 1. Neither this spec nor that spec have a template specified (use {@link #inherit(CheckSpec)} in that case)
     * 2. Neither this spec nor that spec are unnamed (unnamed specs cannot be overridden).
     * 3a. This name matches that name exactly (explicit override allowing different impl), OR
     * 3b. This is abstract and that name ENDS with "/" + this name (implicit override for skipping or merging config)
     *
     * @param that the other spec
     * @return true if this overrides that
     */
    public final boolean overrides(final @NotNull CheckSpec that) {
        return this.notUnnamed() && that.notUnnamed()
                && !this.mustInherit() && !that.mustInherit()
                && (that.getName().equals(this.getName())
                || (this.isAbstract() && that.getName().endsWith("/" + this.getName())));
    }

    /**
     * Returns true if this spec has a non-blank name.
     *
     * @return true if named
     */
    final boolean notUnnamed() {
        return this.getName() != null && !this.getName().trim().isEmpty();
    }

    /**
     * The inverse of {@link #overrides(CheckSpec)}.
     *
     * @param that the other spec
     * @return true if this is overridden by that
     */
    public final boolean isOverriddenBy(final @NotNull CheckSpec that) {
        return that.overrides(this);
    }

    /**
     * Implements the common composition logic for {@link #overlay(CheckSpec)} and {@link #inherit(CheckSpec)}.
     *
     * @param that the other spec
     * @return a mutable composite CheckSpec
     */
    final CheckSpec baseCompositeOver(final @NotNull CheckSpec that) {
        final CheckSpec composite = copyOf(that);
        // only leave impl OR inlineScript+inlineEngine set in the composite. Be sure to unset the
        // alternative in the composite to reduce confusion about which takes precedence downstream.
        if (this.getInlineScript() != null) {
            composite.setInlineScript(this.getInlineScript());
            composite.setInlineEngine(this.getInlineEngine());
            composite.setImpl(null);
        } else if (!isEmpty(this.getImpl())) {
            composite.setInlineScript(null);
            composite.setInlineEngine(null);
            composite.setImpl(this.getImpl());
        }
        composite.setConfig(merge(that.getConfig(), this.getConfig()));
        return composite;
    }

    /**
     * Apply this spec's attributes as an overlay to that spec's attributes. Recommend checking
     * for a true from {@link #overrides(CheckSpec)}. If either this spec or that spec specify
     * "skip=true", the composite spec will be skipped.
     *
     * @param that the other spec to override
     * @return a new composite spec
     * @see #overrides(CheckSpec)
     */
    public final CheckSpec overlay(final @NotNull CheckSpec that) {
        final CheckSpec composite = baseCompositeOver(that);
        composite.setSkip(this.isSkip() || that.isSkip());
        return composite;
    }

    /**
     * Return true if this spec identifies that spec by name in this spec's template attribute.
     *
     * @param that the other spec
     * @return true if this inherits from that
     */
    public final boolean inherits(final @NotNull CheckSpec that) {
        return this.mustInherit() && !that.mustInherit()
                && !this.getTemplate().equals(this.getName())
                && that.notUnnamed()
                && (that.getName().equals(this.getTemplate())
                || that.getName().endsWith("/" + this.getTemplate()));
    }

    /**
     * The inverse of {@link #inherits(CheckSpec)}.
     *
     * @param that the other check spec
     * @return true if this is inherited by that
     */
    public final boolean isInheritedBy(final @NotNull CheckSpec that) {
        return that.inherits(this);
    }

    /**
     * Apply this spec's attributes to that as an overlay to that spec's attributes. Recommend checking for a true from
     * {@link #inherits(CheckSpec)}. This spec's name and skip attributes takes precedence over that's.
     *
     * @param that the other spec to inherit from
     * @return the composite check spec
     */
    public final CheckSpec inherit(final @NotNull CheckSpec that) {
        final CheckSpec composite = baseCompositeOver(that);
        composite.setName(ofNullable(this.getName()).orElse(that.getName()));
        composite.setSkip(this.isSkip());
        return composite;
    }

    /**
     * Merge an overlay json object's entries into a base json object, replacing values
     * for duplicate keys.
     *
     * @param base    the base json object
     * @param overlay the overlay json object
     * @return a merged json object
     */
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
    public static CheckSpec fromJson(final @NotNull JsonObject json) {
        final JsonKeys keys = keys();
        final CheckSpec checkSpec = new CheckSpec();
        if (hasNonNull(json, keys.impl())) {
            checkSpec.setImpl(json.getString(keys.impl()));
        }
        if (hasNonNull(json, keys.inlineScript())) {
            checkSpec.setInlineScript(json.getString(keys.inlineScript()));
        }
        if (hasNonNull(json, keys.inlineEngine())) {
            checkSpec.setInlineEngine(json.getString(keys.inlineEngine()));
        }
        if (hasNonNull(json, keys.name())) {
            checkSpec.setName(json.getString(keys.name()));
        }
        if (hasNonNull(json, keys.template())) {
            checkSpec.setTemplate(json.getString(keys.template()));
        }
        if (hasNonNull(json, keys.skip())) {
            checkSpec.setSkip(json.getBoolean(keys.skip()));
        }
        if (hasNonNull(json, keys.config())) {
            checkSpec.setConfig(json.getJsonObject(keys.config()));
        }

        return checkSpec;
    }

    /**
     * Override to ensure subtype details are retained in JSON.
     *
     * @param builder the json object builder that should be edited by subclasses
     */
    @SuppressWarnings("UnusedParameter")
    protected void editJson(final JsonObjectBuilder builder) {
        // for overriding classes.
    }

    @Override
    public final JsonObject toJson() {
        final JsonKeys keys = keys();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final JavaxJson.Obj obj = obj()
                .key(keys.name()).opt(getName())
                .key(keys.impl()).opt(getImpl())
                .key(keys.inlineScript()).opt(getInlineScript())
                .key(keys.inlineEngine()).opt(getInlineEngine())
                .key(keys.config()).opt(getConfig())
                .key(keys.template()).opt(getTemplate());
        if (isSkip()) {
            obj.key(keys.skip(), true);
        }
        final JsonObject base = obj.get();
        base.forEach(builder::add);
        editJson(builder);
        return builder.build();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckSpec)) return false;
        CheckSpec checkSpec = (CheckSpec) o;
        return isSkip() == checkSpec.isSkip() &&
                Objects.equals(getImpl(), checkSpec.getImpl()) &&
                Objects.equals(getInlineScript(), checkSpec.getInlineScript()) &&
                Objects.equals(getInlineEngine(), checkSpec.getInlineEngine()) &&
                Objects.equals(getName(), checkSpec.getName()) &&
                Objects.equals(getTemplate(), checkSpec.getTemplate()) &&
                Objects.equals(getConfig(), checkSpec.getConfig());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getImpl(), getInlineScript(), getInlineEngine(), getName(), getTemplate(), isSkip(), getConfig());
    }

    public static CheckSpec copyOf(final @NotNull CheckSpec original) {
        final CheckSpec copy = new CheckSpec();
        copy.setName(original.getName());
        copy.setImpl(original.getImpl());
        copy.setTemplate(original.getTemplate());
        copy.setSkip(original.isSkip());
        copy.setInlineScript(original.getInlineScript());
        copy.setInlineEngine(original.getInlineEngine());
        copy.setConfig(original.getConfig());
        return copy;
    }

    public static ImmutableSpec immutableCopyOf(final @NotNull CheckSpec original) {
        return new ImmutableSpec(
                original.getName(),
                original.getImpl(),
                original.getTemplate(),
                original.getInlineScript(),
                original.getInlineEngine(),
                original.getConfig(),
                original.isSkip());
    }


    /**
     * This is an Immutable variant of {@link CheckSpec} for composition in {@link Checklist}.
     */
    public static final class ImmutableSpec extends CheckSpec {
        private ImmutableSpec(
                final @Nullable String name,
                final @Nullable String impl,
                final @Nullable String template,
                final @Nullable String inlineScript,
                final @Nullable String inlineEngine,
                final @Nullable JsonObject config,
                final boolean skip) {
            super();
            super.setName(name);
            super.setImpl(impl);
            super.setTemplate(template);
            super.setInlineScript(inlineScript);
            super.setInlineEngine(inlineEngine);
            super.setConfig(config);
            super.setSkip(skip);
        }

        @Override
        public void setImpl(final String impl) {
            throw new UnsupportedOperationException("this CheckSpec is immutable.");
        }

        @Override
        public void setInlineScript(final String inlineScript) {
            throw new UnsupportedOperationException("this CheckSpec is immutable.");
        }

        @Override
        public void setInlineEngine(final String inlineEngine) {
            throw new UnsupportedOperationException("this CheckSpec is immutable.");
        }

        @Override
        public void setName(final String name) {
            throw new UnsupportedOperationException("this CheckSpec is immutable.");
        }

        @Override
        public void setTemplate(final String template) {
            throw new UnsupportedOperationException("this CheckSpec is immutable.");
        }

        @Override
        public void setSkip(final boolean skip) {
            throw new UnsupportedOperationException("this CheckSpec is immutable.");
        }

        @Override
        public void setConfig(final JsonObject config) {
            throw new UnsupportedOperationException("this CheckSpec is immutable.");
        }
    }
}
