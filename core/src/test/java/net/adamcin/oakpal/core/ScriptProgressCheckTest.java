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

package net.adamcin.oakpal.core;

import net.adamcin.oakpal.api.PathAction;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.Violation;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.jar.Manifest;

import static net.adamcin.oakpal.api.Fun.toEntry;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ScriptProgressCheckTest {
    final File srcDir = new File("src/test/resources/ScriptProgressCheckTest");

    private URL testScriptUrl(final String relPath) throws Exception {
        return new File(srcDir, relPath).toURI().toURL();
    }

    @Test
    public void testCreateInlineScriptCheckFactory() throws Exception {
        final ProgressCheck check = ScriptProgressCheck
                .createInlineScriptCheckFactory("", "").newInstance(obj().get());
        assertEquals("check filename", ScriptProgressCheck.FILENAME_INLINE_SCRIPT, check.getCheckName());
        final ProgressCheck checkJs = ScriptProgressCheck
                .createInlineScriptCheckFactory("", "js").newInstance(obj().get());
        assertEquals("checkJs filename", ScriptProgressCheck.FILENAME_INLINE_SCRIPT, checkJs.getCheckName());

        final ProgressCheckFactory checkConfigFactory = ScriptProgressCheck
                .createInlineScriptCheckFactory("function getCheckName() { return config.checkNameForTest; }", "js");

        assertEquals("checkConfig filename when null config",
                ScriptProgressCheck.FILENAME_INLINE_SCRIPT, checkConfigFactory.newInstance(null).getCheckName());
        assertEquals("checkConfig foobar when null config",
                "foobar", checkConfigFactory.newInstance(key("checkNameForTest", "foobar").get()).getCheckName());
    }

    @Test(expected = ScriptProgressCheck.UnregisteredScriptEngineNameException.class)
    public void testCreateInlineScriptCheckFactory_unregisteredEngine() throws Exception {
        ScriptProgressCheck.createInlineScriptCheckFactory("", "foobar");
    }

    @Test
    public void testScriptHelper() {
        final PackageId id0 = PackageId.fromString("my_packages:test_0:1.0");
        final PackageId id1 = PackageId.fromString("my_packages:test_1:1.0");
        final PackageId id2 = PackageId.fromString("my_packages:test_3:1.0");

        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(mock(Invocable.class), helper, null);

        helper.minorViolation(id0.toString(), id0);
        helper.majorViolation(id1.toString(), id1);
        helper.severeViolation(id2.toString(), id2);

        final Collection<Violation> violations = check.getReportedViolations();
        assertEquals("violations size", 3, violations.size());

        assertTrue("test_0 minor violation", violations.stream()
                .anyMatch(viol -> viol.getSeverity() == Severity.MINOR && viol.getPackages().contains(id0)));
        assertTrue("test_1 major violation", violations.stream()
                .anyMatch(viol -> viol.getSeverity() == Severity.MAJOR && viol.getPackages().contains(id1)));
        assertTrue("test_2 severe violation", violations.stream()
                .anyMatch(viol -> viol.getSeverity() == Severity.SEVERE && viol.getPackages().contains(id2)));

        check.startedScan();

        assertTrue("violations empty", check.getReportedViolations().isEmpty());
    }

    @Test
    public void testRawConstructorWithFilename() throws Exception {
        assertEquals("check filename should be", "noslash.js",
                new ScriptProgressCheck(mock(Invocable.class),
                        mock(ScriptProgressCheck.ScriptHelper.class),
                        new URL("file:noslash.js")).getCheckName());
    }

    @Test(expected = RuntimeException.class)
    public void testGetCheckName_throws() throws Exception {
        final ProgressCheck check = ScriptProgressCheck
                .createScriptCheckFactory(testScriptUrl("checkNameThrows.js")).newInstance(obj().get());
        check.getCheckName();
    }

    @Test
    public void testCreateScriptCheckFactory_checkNameIsFilename() throws Exception {
        final String filename = "checkNoName.js";
        final ProgressCheck check = ScriptProgressCheck
                .createScriptCheckFactory(testScriptUrl(filename)).newInstance(obj().get());
        assertEquals("check filename", filename, check.getCheckName());
        final ProgressCheck checkJs = ScriptProgressCheck
                .createScriptCheckFactory("js", testScriptUrl(filename)).newInstance(obj().get());
        assertEquals("checkJs filename", filename, checkJs.getCheckName());
        final ProgressCheck checkEngine = ScriptProgressCheck
                .createScriptCheckFactory(new ScriptEngineManager().getEngineByExtension("js"),
                        testScriptUrl(filename)).newInstance(obj().get());
        assertEquals("checkEngine filename", filename, checkEngine.getCheckName());
    }

    @Test
    public void testCreateScriptCheckFactory_noExt() throws Exception {
        final String filename = "checkNoExt";
        final ProgressCheckFactory checkFactory = ScriptProgressCheck
                .createScriptCheckFactory(testScriptUrl(filename));
        assertTrue("is instance of ScriptProgressCheckFactory",
                checkFactory instanceof ScriptProgressCheck.ScriptProgressCheckFactory);
        ScriptProgressCheck.ScriptProgressCheckFactory scriptCheckFactory =
                (ScriptProgressCheck.ScriptProgressCheckFactory) checkFactory;
        assertTrue("engine should support js",
                scriptCheckFactory.getEngine().getFactory().getExtensions().contains(ScriptProgressCheck.DEFAULT_SCRIPT_ENGINE_EXTENSION));
        assertEquals("check name should be", filename,
                scriptCheckFactory.newInstance(null).getCheckName());
    }

    @Test
    public void testCreateScriptCheckFactory_checkNameIsDefined() throws Exception {
        final String filename = "checkWithName.js";
        final String checkName = "some check";
        final ProgressCheck check = ScriptProgressCheck
                .createScriptCheckFactory(testScriptUrl(filename)).newInstance(obj().get());
        assertEquals("check filename", checkName, check.getCheckName());
        final ProgressCheck checkJs = ScriptProgressCheck
                .createScriptCheckFactory("js", testScriptUrl(filename)).newInstance(obj().get());
        assertEquals("checkJs filename", checkName, checkJs.getCheckName());
        final ProgressCheck checkEngine = ScriptProgressCheck
                .createScriptCheckFactory(new ScriptEngineManager().getEngineByExtension("js"),
                        testScriptUrl(filename)).newInstance(obj().get());
        assertEquals("checkEngine filename", checkName, checkEngine.getCheckName());
    }

    @Test(expected = ScriptProgressCheck.UnregisteredScriptEngineNameException.class)
    public void testCreateScriptCheckFactory_unregisteredEngine_byName() throws Exception {
        ScriptProgressCheck.createScriptCheckFactory("foobar",
                new File(srcDir, "bogus_script.foobar").toURI().toURL());
    }

    @Test(expected = ScriptProgressCheck.UnregisteredScriptEngineNameException.class)
    public void testCreateScriptCheckFactory_unregisteredEngine_byURL() throws Exception {
        ScriptProgressCheck.createScriptCheckFactory(new File(srcDir, "bogus_script.foobar").toURI().toURL());
    }

    @Test
    public void testUnregisteredScriptEngineNameException_getEngineName() {
        assertEquals("same name", "foobar",
                new ScriptProgressCheck.UnregisteredScriptEngineNameException("foobar").getEngineName());
        assertEquals("same name with message", "foobar",
                new ScriptProgressCheck.UnregisteredScriptEngineNameException("foobar", "some message").getEngineName());
    }

    @Test
    public void testScriptProgressCheckFactory_newInstance() throws Exception {
        final ProgressCheck checkWithName = ScriptProgressCheck
                .createScriptCheckFactory(testScriptUrl("checkWithName.js")).newInstance(null);
        assertEquals("check name should be", "some check", checkWithName.getCheckName());
        final ProgressCheck checkNameFromConfig = ScriptProgressCheck
                .createScriptCheckFactory(testScriptUrl("checkNameFromConfig.js")).newInstance(key("checkNameForTest", "foobar").get());
        assertEquals("check name should be", "foobar", checkNameFromConfig.getCheckName());
        final ProgressCheck checkNameFromNullConfig = ScriptProgressCheck
                .createScriptCheckFactory(testScriptUrl("checkNameFromConfig.js")).newInstance(null);
        assertEquals("check name should be", "checkNameFromConfig.js", checkNameFromNullConfig.getCheckName());
    }

    @Test
    public void testStartedScan_invokeWithArgs() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, helper, null);
        final List<Map.Entry<String, Object[]>> argRecord = new ArrayList<>();
        doAnswer(call -> argRecord.add(toEntry(call.getArgument(0), call.getArguments())))
                .when(delegate).invokeFunction(anyString(), any());
        check.startedScan();
        assertTrue("expect args for startedScan", argRecord.stream()
                .anyMatch(entry -> "startedScan".equals(entry.getKey()) && entry.getValue().length == 1));
    }

    @Test
    public void testFinishedScan_invokeWithArgs() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, helper, null);
        final List<Map.Entry<String, Object[]>> argRecord = new ArrayList<>();
        doAnswer(call -> argRecord.add(toEntry(call.getArgument(0), call.getArguments())))
                .when(delegate).invokeFunction(anyString(), any());
        check.finishedScan();
        assertTrue("expect args for finishedScan", argRecord.stream()
                .anyMatch(entry -> "finishedScan".equals(entry.getKey()) && entry.getValue().length == 1));
    }

    @Test
    public void testIdentifyPackage() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, helper, null);

        final List<Map.Entry<String, Object[]>> argRecord = new ArrayList<>();
        doAnswer(call -> argRecord.add(toEntry(call.getArgument(0), call.getArguments())))
                .when(delegate).invokeFunction(anyString(), any());

        final PackageId arg1 = PackageId.fromString("my_packages:example:1.0");
        final File arg2 = new File("./foo");

        check.identifyPackage(arg1, arg2);

        Map.Entry<String, Object[]> call = argRecord.stream()
                .filter(entry -> "identifyPackage".equals(entry.getKey()) && entry.getValue().length == 3).findFirst()
                .orElse(null);
        assertNotNull("expect call for identifyPackage", call);
        assertSame("same arg1", arg1, call.getValue()[1]);
        assertSame("same arg2", arg2, call.getValue()[2]);
    }

    @Test
    public void testIdentifySubpackage() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, helper, null);

        final List<Map.Entry<String, Object[]>> argRecord = new ArrayList<>();
        doAnswer(call -> argRecord.add(toEntry(call.getArgument(0), call.getArguments())))
                .when(delegate).invokeFunction(anyString(), any());

        final PackageId arg1 = PackageId.fromString("my_packages:example:1.0");
        final PackageId arg2 = PackageId.fromString("my_packages:other-example:1.0");
        final String arg3Deprecated = arg1.getInstallationPath() + ".zip";
        final String arg3 = "/apps/mine/author-packages/install/" + arg1.getDownloadName();

        check.identifySubpackage(arg1, arg2);
        Map.Entry<String, Object[]> callDeprecated = argRecord.stream()
                .filter(entry -> "identifySubpackage".equals(entry.getKey()) && entry.getValue().length == 4).findFirst()
                .orElse(null);
        assertNotNull("expect call for identifySubpackage", callDeprecated);
        assertSame("same arg1", arg1, callDeprecated.getValue()[1]);
        assertSame("same arg2", arg2, callDeprecated.getValue()[2]);
        assertEquals("same arg3", arg3Deprecated, callDeprecated.getValue()[3]);

        argRecord.clear();
        check.identifySubpackage(arg1, arg2, arg3);
        Map.Entry<String, Object[]> call = argRecord.stream()
                .filter(entry -> "identifySubpackage".equals(entry.getKey()) && entry.getValue().length == 4).findFirst()
                .orElse(null);
        assertNotNull("expect call for identifySubpackage", call);
        assertSame("same arg1", arg1, call.getValue()[1]);
        assertSame("same arg2", arg2, call.getValue()[2]);
        assertSame("same arg3", arg3, call.getValue()[3]);
    }

    @Test
    public void testReadManifest() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, helper, null);

        final List<Map.Entry<String, Object[]>> argRecord = new ArrayList<>();
        doAnswer(call -> argRecord.add(toEntry(call.getArgument(0), call.getArguments())))
                .when(delegate).invokeFunction(anyString(), any());

        final PackageId arg1 = PackageId.fromString("my_packages:example:1.0");
        final Manifest arg2 = new Manifest();

        check.readManifest(arg1, arg2);

        Map.Entry<String, Object[]> call = argRecord.stream()
                .filter(entry -> "readManifest".equals(entry.getKey()) && entry.getValue().length == 3).findFirst()
                .orElse(null);
        assertNotNull("expect call for readManifest", call);
        assertSame("same arg1", arg1, call.getValue()[1]);
        assertSame("same arg2", arg2, call.getValue()[2]);
    }

    @Test
    public void testBeforeExtract() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, helper, null);

        final List<Map.Entry<String, Object[]>> argRecord = new ArrayList<>();
        doAnswer(call -> argRecord.add(toEntry(call.getArgument(0), call.getArguments())))
                .when(delegate).invokeFunction(anyString(), any());

        final PackageId arg1 = PackageId.fromString("my_packages:example:1.0");
        final Session arg2 = mock(Session.class);
        final PackageProperties arg3 = mock(PackageProperties.class);
        final MetaInf arg4 = mock(MetaInf.class);
        final List<PackageId> arg5 = Collections.singletonList(PackageId.fromString("my_packages:sub:1.0"));

        check.beforeExtract(arg1, arg2, arg3, arg4, arg5);

        Map.Entry<String, Object[]> call = argRecord.stream()
                .filter(entry -> "beforeExtract".equals(entry.getKey()) && entry.getValue().length == 6).findFirst()
                .orElse(null);
        assertNotNull("expect call for beforeExtract", call);
        assertSame("same arg1", arg1, call.getValue()[1]);
        assertSame("same arg2", arg2, call.getValue()[2]);
        assertSame("same arg3", arg3, call.getValue()[3]);
        assertSame("same arg4", arg4, call.getValue()[4]);
        assertEquals("same arg5", arg5, Arrays.asList((PackageId[]) call.getValue()[5]));
    }

    @Test
    public void testImportedPath() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, helper, null);

        final List<Map.Entry<String, Object[]>> argRecord = new ArrayList<>();
        doAnswer(call -> argRecord.add(toEntry(call.getArgument(0), call.getArguments())))
                .when(delegate).invokeFunction(anyString(), any());

        final PackageId arg1 = PackageId.fromString("my_packages:example:1.0");
        final String arg2 = "/correct/path";
        final Node arg3 = mock(Node.class);
        final PathAction arg4 = PathAction.MODIFIED;

        check.importedPath(arg1, arg2, arg3, arg4);

        Map.Entry<String, Object[]> call = argRecord.stream()
                .filter(entry -> "importedPath".equals(entry.getKey()) && entry.getValue().length == 5).findFirst()
                .orElse(null);
        assertNotNull("expect call for importedPath", call);
        assertSame("same arg1", arg1, call.getValue()[1]);
        assertSame("same arg2", arg2, call.getValue()[2]);
        assertSame("same arg3", arg3, call.getValue()[3]);
        assertSame("same arg4", arg4, call.getValue()[4]);
    }

    @Test
    public void testDeletedPath() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, helper, null);

        final List<Map.Entry<String, Object[]>> argRecord = new ArrayList<>();
        doAnswer(call -> argRecord.add(toEntry(call.getArgument(0), call.getArguments())))
                .when(delegate).invokeFunction(anyString(), any());

        final PackageId arg1 = PackageId.fromString("my_packages:example:1.0");
        final String arg2 = "/correct/path";
        final Session arg3 = mock(Session.class);

        check.deletedPath(arg1, arg2, arg3);

        Map.Entry<String, Object[]> call = argRecord.stream()
                .filter(entry -> "deletedPath".equals(entry.getKey()) && entry.getValue().length == 4).findFirst()
                .orElse(null);
        assertNotNull("expect call for deletedPath", call);
        assertSame("same arg1", arg1, call.getValue()[1]);
        assertSame("same arg2", arg2, call.getValue()[2]);
        assertSame("same arg3", arg3, call.getValue()[3]);
    }

    @Test
    public void testAfterExtract() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        final ScriptProgressCheck.ScriptHelper helper = new ScriptProgressCheck.ScriptHelper();
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, helper, null);

        final List<Map.Entry<String, Object[]>> argRecord = new ArrayList<>();
        doAnswer(call -> argRecord.add(toEntry(call.getArgument(0), call.getArguments())))
                .when(delegate).invokeFunction(anyString(), any());

        final PackageId arg1 = PackageId.fromString("my_packages:example:1.0");
        final Session arg2 = mock(Session.class);

        check.afterExtract(arg1, arg2);

        Map.Entry<String, Object[]> call = argRecord.stream()
                .filter(entry -> "afterExtract".equals(entry.getKey()) && entry.getValue().length == 3).findFirst()
                .orElse(null);
        assertNotNull("expect call for afterExtract", call);
        assertSame("same arg1", arg1, call.getValue()[1]);
        assertSame("same arg2", arg2, call.getValue()[2]);
    }

    @Test(expected = RuntimeException.class)
    public void testGuardHandler_throwsOtherException() throws Exception {
        final ScriptProgressCheck check = (ScriptProgressCheck) ScriptProgressCheck
                .createInlineScriptCheckFactory("function throwScriptException() { return foo.bar(); }", "")
                .newInstance(null);
        check.guardHandler("throwScriptException", ScriptProgressCheck.HandlerHandle::apply);
    }

    @Test
    public void testGuardHandler_cacheMissing() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        doThrow(NoSuchMethodException.class).when(delegate).invokeFunction("missingFunction", "test");
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, new ScriptProgressCheck.ScriptHelper(), null);
        check.guardHandler("missingFunction", handle -> handle.apply("test"));
        // now throw a script exception if called after first missing
        doThrow(ScriptException.class).when(delegate).invokeFunction("missingFunction", "test");
        check.guardHandler("missingFunction", handle -> handle.apply("test"));
    }

    @Test(expected = RepositoryException.class)
    public void testGuardSessionHandler_throwsRepositoryException() throws Exception {
        final ScriptProgressCheck check = (ScriptProgressCheck) ScriptProgressCheck
                .createInlineScriptCheckFactory("function throwRepositoryException() { throw new javax.jcr.RepositoryException(); }", "")
                .newInstance(null);
        check.guardSessionHandler("throwRepositoryException", ScriptProgressCheck.HandlerHandle::apply);
    }

    @Test(expected = RuntimeException.class)
    public void testGuardSessionHandler_throwsOtherException() throws Exception {
        final ScriptProgressCheck check = (ScriptProgressCheck) ScriptProgressCheck
                .createInlineScriptCheckFactory("function throwScriptException() { return foo.bar(); }", "")
                .newInstance(null);
        check.guardSessionHandler("throwScriptException", ScriptProgressCheck.HandlerHandle::apply);
    }

    @Test
    public void testGuardSessionHandler_cacheMissing() throws Exception {
        final Invocable delegate = mock(Invocable.class);
        doThrow(NoSuchMethodException.class).when(delegate).invokeFunction("missingFunction", "test");
        final ScriptProgressCheck check = new ScriptProgressCheck(delegate, new ScriptProgressCheck.ScriptHelper(), null);
        check.guardSessionHandler("missingFunction", handle -> handle.apply("test"));
        // now throw a script exception if called after first missing
        doThrow(ScriptException.class).when(delegate).invokeFunction("missingFunction", "test");
        check.guardSessionHandler("missingFunction", handle -> handle.apply("test"));
    }

    PropertyResourceBundle fromPropertiesUrl(URL propertiesUrl) {
        try (InputStream propsStream = propertiesUrl.openStream()) {
            return new PropertyResourceBundle(propsStream);
        } catch (IOException e) {
            return null;
        }
    }

    @Test
    public void testGetResourceBundleBaseName() throws Exception {
        final ScriptProgressCheck check = (ScriptProgressCheck) ScriptProgressCheck
                .createScriptCheckFactory(testScriptUrl("checkWithResources.js")).newInstance(null);

        assertEquals("expect testKey=testKey", "testKey", check.getHelper().getString("testKey"));

        check.setResourceBundle(fromPropertiesUrl(testScriptUrl("checkWithResources.properties")));

        assertEquals("expect testKey=yeKtset", "yeKtset", check.getHelper().getString("testKey"));

        check.setResourceBundle(fromPropertiesUrl(testScriptUrl("overrideResourceBundle.properties")));

        assertEquals("expect testKey=yeKtsettestKey", "yeKtsettestKey", check.getHelper().getString("testKey"));
    }
}