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

package net.adamcin.oakpal.webster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.oak.run.cli.NodeStoreFixture;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.FileArchive;
import org.apache.jackrabbit.vault.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebsterPlan {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsterPlan.class);

    private static final FixtureProvider DEFAULT_FIXTURE_PROVIDER = new FixtureProvider() {
        @Override
        public NodeStoreFixture openFixture() throws Exception {
            throw new IllegalStateException("This Webster Plan only supports archive validation.");
        }
    };

    @FunctionalInterface
    public interface FixtureProvider {
        NodeStoreFixture openFixture() throws Exception;
    }

    private final FixtureProvider fixtureProvider;
    private final File globalSegmentStore;
    private final List<WebsterTarget> targets;
    private final File archiveRoot;

    public static final class Builder {
        private FixtureProvider fixtureProvider = DEFAULT_FIXTURE_PROVIDER;
        private File globalSegmentStore;
        private File archiveRoot;
        private final List<WebsterTarget> targets = new ArrayList<>();

        public Builder withFixtureProvider(@NotNull FixtureProvider fixtureProvider) {
            this.fixtureProvider = fixtureProvider;
            return this;
        }

        public Builder withGlobalSegmentStore(@NotNull File globalSegmentStore) {
            this.globalSegmentStore = globalSegmentStore;
            return this;
        }

        public Builder withGlobalMemoryStore() {
            this.globalSegmentStore = null;
            return this;
        }

        public Builder withArchiveRoot(@NotNull File archiveRoot) {
            this.archiveRoot = archiveRoot;
            return this;
        }

        public Builder withTarget(@NotNull WebsterTarget action) {
            this.targets.add(action);
            return this;
        }

        public Builder withTargets(@NotNull WebsterTarget... targets) {
            this.targets.addAll(Arrays.asList(targets));
            return this;
        }

        public Builder withTargets(@NotNull List<WebsterTarget> targets) {
            this.targets.addAll(targets);
            return this;
        }

        public WebsterPlan build() {
            return new WebsterPlan(fixtureProvider, targets, globalSegmentStore, archiveRoot);
        }
    }

    WebsterPlan(@NotNull final FixtureProvider fixtureProvider,
                @NotNull final List<WebsterTarget> targets,
                @Nullable final File globalSegmentStore,
                @Nullable final File archiveRoot) {
        this.fixtureProvider = fixtureProvider;
        this.targets = new ArrayList<>(targets);
        this.globalSegmentStore = globalSegmentStore;
        this.archiveRoot = archiveRoot;
    }

    private void performWithGlobalSegment() throws Exception {
        LOGGER.info("Webster Plan: Performing plan with SegmentTar global nodestore...");
        try (NodeStoreFixture fixture = fixtureProvider.openFixture();
             NodeStoreFixture globalFixture = JcrFactory.getReadWriteFixture(this.globalSegmentStore)) {
            Repository repo = null;
            Session session = null;
            try {
                repo = JcrFactory.getJcr(fixture, globalFixture.getStore());
                session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
                internalPerform(session);
            } finally {
                if (session != null) {
                    session.logout();
                }
                if (repo instanceof JackrabbitRepository) {
                    ((JackrabbitRepository) repo).shutdown();
                }
            }
        }
    }

    private void performWithGlobalMemory() throws Exception {
        LOGGER.info("Webster Plan: Performing plan with in-memory global nodestore...");
        try (NodeStoreFixture fixture = fixtureProvider.openFixture()) {
            Repository repo = null;
            Session session = null;
            try {
                repo = JcrFactory.getJcr(fixture);
                session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
                internalPerform(session);
            } finally {
                if (session != null) {
                    session.logout();
                }
                if (repo instanceof JackrabbitRepository) {
                    ((JackrabbitRepository) repo).shutdown();
                }
            }
        }
    }

    void internalPerform(final Session session) throws Exception {
        for (WebsterTarget target : targets) {
            if (target instanceof ArchiveAware) {
                ((ArchiveAware) target).setArchive(createArchive(), archiveRoot);
            }
            target.perform(session);
        }
    }

    Archive createArchive() throws IOException {
        if (archiveRoot == null || !archiveRoot.isDirectory()) {
            throw new IllegalArgumentException("archiveRoot must be an existing directory");
        }

        final FileArchive archive = new FileArchive(archiveRoot);
        if (!new File(archiveRoot, Constants.ROOT_DIR).exists()) {
            throw new IllegalArgumentException("archiveRoot must have a child jcr_root directory");
        }

        final File archiveMetaInfVault = new File(archiveRoot, Constants.META_DIR);
        if (!archiveMetaInfVault.isDirectory()) {
            archiveMetaInfVault.mkdirs();
        }

        archive.open(false);

        return archive;
    }

    public void perform() throws Exception {
        if (this.globalSegmentStore == null) {
            this.performWithGlobalMemory();
        } else {
            this.performWithGlobalSegment();
        }
    }
}
