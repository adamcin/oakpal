/*
 * Copyright 2020 Mark Adamcin
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

package net.adamcin.oakpal.core.sling;

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.SlingOpenable;
import net.adamcin.oakpal.api.SlingSimulator;
import net.adamcin.oakpal.core.ErrorListener;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.felix.cm.json.Configurations;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.vault.packaging.CyclicDependencyException;
import org.apache.jackrabbit.vault.packaging.DependencyUtil;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.installer.api.InstallableResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import static net.adamcin.oakpal.api.Fun.inferTest1;
import static net.adamcin.oakpal.api.Fun.result0;
import static net.adamcin.oakpal.api.Fun.result1;
import static net.adamcin.oakpal.api.Fun.toEntry;

/**
 * Noop implementation of a SlingSimulator.
 */
public final class DefaultSlingSimulator implements SlingSimulatorBackend, SlingSimulator {

    public static SlingSimulatorBackend instance() {
        return new DefaultSlingSimulator();
    }

    private Session session;
    private JcrPackageManager packageManager;

    private ErrorListener errorListener;

    private final LinkedList<SlingInstallable> installables = new LinkedList<>();

    @Override
    public void startedScan() {
        installables.clear();
    }

    @Override
    public void setSession(final Session session) {
        this.session = session;
    }

    @Override
    public void setPackageManager(final JcrPackageManager packageManager) {
        this.packageManager = packageManager;
    }

    @Override
    public void setErrorListener(final ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    @Override
    public @Nullable SlingInstallable dequeueInstallable() {
        return installables.poll();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <ResourceType> Fun.ThrowingSupplier<ResourceType>
    open(@NotNull final SlingOpenable<ResourceType> installable) {
        if (installable instanceof EmbeddedPackageInstallable) {
            return () ->
                    (ResourceType) openEmbeddedPackage((EmbeddedPackageInstallable) installable);
        }
        return () -> {
            throw new IllegalArgumentException("Unsupported installable type: " + installable.getClass());
        };
    }

    public @NotNull JcrPackage
    openEmbeddedPackage(@NotNull final EmbeddedPackageInstallable installable) throws IOException, RepositoryException {
        Node packageNode = session.getNode(installable.getJcrPath());
        try (InputStream input = JcrUtils.readFile(packageNode)) {
            return packageManager.upload(input, true, true);
        }
    }

    <T extends SlingInstallable> Optional<T> createInstallableOrReport(final @NotNull SlingInstallableParams<T> params,
                                                                       final @NotNull PackageId parentPackageId,
                                                                       final @NotNull String jcrPath) {
        try {
            return Optional.of(params.createInstallable(parentPackageId, jcrPath));
        } catch (Exception e) {
            errorListener.onSlingCreateInstallableError(e, params.getInstallableType(),
                    parentPackageId, jcrPath);
            return Optional.empty();
        }
    }

    void internalAddInstallable(final @NotNull SlingInstallable installable) {
        installables.removeIf(registered ->
                installable.getJcrPath().equals(registered.getJcrPath()));
        installables.add(installable);
    }

    @Override
    public @Nullable SlingInstallable addInstallableNode(final @NotNull PackageId parentPackageId,
                                                         final @NotNull Node node) {
        final Result<String> jcrPathResult = result0(node::getPath).get();
        final Result<Optional<SlingInstallableParams<?>>> result = jcrPathResult
                .flatMap(result1(session::getNode))
                .flatMap(this::readInstallableParamsFromNode);
        SlingInstallable installable = jcrPathResult.flatMap(jcrPath ->
                result.map(optParams ->
                        optParams.flatMap(params ->
                                createInstallableOrReport(params, parentPackageId, jcrPath))))
                .toOptional().flatMap(Function.identity())
                .orElse(null);
        if (installable != null) {
            internalAddInstallable(installable);
        }
        if (installable instanceof EmbeddedPackageInstallable) {
            Fun.resultNothing1(DefaultSlingSimulator::shufflePackagesByDependency).apply(this);
        }
        return installable;
    }

    LinkedList<SlingInstallable> getInstallables() {
        return installables;
    }

    void shufflePackagesByDependency() throws RepositoryException, CyclicDependencyException, IOException {
        final Map<PackageId, EmbeddedPackageInstallable> originalLookup = getInstallables().stream()
                .filter(EmbeddedPackageInstallable.class::isInstance)
                .map(EmbeddedPackageInstallable.class::cast)
                .map(inst -> toEntry(inst.getEmbeddedId(), inst))
                .collect(Fun.entriesToMapOfType(LinkedHashMap::new, Fun.keepFirstMerger()));

        final List<PackageId> originalOrder = new ArrayList<>(originalLookup.keySet());
        final List<PackageId> sortedOrder = new ArrayList<>();

        final List<JcrPackage> closeable = new LinkedList<>();
        final List<VaultPackage> sortable = new LinkedList<>();
        try {
            for (PackageId key : originalOrder) {
                final JcrPackage jcrPack = packageManager.open(session.getNode(originalLookup.get(key).getJcrPath()),
                        true);
                closeable.add(jcrPack);
                sortable.add(jcrPack.getPackage());
            }
            DependencyUtil.sort(sortable);
            for (VaultPackage pack : sortable) {
                sortedOrder.add(pack.getId());
            }
        } finally {
            closeable.forEach(Fun.toVoid1(Fun.resultNothing1(JcrPackage::close)));
        }

        if (sortedOrder.size() == originalOrder.size() && !originalOrder.equals(sortedOrder)) {
            for (int i = 0; i < originalOrder.size(); i++) {
                if (!originalOrder.get(i).equals(sortedOrder.get(i))) {
                    for (PackageId key : sortedOrder.subList(i, sortedOrder.size())) {
                        final EmbeddedPackageInstallable installable = originalLookup.get(key);
                        if (getInstallables().removeIf(inferTest1(EmbeddedPackageInstallable.class::isInstance)
                                .and(registered ->
                                        key.equals(((EmbeddedPackageInstallable) registered).getEmbeddedId())))) {

                            internalAddInstallable(installable);
                        }
                    }
                    break;
                }
            }
        }
    }

    static final String SLING_NS = "http://sling.apache.org/jcr/sling/1.0";
    static final String SLING_OSGI_CONFIG = "{" + SLING_NS + "}OsgiConfig";
    static final String JCR_CONTENT_DATA = "jcr:content/jcr:data";

    static class NodeRes {
        private final Node node;
        private final String path;
        private final Map<String, Object> props = new HashMap<>();
        private Exception parseError = null;

        public NodeRes(final Node node, final String path) {
            this.node = node;
            this.path = path;
        }

        public Node getNode() {
            return node;
        }

        public String getPath() {
            return path;
        }

        public Exception getParseError() {
            return parseError;
        }

        public void setParseError(final Exception parseError) {
            this.parseError = parseError;
        }

        public Map<String, Object> getProps() {
            return props;
        }
    }

    @NotNull Result<Optional<SlingInstallableParams<?>>>
    readInstallableParamsFromNode(final @NotNull Node node) {
        return result0(() -> {
            final String path = node.getPath();
            NodeRes nodeRes = new NodeRes(node, path);
            if (Arrays.asList(node.getSession().getWorkspace().getNamespaceRegistry().getURIs()).contains(SLING_NS)
                    && node.isNodeType(SLING_OSGI_CONFIG)) {
                // handle sling:OsgiConfig
                loadJcrProperties(nodeRes.getProps(), node);

                return maybeConfigResource(nodeRes);
            } else if (node.hasProperty(JCR_CONTENT_DATA)) {
                // this could be a properties file, or a package file (.zip), or a bundle (.jar)
                // check extension here
                nodeRes.getProps().put(InstallableResource.INSTALLATION_HINT, node.getParent().getName());

                EmbeddedPackageInstallableParams packageInstallableParams = maybePackageResource(nodeRes);
                if (packageInstallableParams != null) {
                    return packageInstallableParams;
                }

                if (isConfigExtension(path)) {
                    try (InputStream is = node.getProperty(JCR_CONTENT_DATA).getBinary().getStream()) {
                        nodeRes.getProps().putAll(readDictionary(is, nodeRes.getPath()));
                    } catch (Exception e) {
                        nodeRes.setParseError(e);
                    }

                    return maybeConfigResource(nodeRes);
                }
            }
            return null;
        }).get().map(Optional::ofNullable);
    }

    @Nullable EmbeddedPackageInstallableParams maybePackageResource(final @NotNull NodeRes nodeRes) {
        if (nodeRes.getPath().endsWith(".zip")) {
            try (JcrPackage pack = packageManager.open(nodeRes.getNode(), true)) {
                return result1(JcrPackage::getPackage).apply(pack)
                        .flatMap(result1(VaultPackage::getId))
                        .map(EmbeddedPackageInstallableParams::new)
                        .toOptional().orElse(null);
            } catch (RepositoryException e) {
                /* TODO log this or something? */
                return null;
            }
        }
        return null;
    }

    static String separatorsToUnix(final String path) {
        if (path == null || path.indexOf('\\') == -1) {
            return path;
        }
        return path.replace('\\', '/');
    }

    static String getResourceId(final String rawUrl) {
        final String url = separatorsToUnix(rawUrl);
        int pos = url.lastIndexOf('/');
        if (pos == -1) {
            pos = url.indexOf(':');
        }
        final String lastIdPart;
        if (pos != -1) {
            lastIdPart = url.substring(pos + 1);
        } else {
            lastIdPart = url;
        }
        return lastIdPart;
    }

    private static final List<String> EXTENSIONS = Arrays.asList(".config", ".properties", ".cfg", ".cfg.json");

    static String removeConfigExtension(final String id) {
        for (final String ext : EXTENSIONS) {
            if (id.endsWith(ext)) {
                return id.substring(0, id.length() - ext.length());
            }
        }
        return id;
    }

    static boolean isConfigExtension(String url) {
        for (final String ext : EXTENSIONS) {
            if (url.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Read dictionary from an input stream.
     * We use the same logic as Apache Felix FileInstall here, but only for .config files:
     * *.config files are handled by the Apache Felix ConfigAdmin file reader
     * NOTE: Adapted from InternalResource.readDictionary() in sling-installer-core.
     *
     * @param is the input stream
     * @param id the id
     * @throws IOException when a parsing error occurs
     */
    static Map<String, Object> readDictionary(
            final InputStream is, final String id) throws IOException {
        final Map<String, Object> ht = new LinkedHashMap<>();
        if (id.endsWith(".cfg.json")) {
            final String name = "jcrinstall:".concat(id);
            String configId;
            int pos = id.lastIndexOf('/');
            if (pos == -1) {
                configId = id;
            } else {
                configId = id.substring(pos + 1);
            }
            pos = configId.indexOf('-');
            if (pos != -1) {
                configId = configId.substring(0, pos).concat("~").concat(configId.substring(pos + 1));
            }
            configId = removeConfigExtension(configId);

            // read from input stream
            try (final Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                final Dictionary<String, Object> config = Configurations.buildReader()
                        .withIdentifier(configId).build(reader).readConfiguration();

                final Enumeration<String> i = config.keys();
                while (i.hasMoreElements()) {
                    final String key = i.nextElement();
                    ht.put(key, config.get(key));
                }
            }
        } else {
            try (final BufferedInputStream in = new BufferedInputStream(is)) {

                if (id.endsWith(".config")) {
                    // check for initial comment line
                    in.mark(256);
                    final int firstChar = in.read();
                    if (firstChar == '#') {
                        int b;
                        while ((b = in.read()) != '\n') {
                            if (b == -1) {
                                throw new IOException("Unable to read configuration.");
                            }
                        }
                    } else {
                        in.reset();
                    }
                    @SuppressWarnings("unchecked") final Dictionary<String, Object> config = ConfigurationHandler.read(in);
                    final Enumeration<String> i = config.keys();
                    while (i.hasMoreElements()) {
                        final String key = i.nextElement();
                        ht.put(key, config.get(key));
                    }
                } else {
                    final Properties p = new Properties();
                    in.mark(1);
                    boolean isXml = in.read() == '<';
                    in.reset();
                    if (isXml) {
                        p.loadFromXML(in);
                    } else {
                        p.load(in);
                    }
                    final Enumeration<Object> i = p.keys();
                    while (i.hasMoreElements()) {
                        final Object key = i.nextElement();
                        ht.put(key.toString(), p.get(key));
                    }
                }
            }
        }
        return ht;
    }


    static @Nullable OsgiConfigInstallableParams maybeConfigResource(final @NotNull NodeRes resource) {
        final String lastIdPart = getResourceId(resource.getPath());

        // remove extension if known
        final String pid = removeConfigExtension(lastIdPart);

        // split pid and factory pid alias
        final String factoryPid;
        final String configPid;
        int n = pid.indexOf('~');
        if (n == -1) {
            n = pid.indexOf('-');
        }
        if (n > 0) {
            configPid = pid.substring(n + 1);
            factoryPid = pid.substring(0, n);
        } else {
            factoryPid = null;
            configPid = pid;
        }

        return new OsgiConfigInstallableParams(resource.getProps(), configPid,
                factoryPid, resource.getParseError());
    }

    static void loadJcrProperties(final @NotNull Map<String, Object> configMap, final @NotNull Node configNode)
            throws RepositoryException {
        final PropertyIterator pi = configNode.getProperties();
        while (pi.hasNext()) {
            final Property p = pi.nextProperty();
            final String name = p.getName();

            // ignore jcr: and similar properties
            if (name.contains(":")) {
                continue;
            }
            if (p.getDefinition().isMultiple()) {
                Object[] data = null;
                final Value[] values = p.getValues();
                int i = 0;
                for (Value v : values) {
                    Object o = convertJcrValue(v);
                    if (o != null) {
                        if (i == 0) {
                            data = (Object[]) Array.newInstance(o.getClass(), values.length);
                        }
                        data[i++] = o;
                    }
                }
                // create empty array in case no value is specified
                if (data == null) {
                    data = new String[0];
                }
                configMap.put(name, data);

            } else {
                final Object o = convertJcrValue(p.getValue());
                if (o != null) {
                    configMap.put(name, o);
                }
            }
        }
    }

    static @Nullable Object convertJcrValue(final @NotNull Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.STRING:
                return value.getString();
            case PropertyType.DATE:
                return value.getDate();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.BOOLEAN:
                return value.getBoolean();
        }
        return null;
    }

}
