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

import org.json.JSONObject;

/**
 * Config DTO for JCR Namespace Prefix to URI Mappings.
 */
public final class JcrNs {
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
    static JcrNs fromJSON(final JSONObject json) {
        JcrNs jcrNs = new JcrNs();
        jcrNs.setPrefix(json.getString(KEY_PREFIX));
        jcrNs.setUri(json.getString(KEY_URI));
        return jcrNs;
    }

    @Override
    public String toString() {
        JSONObject ret = new JSONObject();
        ret.put(KEY_PREFIX, getPrefix());
        ret.put(KEY_URI, getUri());
        return ret.toString();
    }
}
