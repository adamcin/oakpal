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

import net.adamcin.oakpal.core.Checklist;
import net.adamcin.oakpal.core.ChecklistPlanner;
import net.adamcin.oakpal.interactive.ChecklistTracker;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ChecklistTrackerImpl implements ChecklistTracker, BundleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChecklistTrackerImpl.class);

    private final Map<String, List<Checklist>> checklists = new HashMap<>();

    @Reference
    private ConfigurationResourceResolver configurationResourceResolver;

    @Activate
    protected void activate(final BundleContext ctx) {
        Arrays.stream(ctx.getBundles()).filter(this::isResolved).forEachOrdered(this::discoverChecklists);
    }

    @Deactivate
    protected void deactivate() {
        this.checklists.clear();
    }

    private boolean isResolved(final Bundle bundle) {
        switch (bundle.getState()) {
            case Bundle.ACTIVE:
            case Bundle.STARTING:
            case Bundle.RESOLVED:
                return true;
            default:
                return false;
        }
    }

    private void discoverChecklists(final Bundle bundle) {
        synchronized (checklists) {
            if (bundle.getHeaders().get(ChecklistPlanner.OAKPAL_CHECKLIST) != null) {
                try {
                    URL manifestUrl = bundle.getResource(JarFile.MANIFEST_NAME);
                    if (manifestUrl != null) {
                        List<Checklist> parsed = ChecklistPlanner
                                .constructChecklists(ChecklistPlanner.parseChecklists(manifestUrl));
                        LOGGER.info("[discoverChecklists] found checklists for bundle {}",
                                bundle.getSymbolicName(), parsed);
                        checklists.put(bundle.getSymbolicName(), parsed);
                    }
                } catch (final Exception e) {
                    LOGGER.error("Error occurred while discovering checklists from bundle: " + bundle.getSymbolicName(), e);
                }
            }
        }
    }

    private void undiscoverChecklists(final Bundle bundle) {
        synchronized (checklists) {
            checklists.remove(bundle.getSymbolicName());
        }
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.RESOLVED:
                discoverChecklists(event.getBundle());
            case BundleEvent.UNRESOLVED:
                undiscoverChecklists(event.getBundle());
            default:
                // do nothing
        }
    }

    @Override
    public List<Checklist> getBundleChecklists() {
        List<Checklist> list = new ArrayList<>();
        synchronized (checklists) {
            for (List<Checklist> group : checklists.values()) {
                list.addAll(group);
            }
        }
        return Collections.unmodifiableList(list);
    }
}
