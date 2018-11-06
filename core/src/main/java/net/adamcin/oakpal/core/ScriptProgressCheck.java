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
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import aQute.bnd.annotation.ProviderType;
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
@ProviderType
public final class ScriptProgressCheck implements ProgressCheck {
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

    private ScriptProgressCheck(final Invocable script, final ScriptHelper helper, final URL scriptUrl) {
        this.script = script;
        this.helper = helper;
        this.scriptUrl = scriptUrl;
    }

    private String getFilename() {
        final int lastSlash = this.scriptUrl.getFile().lastIndexOf("/");
        if (lastSlash >= 0 && this.scriptUrl.getFile().length() > lastSlash + 1) {
            return this.scriptUrl.getFile().substring(lastSlash + 1);
        } else {
            return this.scriptUrl.getFile();
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

    @Override
    public void startedScan() {
        try {
            helper.collector.clearViolations();
            this.script.invokeFunction(INVOKE_ON_STARTED_SCAN);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void identifyPackage(final PackageId packageId, final File file) {
        try {
            this.script.invokeFunction(INVOKE_ON_IDENTIFY_PACKAGE, packageId, file);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
        try {
            this.script.invokeFunction(INVOKE_ON_IDENTIFY_SUBPACKAGE, packageId, parentId);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeExtract(final PackageId packageId, final Session inspectSession,
                              final PackageProperties packageProperties, final MetaInf metaInf,
                              final List<PackageId> subpackages) throws RepositoryException {
        try {
            this.script.invokeFunction(INVOKE_ON_BEFORE_EXTRACT, inspectSession, packageId, packageProperties,
                    metaInf, subpackages.toArray(new PackageId[subpackages.size()]));
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void importedPath(final PackageId packageId, final String path, final Node node) throws RepositoryException {
        try {
            this.script.invokeFunction(INVOKE_ON_IMPORTED_PATH, packageId, path, node);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            if (e.getCause() instanceof RepositoryException) {
                throw (RepositoryException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deletedPath(final PackageId packageId, final String path, final Session inspectSession)
            throws RepositoryException {
        try {
            this.script.invokeFunction(INVOKE_ON_DELETED_PATH, packageId, path);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterExtract(final PackageId packageId, final Session inspectSession) throws RepositoryException {
        try {
            this.script.invokeFunction(INVOKE_ON_AFTER_EXTRACT, packageId, inspectSession);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            if (e.getCause() instanceof RepositoryException) {
                throw (RepositoryException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finishedScan() {
        try {
            this.script.invokeFunction(INVOKE_ON_FINISHED_SCAN);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
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
        public ProgressCheck newInstance(final JSONObject config) throws Exception {
            try (InputStream is = scriptUrl.openStream()) {
                Bindings scriptBindings = new SimpleBindings();
                if (config != null) {
                    scriptBindings.put(BINDING_CHECK_CONFIG, config.toMap());
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

    public static ProgressCheckFactory createScriptCheckFactory(final URL scriptUrl) throws Exception {
        final int lastPeriod = scriptUrl.getPath().lastIndexOf(".");
        if (lastPeriod < 0 || lastPeriod + 1 >= scriptUrl.getPath().length()) {
            throw new Exception("Failed to load ScriptEngine for URL missing file extension."
                    + scriptUrl.toString());
        }
        final String ext = scriptUrl.getPath().substring(lastPeriod + 1);
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
}
