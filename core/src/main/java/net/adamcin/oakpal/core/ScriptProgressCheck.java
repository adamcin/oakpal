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

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.api.JavaxJson;
import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.ReportCollector;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleViolation;
import net.adamcin.oakpal.api.SlingInstallable;
import net.adamcin.oakpal.api.SlingSimulator;
import net.adamcin.oakpal.api.Violation;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.core.Util.isEmpty;

/**
 * The {@link ScriptProgressCheck} uses the {@link Invocable} interface from JSR223 to listen for scan events and
 * optionally report check violations.
 * <p>
 * You may implement only the methods you need to enforce your package check rules.
 * <dl>
 * <dt>getCheckName()</dt>
 * <dd>{@link ProgressCheck#getCheckName()}</dd>
 * <dt>simulateSling(slingSimulator, runModes)</dt>
 * <dd>{@link ProgressCheck#simulateSling(SlingSimulator, Set)} ()}</dd>
 * <dt>startedScan()</dt>
 * <dd>{@link ProgressCheck#startedScan()}</dd>
 * <dt>identifyPackage(packageId, packageFile)</dt>
 * <dd>{@link ProgressCheck#identifyPackage(PackageId, File)}</dd>
 * <dt>beforeExtract(packageId, inspectSession, packageProperties, metaInf, subpackageIds)</dt>
 * <dd>{@link ProgressCheck#beforeExtract(PackageId, Session, PackageProperties, MetaInf, List)}</dd>
 * <dt>importedPath(packageId, path, node, action)</dt>
 * <dd>{@link ProgressCheck#importedPath(PackageId, String, Node, PathAction)}</dd>
 * <dt>deletedPath(packageId, path, inspectSession)</dt>
 * <dd>{@link ProgressCheck#deletedPath(PackageId, String, Session)}</dd>
 * <dt>afterExtract(packageId, inspectSession)</dt>
 * <dd>{@link ProgressCheck#afterExtract(PackageId, Session)}</dd>
 * <dt>identifySubpackage(packageId, parentPackageId)</dt>
 * <dd>{@link ProgressCheck#identifySubpackage(PackageId, PackageId)}</dd>
 * <dt>beforeSlingInstall(scanPackageId, slingInstallable, inspectSession)</dt>
 * <dd>{@link ProgressCheck#beforeSlingInstall(PackageId, SlingInstallable, Session)}</dd>
 * <dt>identifyEmbeddedPackage(packageId, parentPackageId, slingInstallable)</dt>
 * <dd>{@link ProgressCheck#identifyEmbeddedPackage(PackageId, PackageId, EmbeddedPackageInstallable)}</dd>
 * <dt>appliedRepoInitScripts(scanPackageId, scripts, slingInstallable, inspectSession)</dt>
 * <dd>{@link ProgressCheck#appliedRepoInitScripts(PackageId, List, SlingInstallable, Session)}</dd>
 * <dt>afterScanPackage(scanPackageId, inspectSession)</dt>
 * <dd>{@link ProgressCheck#afterScanPackage(PackageId, Session)}</dd>
 * <dt>finishedScan()</dt>
 * <dd>{@link ProgressCheck#finishedScan()}</dd>
 * </dl>
 * <p>
 * To report package violations, a {@link ScriptHelper} is bound to the global variable "oakpal".
 */
public final class ScriptProgressCheck implements ProgressCheck {
    public static final String DEFAULT_SCRIPT_ENGINE_EXTENSION = "js";
    public static final String BINDING_SCRIPT_HELPER = "oakpal";
    public static final String BINDING_CHECK_CONFIG = "config";
    public static final String FILENAME_INLINE_SCRIPT = "_inlineScript_";
    public static final String INVOKE_ON_STARTED_SCAN = "startedScan";
    public static final String INVOKE_ON_SIMULATE_SLING = "simulateSling";
    public static final String INVOKE_ON_IDENTIFY_PACKAGE = "identifyPackage";
    public static final String INVOKE_ON_READ_MANIFEST = "readManifest";
    public static final String INVOKE_ON_BEFORE_EXTRACT = "beforeExtract";
    public static final String INVOKE_ON_IMPORTED_PATH = "importedPath";
    public static final String INVOKE_ON_DELETED_PATH = "deletedPath";
    public static final String INVOKE_ON_AFTER_EXTRACT = "afterExtract";
    public static final String INVOKE_ON_IDENTIFY_SUBPACKAGE = "identifySubpackage";
    public static final String INVOKE_ON_BEFORE_SLING_INSTALL = "beforeSlingInstall";
    public static final String INVOKE_ON_IDENTIFY_EMBEDDED_PACKAGE = "identifyEmbeddedPackage";
    public static final String INVOKE_ON_APPLIED_REPO_INIT_SCRIPTS = "appliedRepoInitScripts";
    public static final String INVOKE_ON_AFTER_SCAN_PACKAGE = "afterScanPackage";
    public static final String INVOKE_ON_FINISHED_SCAN = "finishedScan";
    public static final String INVOKE_GET_CHECK_NAME = "getCheckName";
    private static final String GRAAL_JS_PROXY_OBJECT_CLASS = "org.graalvm.polyglot.proxy.ProxyObject";
    private static final String GRAAL_JS_PROXY_OBJECT_METHOD_FROM_MAP = "fromMap";

    private static final String ENGINE_NASHORN = "Nashorn";
    private static final String ENGINE_GRAALJS = "Graal.js";
    private static final Set<String> JS_ENGINES = Stream.of(
                    ENGINE_NASHORN,
                    ENGINE_GRAALJS,
                    "JS", "JavaScript", "ECMAScript")
            .flatMap(name -> Stream.of(name, name.toLowerCase()))
            .collect(Collectors.toSet());
    private static final boolean USE_NASHORN =
            new ScriptEngineManager(ScriptProgressCheck.class.getClassLoader())
                    .getEngineByName(ENGINE_NASHORN) != null;

    private final Invocable script;
    private final ScriptHelper helper;
    private final URL scriptUrl;
    private final Set<String> handlerMissCache = new HashSet<>();

    ScriptProgressCheck(final @NotNull Invocable script,
                        final @NotNull ScriptHelper helper,
                        final @Nullable URL scriptUrl) {
        this.script = script;
        this.helper = helper;
        this.scriptUrl = scriptUrl;
    }

    @Override
    public @Nullable String getResourceBundleBaseName() {
        return null;
    }

    @Override
    public void setResourceBundle(final ResourceBundle resourceBundle) {
        this.helper.setResourceBundle(resourceBundle);
    }

    ScriptHelper getHelper() {
        return helper;
    }

    private String getScriptPath() {
        if (this.scriptUrl != null) {
            return this.scriptUrl.getPath();
        } else {
            return FILENAME_INLINE_SCRIPT;
        }
    }

    private String getFilename() {
        final String scriptPath = this.getScriptPath();
        final int lastSlash = scriptPath.lastIndexOf("/");
        if (lastSlash >= 0 && scriptPath.length() > lastSlash + 1) {
            return scriptPath.substring(lastSlash + 1);
        } else {
            return scriptPath;
        }
    }

    @Override
    public String getCheckName() {
        try {
            Object result = this.script.invokeFunction(INVOKE_GET_CHECK_NAME);
            if (result != null) {
                return String.valueOf(result);
            } else {
                return getFilename();
            }
        } catch (NoSuchMethodException ignored) {
            return getFilename();
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Script handler callback passed to {@link EventHandlerBody}.
     */
    @FunctionalInterface
    interface HandlerHandle {
        void apply(Object... args) throws NoSuchMethodException, ScriptException;
    }

    /**
     * Lambda type for event handler body logic to eliminate boilerplate exception handling.
     */
    @FunctionalInterface
    interface EventHandlerBody {
        void apply(HandlerHandle handle) throws NoSuchMethodException, ScriptException;
    }

    /**
     * Guards against script handler calls by remembering when NoSuchMethodExceptions are thrown when the script
     * function named by the {@code methodName} argument is invoked.
     *
     * @param methodName the name of the handler function to invoke
     * @param body       the ScriptProgressCheck adapter body logic to execute
     */
    void guardHandler(final String methodName, final EventHandlerBody body) {
        if (!handlerMissCache.contains(methodName)) {
            try {
                body.apply((args) -> this.script.invokeFunction(methodName, args));
            } catch (NoSuchMethodException ignored) {
                handlerMissCache.add(methodName);
            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Guards against script handler calls by remembering when NoSuchMethodExceptions are thrown when the script
     * function named by the {@code methodName} argument is invoked.
     * Unlike {@link #guardHandler(String, EventHandlerBody)}, this method rethrows RepositoryExceptions to satisfy the
     * contract of some progress check methods.
     *
     * @param methodName the name of the handler function to invoke
     * @param body       the ScriptProgressCheck adapter body logic to execute
     * @throws RepositoryException if a ScriptException is thrown with a RepositoryException cause
     */
    void guardSessionHandler(final String methodName, final EventHandlerBody body) throws RepositoryException {
        if (!handlerMissCache.contains(methodName)) {
            try {
                body.apply((args) -> this.script.invokeFunction(methodName, args));
            } catch (NoSuchMethodException ignored) {
                handlerMissCache.add(methodName);
            } catch (ScriptException e) {
                if (Result.failure(e).findCause(RepositoryException.class).isPresent()) {
                    throw new ScriptRepositoryException(e);
                }
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void simulateSling(final SlingSimulator slingSimulator, final Set<String> runModes) {
        guardHandler(INVOKE_ON_SIMULATE_SLING, handle -> handle.apply(slingSimulator, runModes));
    }

    @Override
    public void startedScan() {
        helper.collector.clearViolations();
        guardHandler(INVOKE_ON_STARTED_SCAN, HandlerHandle::apply);
    }

    @Override
    public void identifyPackage(final PackageId packageId, final File file) {
        guardHandler(INVOKE_ON_IDENTIFY_PACKAGE, handle -> handle.apply(packageId, file));
    }


    @Override
    public void readManifest(final PackageId packageId, final Manifest manifest) {
        guardHandler(INVOKE_ON_READ_MANIFEST, handle -> handle.apply(packageId, manifest));
    }

    @Override
    public void beforeExtract(final PackageId packageId, final Session inspectSession,
                              final PackageProperties packageProperties, final MetaInf metaInf,
                              final List<PackageId> subpackages) throws RepositoryException {
        guardSessionHandler(INVOKE_ON_BEFORE_EXTRACT,
                handle -> handle.apply(packageId, inspectSession, packageProperties,
                        metaInf, subpackages.toArray(new PackageId[0])));
    }

    @Override
    public void importedPath(final PackageId packageId, final String path, final Node node,
                             final PathAction action) throws RepositoryException {
        guardSessionHandler(INVOKE_ON_IMPORTED_PATH, handle -> handle.apply(packageId, path, node, action));
    }

    @Override
    public void deletedPath(final PackageId packageId, final String path, final Session inspectSession)
            throws RepositoryException {
        guardSessionHandler(INVOKE_ON_DELETED_PATH, handle -> handle.apply(packageId, path, inspectSession));
    }

    @Override
    public void afterExtract(final PackageId packageId, final Session inspectSession) throws RepositoryException {
        guardSessionHandler(INVOKE_ON_AFTER_EXTRACT, handle -> handle.apply(packageId, inspectSession));
    }

    @Override
    public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
        guardHandler(INVOKE_ON_IDENTIFY_SUBPACKAGE, handle -> handle.apply(packageId, parentId));
    }

    @Override
    public void beforeSlingInstall(final PackageId lastPackage,
                                   final SlingInstallable slingInstallable,
                                   final Session inspectSession) throws RepositoryException {
        guardSessionHandler(INVOKE_ON_BEFORE_SLING_INSTALL,
                handle -> handle.apply(lastPackage, slingInstallable, inspectSession));
    }

    @Override
    public void identifyEmbeddedPackage(final PackageId packageId,
                                        final PackageId parentId,
                                        final EmbeddedPackageInstallable slingInstallable) {
        guardHandler(INVOKE_ON_IDENTIFY_EMBEDDED_PACKAGE, handle -> handle.apply(packageId, parentId, slingInstallable));
    }

    @Override
    public void appliedRepoInitScripts(final PackageId lastPackage,
                                       final List<String> scripts,
                                       final SlingInstallable slingInstallable,
                                       final Session inspectSession) throws RepositoryException {
        guardSessionHandler(INVOKE_ON_APPLIED_REPO_INIT_SCRIPTS,
                handle -> handle.apply(lastPackage, scripts, slingInstallable, inspectSession));
    }

    @Override
    public void afterScanPackage(final PackageId scanPackageId, final Session inspectSession) throws RepositoryException {
        guardSessionHandler(INVOKE_ON_AFTER_SCAN_PACKAGE, handle -> handle.apply(scanPackageId, inspectSession));
    }

    @Override
    public void finishedScan() {
        guardHandler(INVOKE_ON_FINISHED_SCAN, HandlerHandle::apply);
    }

    @Override
    public final Collection<Violation> getReportedViolations() {
        return this.helper.collector.getReportedViolations();
    }

    /**
     * ScriptHelper helps scripts to report violations by eliminating the need to import the severity enumerator type.
     */
    @SuppressWarnings("WeakerAccess")
    public static class ScriptHelper {
        private final ReportCollector collector = new ReportCollector();
        private ResourceBundle resourceBundle;

        public void minorViolation(String description, PackageId... packageIds) {
            collector.reportViolation(new SimpleViolation(Severity.MINOR, description, packageIds));
        }

        public void majorViolation(String description, PackageId... packageIds) {
            collector.reportViolation(new SimpleViolation(Severity.MAJOR, description, packageIds));
        }

        public void severeViolation(String description, PackageId... packageIds) {
            collector.reportViolation(new SimpleViolation(Severity.SEVERE, description, packageIds));
        }

        private void setResourceBundle(final ResourceBundle resourceBundle) {
            this.resourceBundle = resourceBundle;
        }

        public String getString(final String key) {
            if (key != null && resourceBundle != null && resourceBundle.containsKey(key)) {
                return resourceBundle.getString(key);
            } else {
                return key;
            }
        }
    }

    /**
     * Internal {@link ProgressCheckFactory} impl for script check creation.
     */
    static class ScriptProgressCheckFactory implements ProgressCheckFactory {

        private final Fun.ThrowingSupplier<ScriptEngine> engineSupplier;
        private final URL scriptUrl;

        private ScriptProgressCheckFactory(final @NotNull Fun.ThrowingSupplier<ScriptEngine> engineSupplier,
                                           final @NotNull URL scriptUrl) {
            this.engineSupplier = engineSupplier;
            this.scriptUrl = scriptUrl;
        }

        ScriptEngine getEngine() {
            return Fun.uncheck0(engineSupplier).get();
        }

        @Override
        public ProgressCheck newInstance(final JsonObject config) throws Exception {
            final ScriptEngine engine = engineSupplier.tryGet();
            try (InputStream is = scriptUrl.openStream();
                 Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return internalNewInstance(engine, reader, scriptUrl, config);
            }
        }
    }

    private static void initContext(ScriptContext context) {
        context.setWriter(context.getErrorWriter());
    }

    private static ScriptProgressCheck internalNewInstance(@NotNull ScriptEngine engine,
                                                           @NotNull Reader reader,
                                                           URL scriptUrl,
                                                           JsonObject config) throws Exception {
        final Bindings scriptBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        final Map<String, Object> configMap = Optional.ofNullable(config)
                .map(JavaxJson::unwrapObject).orElse(Collections.emptyMap());
        Object value = wrapForGraalIfNecessary(engine, configMap);
        scriptBindings.put(BINDING_CHECK_CONFIG, value);
        final ScriptHelper helper = new ScriptHelper();
        scriptBindings.put(BINDING_SCRIPT_HELPER, helper);
        initContext(engine.getContext());
        engine.eval(reader);
        return new ScriptProgressCheck((Invocable) engine, helper, scriptUrl);
    }

    private static Object wrapForGraalIfNecessary(@NotNull ScriptEngine engine, @NotNull Map<String, Object> configMap) throws Exception {
        return (ENGINE_GRAALJS.equals(engine.getFactory().getEngineName()))
                ? Class.forName(GRAAL_JS_PROXY_OBJECT_CLASS).getMethod(GRAAL_JS_PROXY_OBJECT_METHOD_FROM_MAP, Map.class).invoke(null, configMap) : configMap;
    }

    private static class InlineScriptProgressCheckFactory implements ProgressCheckFactory {
        private Fun.ThrowingSupplier<ScriptEngine> engineSupplier;
        private final String source;

        private InlineScriptProgressCheckFactory(final @NotNull Fun.ThrowingSupplier<ScriptEngine> engineSupplier,
                                                 final @NotNull String source) {
            this.engineSupplier = engineSupplier;
            this.source = source;
        }

        @Override
        public ProgressCheck newInstance(final JsonObject config) throws Exception {
            final ScriptEngine engine = engineSupplier.tryGet();
            try (Reader reader = new StringReader(this.source)) {
                return internalNewInstance(engine, reader, null, config);
            }
        }
    }

    public static ProgressCheckFactory createScriptCheckFactory(final @NotNull URL scriptUrl) throws Exception {
        return createClassLoaderScriptCheckFactory(scriptUrl, Util.getDefaultClassLoader());
    }

    static boolean useJavaScriptEngine(@NotNull String nameOrExt) {
        return JS_ENGINES.contains(nameOrExt);
    }

    static ProgressCheckFactory createClassLoaderScriptCheckFactory(final @NotNull URL scriptUrl,
                                                                    final @Nullable ClassLoader classLoader) throws Exception {
        final int lastPeriod = scriptUrl.getPath().lastIndexOf(".");
        final String ext;
        if (lastPeriod < 0 || lastPeriod + 1 >= scriptUrl.getPath().length()) {
            ext = DEFAULT_SCRIPT_ENGINE_EXTENSION;
        } else {
            ext = scriptUrl.getPath().substring(lastPeriod + 1);
        }
        final Fun.ThrowingSupplier<ScriptEngine> engineSupplier = () -> {
            final ScriptEngineManager scriptEngineManager = getScriptEngineManager(classLoader);
            final ScriptEngine engine;
            if (useJavaScriptEngine(ext)) {
                engine = getEngineForJavaScript(scriptEngineManager);
                final Bindings scriptBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
                scriptBindings.put("polyglot.js.nashorn-compat", true);
                scriptBindings.put("polyglot.engine.WarnInterpreterOnly", false);
            } else {
                engine = scriptEngineManager.getEngineByExtension(ext);
            }
            if (engine == null) {
                throw new UnregisteredScriptEngineNameException(ext,
                        "Failed to find a ScriptEngine for URL extension: " + ext + " - " + scriptUrl);
            }
            return engine;
        };

        return new ScriptProgressCheckFactory(engineSupplier, scriptUrl);
    }

    @SuppressWarnings("WeakerAccess")
    public static class UnregisteredScriptEngineNameException extends Exception {
        private final String engineName;

        UnregisteredScriptEngineNameException(final String engineName, final String message) {
            super(message);
            this.engineName = engineName;
        }

        UnregisteredScriptEngineNameException(final String engineName) {
            super("Failed to load ScriptEngine by name: " + engineName);
            this.engineName = engineName;
        }

        @SuppressWarnings("WeakerAccess")
        public String getEngineName() {
            return engineName;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static ProgressCheckFactory createScriptCheckFactory(final @NotNull String engineName,
                                                                final @NotNull URL scriptUrl)
            throws UnregisteredScriptEngineNameException {
        return createClassLoaderScriptCheckFactory(engineName, scriptUrl, Util.getDefaultClassLoader());
    }

    @SuppressWarnings("WeakerAccess")
    static ProgressCheckFactory createClassLoaderScriptCheckFactory(final @NotNull String engineName,
                                                                    final @NotNull URL scriptUrl,
                                                                    final @Nullable ClassLoader classLoader)
            throws UnregisteredScriptEngineNameException {
        final ScriptEngineManager scriptEngineManager = getScriptEngineManager(classLoader);
        final ScriptEngine engine = useJavaScriptEngine(engineName)
                ? getEngineForJavaScript(scriptEngineManager)
                : scriptEngineManager.getEngineByName(engineName);
        if (engine == null) {
            throw new UnregisteredScriptEngineNameException(engineName);
        }
        return createScriptCheckFactory(engine, scriptUrl);
    }

    static ScriptEngineManager getScriptEngineManager(final @Nullable ClassLoader classLoader) {
        return new ScriptEngineManager(classLoader);
    }

    static ScriptEngine getEngineForJavaScript(final @NotNull ScriptEngineManager scriptEngineManager) {
        return USE_NASHORN
                ? scriptEngineManager.getEngineByName(ENGINE_NASHORN)
                : scriptEngineManager.getEngineByName(ENGINE_GRAALJS);
    }

    @SuppressWarnings("WeakerAccess")
    public static ProgressCheckFactory createScriptCheckFactory(final @NotNull ScriptEngine engine,
                                                                final @NotNull URL scriptUrl) {
        return new ScriptProgressCheckFactory(() -> engine, scriptUrl);
    }

    @SuppressWarnings("WeakerAccess")
    public static ProgressCheckFactory createInlineScriptCheckFactory(final @NotNull String inlineScript,
                                                                      final @Nullable String inlineEngine) {
        return createClassLoaderInlineScriptCheckFactory(inlineScript, inlineEngine, Util.getDefaultClassLoader());
    }

    @SuppressWarnings("WeakerAccess")
    static ProgressCheckFactory createClassLoaderInlineScriptCheckFactory(final @NotNull String inlineScript,
                                                                          final @Nullable String inlineEngine,
                                                                          final @Nullable ClassLoader classLoader) {

        final Fun.ThrowingSupplier<ScriptEngine> engineSupplier = () -> {
            final ScriptEngine engine;
            if (isEmpty(inlineEngine)) {
                engine = getEngineForJavaScript(getScriptEngineManager(classLoader));
                final Bindings scriptBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
                scriptBindings.put("polyglot.js.nashorn-compat", true);
                scriptBindings.put("polyglot.engine.WarnInterpreterOnly", false);
            } else {
                engine = getScriptEngineManager(classLoader).getEngineByName(inlineEngine);
            }
            if (engine == null) {
                throw new UnregisteredScriptEngineNameException(inlineEngine);
            }
            return engine;
        };

        return new InlineScriptProgressCheckFactory(engineSupplier, inlineScript);
    }
}
