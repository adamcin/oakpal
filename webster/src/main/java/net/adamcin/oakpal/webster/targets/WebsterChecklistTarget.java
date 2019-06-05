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

import static net.adamcin.oakpal.core.JavaxJson.obj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.jcr.Session;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.adamcin.oakpal.core.Checklist;
import net.adamcin.oakpal.core.JavaxJson;
import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.checks.Rule;
import net.adamcin.oakpal.webster.ChecklistExporter;
import net.adamcin.oakpal.webster.WebsterTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WebsterChecklistTarget implements WebsterTarget {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsterChecklistTarget.class);

    static final String KEY_TYPE = "type";
    static final String KEY_ARGS = "args";
    static final String COMPACT_KEY_BY_NODETYPES = "selectNodeTypes";
    static final String COMPACT_KEY_BY_PATHS = "selectPaths";
    static final String COMPACT_KEY_BY_QUERY = "selectQuery";
    static final String KEY_SELECTORS = "selectors";
    static final String KEY_SCOPE_PATHS = "scopePaths";
    static final String KEY_SCOPE_NODE_TYPES = "scopeNodeTypes";
    static final String KEY_JCR_NAMESPACES = "jcrNamespaces";
    static final String KEY_UPDATE_POLICY = "updatePolicy";
    static final String KEY_EXPORT_NODETYPES = "exportNodeTypes";

    static class Selector {
        private final ChecklistExporter.SelectorType type;
        private final String[] args;

        Selector(final ChecklistExporter.SelectorType type, final String[] args) {
            LOGGER.debug("[Selector] type={}, args={}", type, Arrays.toString(args));
            this.type = type;
            this.args = args;
        }

        void selectOnExporter(ChecklistExporter.Builder exporter) {
            switch (type) {
                case PATH:
                    exporter.byPath(args);
                    break;
                case NODETYPE:
                    exporter.byNodeType(args);
                    break;
                case QUERY:
                default:
                    exporter.byQuery(args[0]);
                    break;
            }
        }
    }

    static Selector selectorFromJson(final JsonObject json) {
        ChecklistExporter.SelectorType type = ChecklistExporter.SelectorType.byName(json.getString(KEY_TYPE));
        String[] args = JavaxJson.mapArrayOfStrings(json.getJsonArray(KEY_ARGS)).toArray(new String[0]);
        return new Selector(type, args);
    }

    static List<Selector> selectorsFromConfigCompactForm(final JsonObject json) {
        final List<Selector> selectors = new ArrayList<>();
        if (json.containsKey(COMPACT_KEY_BY_NODETYPES)) {
            final List<String> args = JavaxJson.mapArrayOfStrings(json.getJsonArray(COMPACT_KEY_BY_NODETYPES));
            selectors.add(new Selector(ChecklistExporter.SelectorType.NODETYPE, args.toArray(new String[0])));
        }
        if (json.containsKey(COMPACT_KEY_BY_PATHS)) {
            final List<String> args = JavaxJson.mapArrayOfStrings(json.getJsonArray(COMPACT_KEY_BY_PATHS));
            selectors.add(new Selector(ChecklistExporter.SelectorType.PATH, args.toArray(new String[0])));
        }
        if (json.containsKey(COMPACT_KEY_BY_QUERY)) {
            selectors.add(new Selector(ChecklistExporter.SelectorType.QUERY,
                    new String[]{json.getString(COMPACT_KEY_BY_QUERY)}));
        }
        return selectors;
    }

    private final File checklist;
    private final ChecklistExporter exporter;
    private final ChecklistExporter.ForcedRootUpdatePolicy updatePolicy;

    private WebsterChecklistTarget(final File checklist,
                                   final ChecklistExporter exporter,
                                   final ChecklistExporter.ForcedRootUpdatePolicy updatePolicy) {
        this.checklist = checklist;
        this.exporter = exporter;
        this.updatePolicy = updatePolicy;
    }

    @Override
    public void perform(final Session session) throws Exception {
        final JsonObject json;
        if (!checklist.exists()) {
            checklist.getParentFile().mkdirs();
            json = obj().get();
        } else {
            try (Reader reader = new InputStreamReader(new FileInputStream(checklist), StandardCharsets.UTF_8);
                 JsonReader jsonReader = Json.createReader(reader)) {
                json = jsonReader.readObject();
            }
        }

        exporter.updateChecklist(() -> new OutputStreamWriter(
                        new FileOutputStream(checklist), StandardCharsets.UTF_8),
                session, Checklist.fromJson("webster-temp", null, json), updatePolicy);

        LOGGER.info("Checklist JSON written to {}", checklist.getAbsolutePath());
    }

    static WebsterChecklistTarget fromJson(final File target, final JsonObject config) {
        LOGGER.debug("[fromJson] fromJson: {}", config.toString());
        ChecklistExporter.Builder exporter = new ChecklistExporter.Builder();
        WebsterChecklistTarget.selectorsFromConfigCompactForm(config)
                .forEach(selector -> selector.selectOnExporter(exporter));
        if (config.containsKey(KEY_SELECTORS)) {
            JsonArray ops = config.getJsonArray(KEY_SELECTORS);
            List<Selector> selectors = JavaxJson.mapArrayOfObjects(ops, WebsterChecklistTarget::selectorFromJson);
            selectors.forEach(selector -> selector.selectOnExporter(exporter));
        }
        if (config.containsKey(KEY_SCOPE_PATHS)) {
            List<Rule> scopePaths = Rule.fromJsonArray(config.getJsonArray(KEY_SCOPE_PATHS));
            exporter.withScopePaths(scopePaths);
        }
        if (config.containsKey(KEY_SCOPE_NODE_TYPES)) {
            List<Rule> scopeNodeTypes = Rule.fromJsonArray(config.getJsonArray(KEY_SCOPE_NODE_TYPES));
            exporter.withScopeNodeTypes(scopeNodeTypes);
        }
        if (config.containsKey(KEY_JCR_NAMESPACES)) {
            List<JcrNs> jcrNsList = JavaxJson.mapArrayOfObjects(config.getJsonArray(KEY_JCR_NAMESPACES),
                    JcrNs::fromJson);
            exporter.withJcrNamespaces(jcrNsList);
        }
        if (config.containsKey(KEY_EXPORT_NODETYPES)) {
            List<String> ntList = JavaxJson.mapArrayOfStrings(config.getJsonArray(KEY_EXPORT_NODETYPES));
            exporter.withExportNodeTypes(ntList);
        }
        ChecklistExporter.ForcedRootUpdatePolicy updatePolicy = null;
        if (config.containsKey(KEY_UPDATE_POLICY)) {
            updatePolicy = ChecklistExporter.ForcedRootUpdatePolicy.byName(config.getString(KEY_UPDATE_POLICY));
        }
        return new WebsterChecklistTarget(target, exporter.build(), updatePolicy);
    }
}
