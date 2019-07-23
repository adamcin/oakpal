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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discoverer of checklists and computer of checkSpecs.
 */
public final class ChecklistPlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChecklistPlanner.class);
    public static final String OAKPAL_CHECKLIST = "Oakpal-Checklist";
    public static final String OAKPAL_MODULENAME = "Oakpal-ModuleName";
    public static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
    public static final String AUTOMATIC_MODULE_NAME = "Automatic-Module-Name";

    private final Set<String> activeChecklistIds = new LinkedHashSet<>();
    private final List<Checklist> checklists = new ArrayList<>();
    private final List<Checklist> inactiveChecklists = new ArrayList<>();

    public ChecklistPlanner(final List<String> activeChecklistIds) {
        this.activeChecklistIds.addAll(activeChecklistIds);
    }

    public void provideChecklists(final List<Checklist> constructed) {
        selectChecklists(new ArrayList<>(constructed));
    }

    public void discoverChecklists() {
        discoverChecklists(Util.getDefaultClassLoader());
    }

    public void discoverChecklists(final ClassLoader classLoader) {
        try {
            Map<URL, List<JsonObject>> parsed = parseChecklists(classLoader);
            selectChecklists(constructChecklists(parsed));
        } catch (final Exception e) {
            LOGGER.debug("[discoverChecklists(ClassLoader)] error occurred during discovery", e);
        }
    }

    public void discoverChecklists(final List<File> files) {
        try {
            Map<URL, List<JsonObject>> parsed = parseChecklists(files);
            selectChecklists(constructChecklists(parsed));
        } catch (final Exception e) {
            LOGGER.debug("[discoverChecklists(List<File>)] error occurred during discovery", e);
        }
    }

    private void selectChecklists(final List<Checklist> constructed) {
        for (String activeId : activeChecklistIds) {
            if (activeId.isEmpty()) {
                continue;
            }
            List<Checklist> selected = constructed.stream()
                    .filter(checklist -> isChecklistSelected(activeId, checklist)).collect(Collectors.toList());
            checklists.addAll(selected);
            constructed.removeAll(selected);
        }
        inactiveChecklists.addAll(constructed);
    }

    private boolean isChecklistSelected(final String activeId, final Checklist checklist) {
        return activeId.matches(checklist.getName())
                || activeId.matches(checklist.getModuleName() + "/" + checklist.getName());
    }

    public List<InitStage> getInitStages() {
        return getSelectedChecklists().map(Checklist::asInitStage).collect(Collectors.toList());
    }

    Stream<Checklist> getSelectedChecklists() {
        return checklists.stream();
    }

    Stream<Checklist> getAllChecklists() {
        List<Checklist> allChecklists = new ArrayList<>(checklists);
        allChecklists.addAll(inactiveChecklists);
        return allChecklists.stream();
    }

    /**
     * Compute the checklist plan for progress checks, given a list of override specs to apply.
     *
     * Intended Algorithm:
     * from selected checklists, stream specs
     *   -> apply overrides
     *   -> drop skipped specs
     *   -> collect overlaid
     * from overrides, stream as remaining specs
     *   -> drop skipped specs
     *   -> drop if already applied as overlay
     *   -> apply as overlay to inactive checklist specs if possible
     *   -> apply inheritance from any checklist spec if necessary
     *   -> add result if not abstract
     *
     * @param checkOverrides the list of check overrides
     * @return a final list of {@link CheckSpec}s to use for a scan
     * @see Locator#loadFromCheckSpecs(List)
     * @see OakMachine.Builder#withProgressChecks(List)
     */
    public List<CheckSpec> getEffectiveCheckSpecs(final List<CheckSpec> checkOverrides) {
        Map<String, CheckSpec> overlaid = new LinkedHashMap<>();
        List<CheckSpec> overrides = new ArrayList<>();

        if (checkOverrides != null) {
            overrides.addAll(checkOverrides);
        }

        LOGGER.trace("[getEffectiveCheckSpecs] checkOverrides: {}", checkOverrides);

        // first accum checks selected via selected checklists
        getSelectedChecklists()
                // debug filter
                .filter(Util.traceFilter(LOGGER, "[getEffectiveCheckSpecs] selected checklist: {}"))
                // Stream<Checklist> -> Stream<CheckSpec>
                .flatMap(checklist -> checklist.getChecks().stream())
                // apply overrides to each base spec
                .map(base -> applyOverrides(overrides, base))
                // evaluate skip after override
                .filter(CheckSpec::notSkipped)
                // only accum once
                .forEachOrdered(checkSpec -> {
                    if (!overlaid.containsKey(checkSpec.getName())) {
                        overlaid.put(checkSpec.getName(), checkSpec);
                    }
                });

        List<CheckSpec> toReturn = new ArrayList<>(overlaid.values());

        LOGGER.trace("[getEffectiveCheckSpecs] selectedChecklistChecks: {}", toReturn);

        // stream overrides to identify remaining specs
        overrides.stream()
                // filter out skipped overrides
                .filter(CheckSpec::notSkipped)
                // filter out overrides that already applied
                .filter(spec -> overlaid.values().stream().noneMatch(spec::overrides))
                // find first base spec to override among inactive checklists
                .forEachOrdered(spec -> {
                    CheckSpec merged = inactiveChecklists.stream()
                            // Stream<Checklist> -> Stream<CheckSpec>
                            .flatMap(checklist -> checklist.getChecks().stream())
                            // select base specs that each override spec overrides
                            .filter(spec::overrides)
                            // take optional head and overlay with override, or return override itself
                            .findFirst()
                            .map(spec::overlay)
                            .filter(CheckSpec::notSkipped)
                            .orElse(spec);

                    if (merged.mustInherit()) {
                        CheckSpec extended = getAllChecklists()
                                .flatMap(checklist -> checklist.getChecks().stream())
                                .filter(merged::inherits)
                                .findFirst().map(merged::inherit).orElse(merged);
                        // if extended spec has impl, add it
                        if (extended.notAbstract()) {
                            toReturn.add(extended);
                        }
                    } else {
                        // if merged spec has impl, add it
                        if (merged.notAbstract()) {
                            toReturn.add(merged);
                        }
                    }
                });

        LOGGER.trace("[getEffectiveCheckSpecs] effective check specs: {}", toReturn);
        return toReturn;
    }

    static CheckSpec applyOverrides(final List<CheckSpec> checkOverrides, final CheckSpec base) {
        CheckSpec merged = base;
        LOGGER.trace("[applyOverrides] base: {}", base);
        List<CheckSpec> applicable = checkOverrides.stream().filter(base::isOverriddenBy)
                .collect(Collectors.toList());
        for (CheckSpec spec : applicable) {
            merged = spec.overlay(merged);
            LOGGER.trace("[applyOverrides] spec: {}, merged: {}", spec, merged);
        }
        return merged;
    }

    public static List<Checklist> constructChecklists(final Map<URL, List<JsonObject>> parsed) throws Exception {
        List<Checklist> checklists = new ArrayList<>();
        for (Map.Entry<URL, List<JsonObject>> entry : parsed.entrySet()) {
            final URL manifestUrl = entry.getKey();
            final String moduleName = bestModuleName(manifestUrl);
            entry.getValue().forEach(json -> checklists.add(Checklist.fromJson(moduleName, manifestUrl, json)));
        }
        return checklists;
    }

    public static Map<URL, List<JsonObject>> parseChecklists(final URL manifestUrl) throws Exception {
        Map<URL, List<URL>> manifestLookup = Util.mapManifestHeaderResources(OAKPAL_CHECKLIST, manifestUrl);
        return parseChecklists(manifestLookup);
    }

    public static Map<URL, List<JsonObject>> parseChecklists(final ClassLoader classLoader) throws Exception {
        Map<URL, List<URL>> manifestLookup = Util.mapManifestHeaderResources(OAKPAL_CHECKLIST, classLoader);
        return parseChecklists(manifestLookup);
    }

    public static Map<URL, List<JsonObject>> parseChecklists(final List<File> files) throws Exception {
        Map<URL, List<URL>> manifestLookup = Util.mapManifestHeaderResources(OAKPAL_CHECKLIST, files);
        return parseChecklists(manifestLookup);
    }

    static Map<URL, List<JsonObject>> parseChecklists(final Map<URL, List<URL>> manifestLookup)
            throws Exception {
        Map<URL, List<JsonObject>> parsed = new LinkedHashMap<>();
        for (Map.Entry<URL, List<URL>> manifestEntry : manifestLookup.entrySet()) {
            final URL manifestUrl = manifestEntry.getKey();
            final List<URL> checklistUrls = manifestEntry.getValue();
            final List<JsonObject> allParsedForModule = new ArrayList<>();
            parsed.put(manifestUrl, allParsedForModule);
            for (URL checklistUrl : checklistUrls) {
                try (InputStream is = checklistUrl.openStream();
                     JsonReader reader = Json.createReader(is)) {
                    allParsedForModule.add(reader.readObject());
                }
            }
        }
        return parsed;
    }

    static String bestModuleName(final URL manifestUrl) throws Exception {
        try (InputStream is = manifestUrl.openStream()) {
            final Manifest manifest = new Manifest(is);
            List<String> omns = Util.getManifestHeaderValues(manifest, OAKPAL_MODULENAME);
            if (!omns.isEmpty()) {
                return omns.get(0);
            }
            List<String> bsns = Util.getManifestHeaderValues(manifest, BUNDLE_SYMBOLICNAME);
            if (!bsns.isEmpty()) {
                return bsns.get(0);
            }
            List<String> amns = Util.getManifestHeaderValues(manifest, AUTOMATIC_MODULE_NAME);
            if (!amns.isEmpty()) {
                return amns.get(0);
            }
        }
        return "";
    }
}
