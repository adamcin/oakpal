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

package net.adamcin.opal.core;

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

import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;

/**
 * Whoa...
 */
public class OpalScriptHandler implements OpalHandler {
    public static final String INVOKE_ON_BEGIN_PACKAGE = "onBeginPackage";
    public static final String INVOKE_ON_BEGIN_SUBPACKAGE = "onBeginSubpackage";
    public static final String INVOKE_ON_OPEN = "onOpen";
    public static final String INVOKE_ON_IMPORT_PATH = "onImportPath";
    public static final String INVOKE_ON_DELETE_PATH = "onDeletePath";
    public static final String INVOKE_ON_CLOSE = "onClose";
    public static final String INVOKE_ON_COMPLETE = "onComplete";

    private final Invocable script;
    private final ScriptViolationReporter reporter;

    public OpalScriptHandler(Invocable script, ScriptViolationReporter reporter) {
        this.script = script;
        this.reporter = reporter;
    }

    @Override
    public void onBeginPackage(PackageId packageId, File file) {
        try {
            this.script.invokeFunction(INVOKE_ON_BEGIN_PACKAGE, packageId, file);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onBeginSubpackage(PackageId packageId, PackageId parentId) {
        try {
            this.script.invokeFunction(INVOKE_ON_BEGIN_SUBPACKAGE, packageId, parentId);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onOpen(PackageId packageId, PackageProperties packageProperties, MetaInf metaInf, List<PackageId> subpackages) {
        try {
            this.script.invokeFunction(INVOKE_ON_OPEN, packageId, packageProperties, metaInf, subpackages);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onImportPath(PackageId packageId, String path, Node node) throws RepositoryException {
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
    public void onDeletePath(PackageId packageId, String path) {
        try {
            this.script.invokeFunction(INVOKE_ON_DELETE_PATH, packageId, path);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClose(PackageId packageId, Session inspectSession) throws RepositoryException {
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
    public Collection<OpalViolation> onComplete() {
        try {
            this.script.invokeFunction(INVOKE_ON_COMPLETE);
        } catch (NoSuchMethodException ignored) {
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
        return this.reporter.onComplete();
    }

    public static class ScriptViolationReporter extends AbstractViolationReporter {
        public void minorViolation(String description, PackageId... packageIds) {
            reportViolation(new SimpleViolation(OpalViolation.Severity.MINOR, description, packageIds));
        }

        public void majorViolation(String description, PackageId... packageIds) {
            reportViolation(new SimpleViolation(OpalViolation.Severity.MAJOR, description, packageIds));
        }

        public void severeViolation(String description, PackageId... packageIds) {
            reportViolation(new SimpleViolation(OpalViolation.Severity.SEVERE, description, packageIds));
        }

        @Override
        protected void beforeComplete() { }
    }

    public static OpalScriptHandler createScriptHandler(String engineName, URL scriptUrl) throws Exception {
        ScriptViolationReporter reporter = new ScriptViolationReporter();

        InputStream is = null;

        try {
            is = scriptUrl.openStream();

            Bindings scriptBindings = new SimpleBindings();
            scriptBindings.put("opal", reporter);

            ScriptEngine engine = new ScriptEngineManager().getEngineByName(engineName);
            engine.setBindings(scriptBindings, ScriptContext.ENGINE_SCOPE);
            engine.eval(new InputStreamReader(is, Charset.forName("UTF-8")));
            return new OpalScriptHandler((Invocable) engine, reporter) ;
        } finally {
            if (is != null) {
                is.close();
            }
        }

    }
}
