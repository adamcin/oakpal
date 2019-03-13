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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import net.adamcin.oakpal.core.Checklist;
import net.adamcin.oakpal.core.ChecklistPlanner;
import net.adamcin.oakpal.interactive.ChecklistTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background service that keeps track of checklists exported by active bundles.
 */
@Component
class ChecklistTrackerImpl implements ChecklistTracker, BundleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChecklistTrackerImpl.class);

    /**
     * Checks aren't loaded in the context of SCR, so what we care about are bundles that are at least in Resolved state.
     */
    private static final List<Integer> DISCOVERABLE_STATES =
            Arrays.asList(Bundle.ACTIVE, Bundle.STARTING, Bundle.STOPPING, Bundle.RESOLVED);

    private final Map<Long, List<Checklist>> checklists = Collections.synchronizedMap(new HashMap<>());

    /**
     * Trigger a one-off thread to avoid blocking bundle activation.
     *
     * @param ctx the bundle context
     */
    @Activate
    protected void activate(final BundleContext ctx) {
        new Thread(() -> Arrays.stream(ctx.getBundles()).forEachOrdered(this::discoverChecklists)).start();
    }

    /**
     * Clear the discovered state on deactivation.
     */
    @Deactivate
    protected void deactivate() {
        this.checklists.clear();
    }

    /**
     * Determines whether the bundle should be discovered or undiscovered.
     *
     * @param bundle the bundle to inspect
     * @return true if bundle should be inspected, false if it should be uninspected
     */
    static boolean isDiscoverable(final Bundle bundle) {
        return DISCOVERABLE_STATES.contains(bundle.getState());
    }

    /**
     * Inspect the specified bundle for checklists and register them.
     *
     * @param bundle the bundle
     */
    void discoverChecklists(final Bundle bundle) {
        // only look into the bundle when it has an Oakpal-Checklist manifest header
        if (bundle.getHeaders().get(ChecklistPlanner.OAKPAL_CHECKLIST) != null) {
            // check that the bundle state is at least resolved
            if (isDiscoverable(bundle)) {
                try {
                    // get the manifest url to parse using the API
                    URL manifestUrl = bundle.getResource(JarFile.MANIFEST_NAME);
                    if (manifestUrl != null) {
                        List<Checklist> parsed = ChecklistPlanner
                                .constructChecklists(ChecklistPlanner.parseChecklists(manifestUrl));
                        checklists.put(bundle.getBundleId(), parsed);
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("[discoverChecklists] found checklists for bundle {}: {}",
                                    bundle.getSymbolicName(),
                                    parsed.stream().map(Checklist::getName).collect(Collectors.toList()));
                        }
                    }
                } catch (final Exception e) {
                    LOGGER.error("[discoverChecklists] Error occurred while discovering checklists from bundle: "
                            + bundle.getSymbolicName(), e);
                    undiscoverChecklists(bundle);
                }
            } else {
                undiscoverChecklists(bundle);
            }
        }
    }

    /**
     * Deregister discovered checklists for the specified bundle
     *
     * @param bundle the bundle
     */
    private void undiscoverChecklists(final Bundle bundle) {
        checklists.remove(bundle.getBundleId());
    }

    /**
     * BundleListener: Handle bundleChanged events.
     *
     * @param event the event to handle
     */
    @Override
    public void bundleChanged(final BundleEvent event) {
        discoverChecklists(event.getBundle());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Checklist> getBundleChecklists() {
        List<Checklist> list = new ArrayList<>();
        for (List<Checklist> group : checklists.values()) {
            list.addAll(group);
        }
        return Collections.unmodifiableList(list);
    }
}
