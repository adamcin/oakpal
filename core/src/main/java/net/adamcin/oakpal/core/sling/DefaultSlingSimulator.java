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
import net.adamcin.oakpal.api.RepoInitScriptsInstallable;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.SlingSimulator;
import net.adamcin.oakpal.core.ErrorListener;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.impl.ZipVaultPackage;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.core.impl.InternalResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;

import static net.adamcin.oakpal.api.Fun.compose1;
import static net.adamcin.oakpal.api.Fun.result0;
import static net.adamcin.oakpal.api.Fun.result1;

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

    private final Queue<SlingInstallable<?>> installables = new LinkedList<>();

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
    public @Nullable SlingInstallable<?> dequeueInstallable() {
        return installables.poll();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <InstallableType> Fun.ThrowingSupplier<InstallableType>
    open(@NotNull final SlingInstallable<InstallableType> installable) {
        if (installable instanceof RepoInitScriptsInstallable) {
            return () ->
                    (InstallableType) openRepoInitScripts((RepoInitScriptsInstallable) installable);
        } else if (installable instanceof EmbeddedPackageInstallable) {
            return () ->
                    (InstallableType) openEmbeddedPackage((EmbeddedPackageInstallable) installable);
        }
        return () -> {
            throw new IllegalArgumentException("Unsupported installable type: " + installable.getClass());
        };
    }

    public @NotNull Iterable<String>
    openRepoInitScripts(@NotNull final RepoInitScriptsInstallable installable) {
        return installable.getScripts();
    }

    public @Nullable Fun.ThrowingSupplier<JcrPackage>
    openEmbeddedPackage(@NotNull final EmbeddedPackageInstallable installable) {
        return () -> packageManager.open(session.getNode(installable.getJcrPath()), true);
    }

    @Override
    public @Nullable SlingInstallable<?> prepareInstallableNode(final @NotNull PackageId parentPackageId,
                                                                final @NotNull Node node) {
        final Result<String> jcrPathResult = result0(node::getPath).get();
        final Result<Optional<SlingInstallableParams<?>>> result = jcrPathResult
                .flatMap(result1(session::getNode))
                .flatMap(DefaultSlingSimulator::readInternalResourceFromNode)
                .map(resource -> resource.flatMap(DefaultSlingSimulator::createSlingInstallableParams));
        return jcrPathResult.flatMap(jcrPath ->
                result.map(optParams ->
                        optParams.map(params -> params.createInstallable(parentPackageId, jcrPath))))
                .toOptional().flatMap(Function.identity())
                .orElse(null);
    }

    @Override
    public @Nullable SlingInstallable<?> submitInstallable(final @NotNull SlingInstallable<?> installable) {
        installables.add(installable);
        return installable;
    }

    static final String SLING_NS = "http://sling.apache.org/jcr/sling/1.0";
    static final String SLING_OSGI_CONFIG = "{" + SLING_NS + "}OsgiConfig";
    static final String JCR_CONTENT_DATA = "jcr:content/jcr:data";

    static @NotNull Result<Optional<InternalResource>>
    readInternalResourceFromNode(final @NotNull Node node) {
        return result0(() -> {
            final String path = node.getPath();
            if (Arrays.asList(node.getSession().getWorkspace().getNamespaceRegistry().getURIs()).contains(SLING_NS)
                    && node.isNodeType(SLING_OSGI_CONFIG)) {
                final Dictionary<String, Object> props = new Hashtable<>();
                loadJcrProperties(props, node);
                return Optional.of(new InstallableResource(path, null, props, "", null, 20));
            } else if (node.hasProperty(JCR_CONTENT_DATA)) {
                final InputStream is = node.getProperty(JCR_CONTENT_DATA).getStream();
                final Dictionary<String, Object> dict = new Hashtable<>();
                dict.put(InstallableResource.INSTALLATION_HINT, node.getParent().getName());
                return Optional.of(new InstallableResource(path, is, dict, "", null, 200));
            }
            return Optional.<InstallableResource>empty();
        }).get().flatMap(resource -> resource
                .map(compose1(
                        DefaultSlingSimulator::readInternalResourceFromInstallableResource,
                        result -> result.map(Optional::of)))
                .orElse(Result.success(Optional.empty())));
    }

    static @NotNull Result<InternalResource>
    readInternalResourceFromInstallableResource(final @NotNull InstallableResource resource) {
        return result0(() -> InternalResource.create("jcrinstall", resource)).get();
    }

    static @NotNull Optional<SlingInstallableParams<?>>
    createSlingInstallableParams(final @NotNull InternalResource resource) {
        SlingInstallableParams<?> installable = null;
        if (InstallableResource.TYPE_FILE.equals(resource.getType())) {
            installable = maybePackageResource(resource);
            if (installable != null) {
                return Optional.of(installable);
            }
            // maybe do something with bundles in the future
        } else if (InstallableResource.TYPE_PROPERTIES.equals(resource.getType())) {
            // convert to OsgiConfigInstallable
            installable = maybeConfigResource(resource);
            if (installable != null) {
                // if RepoInit, wrap with RepoInitScriptInstallable
                return Optional.of(installable);
            }
        }
        return Optional.empty();
    }

    static @Nullable EmbeddedPackageInstallableParams maybePackageResource(final @NotNull InternalResource resource) {
        return result0(() -> new ZipVaultPackage(resource.getPrivateCopyOfFile(), true, false)).get()
                .map(VaultPackage::getId)
                .map(EmbeddedPackageInstallableParams::new).toOptional().orElse(null);
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

    private static String removeConfigExtension(final String id) {
        for (final String ext : EXTENSIONS) {
            if (id.endsWith(ext)) {
                return id.substring(0, id.length() - ext.length());
            }
        }
        return id;
    }

    static @Nullable OsgiConfigInstallableParams maybeConfigResource(final @NotNull InternalResource resource) {
        final String lastIdPart = getResourceId(resource.getURL());

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

        final Map<String, Object> properties = new HashMap<>();
        Optional.ofNullable(resource.getPrivateCopyOfDictionary()).ifPresent(dict -> {
            for (final Enumeration<String> keys = dict.keys(); keys.hasMoreElements(); ) {
                final String key = keys.nextElement();
                properties.put(key, dict.get(key));
            }
        });

        return new OsgiConfigInstallableParams(properties, configPid, factoryPid);
    }

    static void loadJcrProperties(final @NotNull Dictionary<String, Object> configMap, final @NotNull Node configNode)
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
