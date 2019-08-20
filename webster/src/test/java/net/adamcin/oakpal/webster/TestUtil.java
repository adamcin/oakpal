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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.security.Privilege;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.core.Result;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.run.cli.FileStoreTarBuilderCustomizer;
import org.apache.jackrabbit.oak.run.cli.NodeStoreFixture;
import org.apache.jackrabbit.oak.run.cli.NodeStoreFixtureProvider;
import org.apache.jackrabbit.oak.run.cli.Options;
import org.apache.jackrabbit.oak.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders;
import org.apache.jackrabbit.oak.segment.file.FileStore;
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionReader;

import static net.adamcin.oakpal.core.Fun.result1;
import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.Fun.zipKeysWithValueFunc;

public final class TestUtil {
    private TestUtil() {
        // no instantiation
    }

    public static void closeRepo(final Repository repo) {
        if (repo instanceof JackrabbitRepository) {
            ((JackrabbitRepository) repo).shutdown();
        }
    }

    public static void withInMemoryRepo(final SessionStrategy sessionStrategy) throws Exception {
        final Repository repo = new Jcr(new Oak(), true).createRepository();
        Session session = null;

        try {
            session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
            sessionStrategy.accept(session);
        } finally {
            if (session != null) {
                session.logout();
            }
            closeRepo(repo);
        }
    }

    public static void prepareRepo(final File repoDir, final SessionStrategy sessionStrategy) throws Exception {
        FileUtils.deleteDirectory(repoDir);
        repoDir.mkdirs();

        final FileStore fs = FileStoreBuilder.fileStoreBuilder(repoDir).withMaxFileSize(256).build();
        final SegmentNodeStore ns = SegmentNodeStoreBuilders.builder(fs).build();
        final Repository repo = new Jcr(new Oak(ns), true).createRepository();
        Session session = null;

        try {
            session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
            sessionStrategy.accept(session);
        } finally {
            if (session != null) {
                session.logout();
            }
            closeRepo(repo);
            fs.close();
        }
    }

    public static NodeStoreFixture getReadWriteFixture(final File segmentStore,
                                                       final FileStoreTarBuilderCustomizer builderCustomizer) throws Exception {
        OptionParser parser = new OptionParser();
        Options opts = new Options();

        OptionSet options = opts.parseAndConfigure(parser, new String[]{segmentStore.getAbsolutePath()});
        if (builderCustomizer != null) {
            opts.getWhiteboard().register(FileStoreTarBuilderCustomizer.class,
                    builderCustomizer, Collections.emptyMap());
        }

        return NodeStoreFixtureProvider.create(opts, false);
    }

    public static NodeStoreFixture getReadOnlyFixture(final File segmentStore,
                                                      final FileStoreTarBuilderCustomizer builderCustomizer)
            throws Exception {

        OptionParser parser = new OptionParser();
        Options opts = new Options();

        OptionSet options = opts.parseAndConfigure(parser, new String[]{segmentStore.getAbsolutePath()});
        if (builderCustomizer != null) {
            opts.getWhiteboard().register(FileStoreTarBuilderCustomizer.class,
                    builderCustomizer, Collections.emptyMap());
        }

        return NodeStoreFixtureProvider.create(opts, true);
    }

    public static void withReadOnlyFixture(final File segmentStore, final File globalStore,
                                           final SessionStrategy sessionStrategy)
            throws Exception {

        try (NodeStoreFixture fixture = TestUtil.getReadOnlyFixture(segmentStore, null);
             NodeStoreFixture globalFixture = TestUtil.getReadWriteFixture(globalStore, null)) {

            Repository repo = null;
            Session session = null;
            try {
                repo = JcrFactory.getJcr(fixture, globalFixture.getStore());
                session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
                sessionStrategy.accept(session);
            } finally {
                if (session != null) {
                    session.logout();
                }
                TestUtil.closeRepo(repo);
            }
        }
    }

    public static void withReadOnlyFixture(final File segmentStore,
                                           final SessionStrategy sessionStrategy)
            throws Exception {

        try (NodeStoreFixture fixture = TestUtil.getReadOnlyFixture(segmentStore, null)) {
            Repository repo = null;
            Session session = null;
            try {
                repo = JcrFactory.getJcr(fixture);
                session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
                sessionStrategy.accept(session);
            } finally {
                if (session != null) {
                    session.logout();
                }
                TestUtil.closeRepo(repo);
            }
        }
    }


    public static void installCndFromURL(final Session session, final URL... cnds)
            throws RepositoryException, IOException, ParseException {
        final Workspace workspace = session.getWorkspace();
        final NodeTypeManager nodeTypeManager = workspace.getNodeTypeManager();
        final NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
        final ValueFactory valueFactory = session.getValueFactory();

        if (cnds != null) {
            for (URL cnd : cnds) {
                try (InputStream is = cnd.openStream()) {
                    Reader reader = new InputStreamReader(is);
                    CndImporter.registerNodeTypes(reader, cnd.toExternalForm(), nodeTypeManager, namespaceRegistry,
                            valueFactory, false);
                }
            }
        }
    }

    public static List<Map.Entry<PrivilegeDefinition, Result<Privilege>>>
    installPrivilegesWithIndivResults(final Session session, final URL... privileges) throws RepositoryException {
        final Workspace workspace = session.getWorkspace();
        final NamePathResolver resolver = new DefaultNamePathResolver(session);
        final NamespaceRegistry registry = workspace.getNamespaceRegistry();

        final BiConsumer<String, String> nsSetter = Fun.tryOrVoid2(registry::registerNamespace);
        final Function<Name, String> mapper = Fun.tryOrDefault1(resolver::getJCRName, null);

        if (!(workspace instanceof JackrabbitWorkspace)) {
            throw new RepositoryException("Workspace must be instance of JackrabbitWorkspace, but isn't. type: " +
                    workspace.getClass().getName());
        }
        final PrivilegeManager privilegeManager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
        return Stream.of(privileges).flatMap(uncheck1((URL privUrl) -> {
            try (Reader reader = new InputStreamReader(privUrl.openStream(), StandardCharsets.UTF_8)) {
                final PrivilegeDefinitionReader privReader =
                        new PrivilegeDefinitionReader(reader, "text/xml");
                privReader.getNamespaces().forEach(nsSetter);
                return Stream.of(privReader.getPrivilegeDefinitions());
            }
        })).map(zipKeysWithValueFunc(result1((PrivilegeDefinition def) ->
                privilegeManager.registerPrivilege(resolver.getJCRName(def.getName()),
                        def.isAbstract(),
                        def.getDeclaredAggregateNames().stream()
                                .map(mapper).toArray(String[]::new))
        ))).collect(Collectors.toList());
    }

    public static void installPrivilegesFromURL(final Session session, final URL... privileges)
            throws RepositoryException, IOException, org.apache.jackrabbit.spi.commons.privilege.ParseException {
        final Workspace workspace = session.getWorkspace();
        final NamePathResolver resolver = new DefaultNamePathResolver(session);
        final NamespaceRegistry registry = workspace.getNamespaceRegistry();

        final BiConsumer<String, String> nsSetter = Fun.tryOrVoid2(registry::registerNamespace);
        final Function<Name, String> mapper = Fun.tryOrDefault1(resolver::getJCRName, null);

        if (!(workspace instanceof JackrabbitWorkspace)) {
            throw new RepositoryException("Workspace must be instance of JackrabbitWorkspace, but isn't. type: " +
                    workspace.getClass().getName());
        }
        final PrivilegeManager privilegeManager = ((JackrabbitWorkspace) session.getWorkspace()).getPrivilegeManager();
        for (URL privUrl : privileges) {
            try (Reader reader = new InputStreamReader(privUrl.openStream(), StandardCharsets.UTF_8)) {
                PrivilegeDefinitionReader privReader =
                        new PrivilegeDefinitionReader(reader, "text/xml");

                privReader.getNamespaces().forEach(nsSetter);

                for (PrivilegeDefinition def : privReader.getPrivilegeDefinitions()) {
                    privilegeManager.registerPrivilege(resolver.getJCRName(def.getName()),
                            def.isAbstract(),
                            def.getDeclaredAggregateNames().stream()
                                    .map(mapper).toArray(String[]::new));
                }
            }
        }
    }

    public interface SessionStrategy {
        void accept(Session session) throws Exception;
    }
}
