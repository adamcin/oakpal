/*
 * Copyright 2017 Mark Adamcin
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

/**
 * The {@link ScriptPackageCheck} uses the {@link Invocable} interface from JSR223 to listen for scan events and optionally
 * report check violations.
 * <p/>
 * You may implement only the methods you need to enforce your package check rules.
 * <dl>
 *     <dt>getCheckName()</dt>
 *     <dd>{@link PackageCheck#getCheckName()}</dd>
 *     <dt>startedScan()</dt>
 *     <dd>{@link PackageCheck#startedScan()}</dd>
 *     <dt>identifyPackage(packageId, packageFile)</dt>
 *     <dd>{@link PackageCheck#identifyPackage(PackageId, File)}</dd>
 *     <dt>identifySubpackage(packageId, parentPackageId)</dt>
 *     <dd>{@link PackageCheck#identifySubpackage(PackageId, PackageId)}</dd>
 *     <dt>beforeExtract()</dt>
 *     <dd>{@link PackageCheck#beforeExtract(PackageId, PackageProperties, MetaInf, List)}</dd>
 *     <dt>importedPath()</dt>
 *     <dd>{@link PackageCheck#importedPath(PackageId, String, Node)}</dd>
 *     <dt>deletedPath()</dt>
 *     <dd>{@link PackageCheck#deletedPath(PackageId, String)}</dd>
 *     <dt>afterExtract()</dt>
 *     <dd>{@link PackageCheck#afterExtract(PackageId, Session)}</dd>
 *     <dt>finishedScan()</dt>
 *     <dd>{@link PackageCheck#finishedScan()}</dd>
 * </dl>
 * <p/>
 * To report package violations, a {@link ScriptHelper} is bound to the global variable "oakpal".
 *
 */
@ProviderType
public final class ScriptPackageCheck implements PackageCheck {
    public static final String BINDING_SCRIPT_HELPER = "oakpal";
    public static final String INVOKE_ON_BEGIN_SCAN = "startedScan";
    public static final String INVOKE_ON_BEGIN_PACKAGE = "identifyPackage";
    public static final String INVOKE_ON_BEGIN_SUBPACKAGE = "identifySubpackage";
    public static final String INVOKE_ON_OPEN = "beforeExtract";
    public static final String INVOKE_ON_IMPORT_PATH = "importedPath";
    public static final String INVOKE_ON_DELETE_PATH = "deletedPath";
    public static final String INVOKE_ON_CLOSE = "afterExtract";
    public static final String INVOKE_ON_END_SCAN = "finishedScan";
    public static final String INVOKE_GET_LABEL = "getCheckName";

    private final Invocable script;
    private final ScriptHelper helper;
    private final URL scriptUrl;

    private ScriptPackageCheck(final Invocable script, final ScriptHelper helper, final URL scriptUrl) {
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
            Object result = this.script.invokeFunction(INVOKE_GET_LABEL);
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
            this.script.invokeFunction(INVOKE_ON_BEGIN_SCAN);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void identifyPackage(PackageId packageId, File file) {
        try {
            this.script.invokeFunction(INVOKE_ON_BEGIN_PACKAGE, packageId, file);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void identifySubpackage(PackageId packageId, PackageId parentId) {
        try {
            this.script.invokeFunction(INVOKE_ON_BEGIN_SUBPACKAGE, packageId, parentId);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeExtract(PackageId packageId, PackageProperties packageProperties, MetaInf metaInf, List<PackageId> subpackages) {
        try {
            this.script.invokeFunction(INVOKE_ON_OPEN, packageId, packageProperties,
                    metaInf, subpackages.toArray(new PackageId[subpackages.size()]));
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void importedPath(PackageId packageId, String path, Node node) throws RepositoryException {
        try {
            this.script.invokeFunction(INVOKE_ON_IMPORT_PATH, packageId, path, node);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            if (e.getCause() instanceof RepositoryException) {
                throw (RepositoryException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deletedPath(PackageId packageId, String path) {
        try {
            this.script.invokeFunction(INVOKE_ON_DELETE_PATH, packageId, path);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterExtract(PackageId packageId, Session inspectSession) throws RepositoryException {
        try {
            this.script.invokeFunction(INVOKE_ON_CLOSE, packageId, inspectSession);
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
            this.script.invokeFunction(INVOKE_ON_END_SCAN);
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

    public static ScriptPackageCheck createScriptListener(final URL scriptUrl) throws Exception {
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
        return createScriptListener(engine, scriptUrl);
    }

    public static ScriptPackageCheck createScriptListener(String engineName, URL scriptUrl) throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName(engineName);
        if (engine == null) {
            throw new Exception("Failed to load ScriptEngine by name: " + engineName);
        }
        return createScriptListener(engine, scriptUrl);
    }

    public static ScriptPackageCheck createScriptListener(ScriptEngine engine, URL scriptUrl) throws Exception {
        try (InputStream is = scriptUrl.openStream()) {
            Bindings scriptBindings = new SimpleBindings();
            final ScriptHelper helper = new ScriptHelper();
            scriptBindings.put(BINDING_SCRIPT_HELPER, helper);

            engine.setBindings(scriptBindings, ScriptContext.ENGINE_SCOPE);
            engine.eval(new InputStreamReader(is, Charset.forName("UTF-8")));
            return new ScriptPackageCheck((Invocable) engine, helper, scriptUrl);
        }
    }
}
