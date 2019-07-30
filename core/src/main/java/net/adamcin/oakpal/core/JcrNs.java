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

import static net.adamcin.oakpal.core.JavaxJson.key;

import java.util.Objects;
import java.util.Optional;
import javax.json.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Config DTO for JCR Namespace Prefix to URI Mappings.
 */
public final class JcrNs implements JavaxJson.ObjectConvertible, Comparable<JcrNs> {
    static final String KEY_PREFIX = "prefix";
    static final String KEY_URI = "uri";

    private String prefix;
    private String uri;

    /**
     * The namespace prefix.
     *
     * @return the namespace prefix
     */
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * The namespace URI.
     *
     * @return the namespace URI
     */
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Map a JSON object to a {@link JcrNs}.
     *
     * @param json JSON object
     * @return a new JCR NS mapping
     */
    public static @Nullable JcrNs fromJson(final @NotNull JsonObject json) {
        if (!json.containsKey(KEY_PREFIX) || !json.containsKey(KEY_URI)) {
            return null;
        }
        JcrNs jcrNs = new JcrNs();
        jcrNs.setPrefix(json.getString(KEY_PREFIX, ""));
        jcrNs.setUri(json.getString(KEY_URI, ""));
        return jcrNs;
    }

    /**
     * Create a new JcrNs with both values set.
     *
     * @param prefix the namespace prefix
     * @param uri    the namespace uri
     * @return a new JCR namespace mapping
     */
    public static @NotNull JcrNs create(final @NotNull String prefix, final @NotNull String uri) {
        final JcrNs ns = new JcrNs();
        ns.setPrefix(prefix);
        ns.setUri(uri);
        return ns;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof JcrNs)) return false;
        JcrNs jcrNs = (JcrNs) o;
        return Objects.equals(getPrefix(), jcrNs.getPrefix()) &&
                Objects.equals(getUri(), jcrNs.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPrefix(), getUri());
    }

    @Override
    public JsonObject toJson() {
        return key(KEY_PREFIX, getPrefix()).key(KEY_URI, getUri()).get();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public int compareTo(final @NotNull JcrNs o) {
        return Optional.of(getPrefix().compareTo(o.getPrefix()))
                .filter(comp -> comp != 0).orElseGet(() -> getUri().compareTo(o.getUri()));
    }
}
