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

import static net.adamcin.oakpal.core.Util.isEmpty;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import javax.script.SimpleBindings;

import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.json.JSONObject;

/**
 * The {@link ScriptProgressCheck} uses the {@link Invocable} interface from JSR223 to listen for scan events and
 * optionally report check violations.
 * <p>
 * You may implement only the methods you need to enforce your package check rules.
 * <dl>
 * <dt>getCheckName()</dt>
 * <dd>{@link ProgressCheck#getCheckName()}</dd>
 * <dt>startedScan()</dt>
 * <dd>{@link ProgressCheck#startedScan()}</dd>
 * <dt>identifyPackage(packageId, packageFile)</dt>
 * <dd>{@link ProgressCheck#identifyPackage(PackageId, File)}</dd>
 * <dt>identifySubpackage(packageId, parentPackageId)</dt>
 * <dd>{@link ProgressCheck#identifySubpackage(PackageId, PackageId)}</dd>
 * <dt>beforeExtract(packageId, inspectSession, packageProperties, metaInf, subpackageIds)</dt>
 * <dd>{@link ProgressCheck#beforeExtract(PackageId, Session, PackageProperties, MetaInf, List)}</dd>
 * <dt>importedPath(packageId, path, node)</dt>
 * <dd>{@link ProgressCheck#importedPath(PackageId, String, Node)}</dd>
 * <dt>deletedPath(packageId, path, inspectSession)</dt>
 * <dd>{@link ProgressCheck#deletedPath(PackageId, String, Session)}</dd>
 * <dt>afterExtract(packageId, inspectSession)</dt>
 * <dd>{@link ProgressCheck#afterExtract(PackageId, Session)}</dd>
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
    public static final String INVOKE_ON_STARTED_SCAN = "startedScan";
    public static final String INVOKE_ON_IDENTIFY_PACKAGE = "identifyPackage";
    public static final String INVOKE_ON_IDENTIFY_SUBPACKAGE = "identifySubpackage";
    public static final String INVOKE_ON_BEFORE_EXTRACT = "beforeExtract";
    public static final String INVOKE_ON_IMPORTED_PATH = "importedPath";
    public static final String INVOKE_ON_DELETED_PATH = "deletedPath";
    public static final String INVOKE_ON_AFTER_EXTRACT = "afterExtract";
    public static final String INVOKE_ON_FINISHED_SCAN = "finishedScan";
    public static final String INVOKE_GET_CHECK_NAME = "getCheckName";

    private final Invocable script;
    private final ScriptHelper helper;
    private final URL scriptUrl;
    private final Set<String> handlerMissCache = new HashSet<>();

    private ScriptProgressCheck(final Invocable script, final ScriptHelper helper, final URL scriptUrl) {
        this.script = script;
        this.helper = helper;
        this.scriptUrl = scriptUrl;
    }

    private String getFilename() {
        if (this.scriptUrl != null) {
            final int lastSlash = this.scriptUrl.getFile().lastIndexOf("/");
            if (lastSlash >= 0 && this.scriptUrl.getFile().length() > lastSlash + 1) {
                return this.scriptUrl.getFile().substring(lastSlash + 1);
            } else {
                return this.scriptUrl.getFile();
            }
        } else {
            return "_inlineScript_.js";
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
    private interface HandlerHandle {
        void apply(Object... args) throws NoSuchMethodException, ScriptException;
    }

    /**
     * Lambda type for event handler body logic to eliminate boilerplate exception handling.
     */
    @FunctionalInterface
    private interface EventHandlerBody {
        void apply(HandlerHandle handle) throws NoSuchMethodException, ScriptException;
    }

    /**
     * Guards against script handler calls by remembering when NoSuchMethodExceptions are thrown when the script
     * function named by the {@code methodName} argument is invoked.
     *
     * @param methodName the name of the handler function to invoke
     * @param body       the ScriptProgressCheck adapter body logic to execute
     */
    private void guardHandler(final String methodName, final EventHandlerBody body) {
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
    private void guardSessionHandler(final String methodName, final EventHandlerBody body) throws RepositoryException {
        if (!handlerMissCache.contains(methodName)) {
            try {
                body.apply((args) -> this.script.invokeFunction(methodName, args));
            } catch (NoSuchMethodException ignored) {
                handlerMissCache.add(methodName);
            } catch (ScriptException e) {
                if (e.getCause() instanceof RepositoryException) {
                    throw new ScriptRepositoryException(e);
                }
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void startedScan() {
        guardHandler(INVOKE_ON_STARTED_SCAN, handle -> {
            helper.collector.clearViolations();
            handle.apply();
        });
    }

    @Override
    public void identifyPackage(final PackageId packageId, final File file) {
        guardHandler(INVOKE_ON_IDENTIFY_PACKAGE, handle -> handle.apply(packageId, file));
    }

    @Override
    public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
        guardHandler(INVOKE_ON_IDENTIFY_SUBPACKAGE, handle -> handle.apply(packageId, parentId));
    }

    @Override
    public void beforeExtract(final PackageId packageId, final Session inspectSession,
                              final PackageProperties packageProperties, final MetaInf metaInf,
                              final List<PackageId> subpackages) throws RepositoryException {
        guardSessionHandler(INVOKE_ON_BEFORE_EXTRACT, handle -> handle.apply(inspectSession, packageId, packageProperties,
                metaInf, subpackages.toArray(new PackageId[subpackages.size()])));
    }

    @Override
    public void importedPath(final PackageId packageId, final String path, final Node node) throws RepositoryException {
        guardSessionHandler(INVOKE_ON_IMPORTED_PATH, handle -> handle.apply(packageId, path, node));
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
    public void finishedScan() {
        guardHandler(INVOKE_ON_FINISHED_SCAN, handle -> handle.apply());
    }

    @Override
    public final Collection<Violation> getReportedViolations() {
        return this.helper.collector.getReportedViolations();
    }

    public static class ScriptHelper {
        private final ReportCollector collector = new ReportCollector();

        public void minorViolation(String description, PackageId... packageIds) {
            collector.reportViolation(new SimpleViolation(Violation.Severity.MINOR, description, packageIds));
        }

        public void majorViolation(String description, PackageId... packageIds) {
            collector.reportViolation(new SimpleViolation(Violation.Severity.MAJOR, description, packageIds));
        }

        public void severeViolation(String description, PackageId... packageIds) {
            collector.reportViolation(new SimpleViolation(Violation.Severity.SEVERE, description, packageIds));
        }
    }

    /**
     * Internal {@link ProgressCheckFactory} impl for script check creation.
     */
    private static class ScriptProgressCheckFactory implements ProgressCheckFactory {

        private final ScriptEngine engine;
        private final URL scriptUrl;

        private ScriptProgressCheckFactory(final ScriptEngine engine, final URL scriptUrl) {
            this.engine = engine;
            this.scriptUrl = scriptUrl;
        }

        @Override
        public ProgressCheck newInstance(final JsonObject config) throws Exception {
            try (InputStream is = scriptUrl.openStream()) {
                Bindings scriptBindings = new SimpleBindings();
                if (config != null) {
                    scriptBindings.put(BINDING_CHECK_CONFIG, JavaxJson.unwrapObject(config));
                } else {
                    scriptBindings.put(BINDING_CHECK_CONFIG, Collections.<String, Object>emptyMap());
                }
                final ScriptHelper helper = new ScriptHelper();
                scriptBindings.put(BINDING_SCRIPT_HELPER, helper);

                engine.setBindings(scriptBindings, ScriptContext.ENGINE_SCOPE);
                engine.eval(new InputStreamReader(is, Charset.forName("UTF-8")));
                return new ScriptProgressCheck((Invocable) engine, helper, scriptUrl);
            }
        }
    }

    private static class InlineScriptProgressCheckFactory implements ProgressCheckFactory {
        private ScriptEngine engine;
        private final String source;

        private InlineScriptProgressCheckFactory(final ScriptEngine engine, final String source) {
            this.engine = engine;
            this.source = source;
        }

        @Override
        public ProgressCheck newInstance(final JsonObject config) throws Exception {
            Bindings scriptBindings = new SimpleBindings();
            if (config != null) {
                scriptBindings.put(BINDING_CHECK_CONFIG, JavaxJson.unwrapObject(config));
            } else {
                scriptBindings.put(BINDING_CHECK_CONFIG, Collections.<String, Object>emptyMap());
            }
            final ScriptHelper helper = new ScriptHelper();
            scriptBindings.put(BINDING_SCRIPT_HELPER, helper);

            engine.setBindings(scriptBindings, ScriptContext.ENGINE_SCOPE);
            engine.eval(this.source);
            return new ScriptProgressCheck((Invocable) engine, helper, null);
        }
    }

    public static ProgressCheckFactory createScriptCheckFactory(final URL scriptUrl) throws Exception {
        final int lastPeriod = scriptUrl.getPath().lastIndexOf(".");
        final String ext;
        if (lastPeriod < 0 || lastPeriod + 1 >= scriptUrl.getPath().length()) {
            ext = DEFAULT_SCRIPT_ENGINE_EXTENSION;
        } else {
            ext = scriptUrl.getPath().substring(lastPeriod + 1);
        }
        ScriptEngine engine = new ScriptEngineManager().getEngineByExtension(ext);
        if (engine == null) {
            throw new Exception("Failed to find a ScriptEngine for URL extension: " + scriptUrl.toString());
        }
        return createScriptCheckFactory(engine, scriptUrl);
    }

    public static ProgressCheckFactory createScriptCheckFactory(final String engineName, final URL scriptUrl)
            throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName(engineName);
        if (engine == null) {
            throw new Exception("Failed to load ScriptEngine by name: " + engineName);
        }
        return createScriptCheckFactory(engine, scriptUrl);
    }

    public static ProgressCheckFactory createScriptCheckFactory(final ScriptEngine engine, final URL scriptUrl)
            throws Exception {
        return new ScriptProgressCheckFactory(engine, scriptUrl);
    }

    public static ProgressCheckFactory createInlineScriptCheckFactory(final String inlineScript, final String inlineEngine)
            throws Exception {
        final ScriptEngine engine;
        if (isEmpty(inlineEngine)) {
            engine = new ScriptEngineManager().getEngineByExtension(DEFAULT_SCRIPT_ENGINE_EXTENSION);
        } else {
            engine = new ScriptEngineManager().getEngineByName(inlineEngine);
        }

        return new InlineScriptProgressCheckFactory(engine, inlineScript);
    }
}
