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

package net.adamcin.oakpal.webster.targets;

import net.adamcin.oakpal.core.JcrNs;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.webster.TestUtil;
import net.adamcin.oakpal.webster.WebsterTarget;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.vault.fs.io.FileArchive;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.*;

public class WebsterNodetypesTargetTest {
    final File testOutDir = new File("target/test-out/WebsterNodetypesTargetTest");

    @Before
    public void setUp() throws Exception {
        testOutDir.mkdirs();
    }

    @Test
    public void testFromJsonTargetFactory() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJsonTargetFactory.json");
        WebsterTarget target = JsonTargetFactory.NODETYPES.createTarget(targetFile, obj().get());
        assertTrue("target is nodetypes target: " + target.getClass().getSimpleName(),
                target instanceof WebsterNodetypesTarget);
    }

    @Test
    public void testFromJson() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson.cnd");
        final File writeBack = new File(testOutDir, "testFromJson_writeback");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (writeBack.exists()) {
            FileUtils.deleteDirectory(writeBack);
        }
        OakMachine.Builder builder = TestUtil.fromPlan(obj().get());
        WebsterNodetypesTarget target = WebsterNodetypesTarget.fromJson(targetFile, obj().get());
        target.setArchive(new FileArchive(new File("src/test/resources/filevault/noReferences")), writeBack);
        builder.build().initAndInspect(target::perform);
    }

    static void assertFileNotContains(final @NotNull File haystack, final @NotNull String needle)
            throws Exception {
        final String contents = FileUtils.readFileToString(haystack, StandardCharsets.UTF_8);
        assertFalse(haystack.getName() + " not contains '" + needle + "'",
                contents.contains(needle));
    }

    @Test
    public void testFromJson_scopeExportNames() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson_scopeExportNames.cnd");
        final File writeBack = new File(testOutDir, "testFromJson_scopeExportNames_writeback");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (writeBack.exists()) {
            FileUtils.deleteDirectory(writeBack);
        }
        OakMachine.Builder builder = TestUtil.fromPlan(obj()
                .key("jcrNamespaces", arr().val(JcrNs.create("foo", "http://adamcin.net/foo")))
                .key("jcrNodetypes", obj()
                        .key("foo:bar",
                                key("extends", arr("nt:folder"))))
                .get());
        WebsterNodetypesTarget target = WebsterNodetypesTarget.fromJson(targetFile, obj()
                .key("scopeExportNames", arr().val(new Rule(Rule.RuleType.INCLUDE, Pattern.compile("foo:.*"))))
                .get());
        target.setArchive(new FileArchive(new File("src/test/resources/filevault/oneNtRef")), writeBack);
        builder.build().initAndInspect(target::perform);

        TestUtil.assertFileContains(targetFile, "<'nt'=");
        TestUtil.assertFileContains(targetFile, "<'foo'=");
        TestUtil.assertFileContains(targetFile, "[foo:bar]");
    }

    @Test(expected = NoSuchNodeTypeException.class)
    public void testFromJson_scopeExportNames_throws() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson_scopeExportNames_throws.cnd");
        final File writeBack = new File(testOutDir, "testFromJson_scopeExportNames_throws_writeback");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (writeBack.exists()) {
            FileUtils.deleteDirectory(writeBack);
        }
        OakMachine.Builder builder = TestUtil.fromPlan(obj()
                .key("jcrNamespaces", arr().val(JcrNs.create("foo", "http://adamcin.net/foo")))
                .get());
        WebsterNodetypesTarget target = WebsterNodetypesTarget.fromJson(targetFile, obj()
                .key("scopeExportNames", arr().val(new Rule(Rule.RuleType.INCLUDE, Pattern.compile("foo:.*"))))
                .get());
        target.setArchive(new FileArchive(new File("src/test/resources/filevault/oneNtRef")), writeBack);
        builder.build().initAndInspect(target::perform);
    }

    @Test
    public void testFromJson_scopeReplaceNames() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson_scopeReplaceNames.cnd");
        FileUtils.copyFile(new File("src/test/resources/sling_nodetypes.cnd"), targetFile);
        final File writeBack = new File(testOutDir, "testFromJson_scopeReplaceNames_writeback");
        if (writeBack.exists()) {
            FileUtils.deleteDirectory(writeBack);
        }
        OakMachine.Builder builder = TestUtil.fromPlan(obj()
                .key("jcrNamespaces", arr()
                        .val(JcrNs.create("sling", "http://sling.apache.org/jcr/sling/1.0")))
                .key("jcrNodetypes", obj()
                        .key("sling:Folder",
                                key("extends", arr("nt:hierarchyNode")))
                        .key("sling:OrderedFolder",
                                key("extends", arr("nt:folder")))
                )
                .get());
        WebsterNodetypesTarget targetJustOrdered = WebsterNodetypesTarget.fromJson(targetFile, obj()
                .key("scopeReplaceNames", arr().val(new Rule(Rule.RuleType.INCLUDE, Pattern.compile("sling:OrderedFolder"))))
                .get());
        targetJustOrdered.setArchive(new FileArchive(new File("src/test/resources/filevault/ntRefsSlingFolderOrderedFolder")), writeBack);
        builder.build().initAndInspect(targetJustOrdered::perform);

        // sling:Folder is referenced, but not in scopeReplaceNames
        TestUtil.assertFileContains(targetFile, "[sling:Folder] > nt:folder");
        TestUtil.assertFileContains(targetFile, "[sling:OrderedFolder] > nt:folder");

        WebsterNodetypesTarget targetReplaceAll = WebsterNodetypesTarget.fromJson(targetFile, obj()
                .get());
        targetReplaceAll.setArchive(new FileArchive(new File("src/test/resources/filevault/ntRefsSlingFolderOrderedFolder")), writeBack);
        builder.build().initAndInspect(targetReplaceAll::perform);

        // sling:Folder is referenced, and now included in scopeReplaceNames
        TestUtil.assertFileContains(targetFile, "[sling:Folder] > nt:hierarchyNode");
        TestUtil.assertFileContains(targetFile, "[sling:OrderedFolder] > nt:folder");
    }

    @Test
    public void testFromJson_scopeReplaceNames_noFailOnNoMissingDefDependencies() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson_scopeReplaceNames_noFailOnMissingDefDependencies.cnd");
        FileUtils.copyFile(new File("src/test/resources/sling_nodetypes.cnd"), targetFile);
        final File writeBack = new File(testOutDir, "testFromJson_scopeReplaceNames_noFailOnMissingDefDependencies_writeback");
        if (writeBack.exists()) {
            FileUtils.deleteDirectory(writeBack);
        }
        OakMachine.Builder builder = TestUtil.fromPlan(obj()
                .key("jcrNamespaces", arr()
                        .val(JcrNs.create("sling", "http://sling.apache.org/jcr/sling/1.0")))
                .get());
        WebsterNodetypesTarget target = WebsterNodetypesTarget.fromJson(targetFile, obj()
                .get());
        target.setArchive(new FileArchive(new File("src/test/resources/filevault/noReferences")), writeBack);
        builder.build().initAndInspect(target::perform);
    }

    @Test
    public void testFromJson_includeBuiltins() throws Exception {
        final File targetFile = new File(testOutDir, "testFromJson_includeBuiltins.cnd");
        final File writeBack = new File(testOutDir, "testFromJson_includeBuiltins_writeback");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (writeBack.exists()) {
            FileUtils.deleteDirectory(writeBack);
        }
        OakMachine.Builder builder = TestUtil.fromPlan(obj()
                .key("jcrNamespaces", arr().val(JcrNs.create("foo", "http://adamcin.net/foo")))
                .key("jcrNodetypes", obj()
                        .key("foo:bar",
                                key("extends", arr("nt:folder"))))
                .get());

        WebsterNodetypesTarget targetWithoutBuiltins = WebsterNodetypesTarget.fromJson(targetFile, obj()
                .key("includeBuiltins", false)
                .get());
        targetWithoutBuiltins.setArchive(new FileArchive(new File("src/test/resources/filevault/oneNtRef")), writeBack);
        builder.build().initAndInspect(targetWithoutBuiltins::perform);

        TestUtil.assertFileContains(targetFile, "[foo:bar]");
        assertFileNotContains(targetFile, "[nt:folder]");

        WebsterNodetypesTarget targetWithBuiltins = WebsterNodetypesTarget.fromJson(targetFile, obj()
                .key("includeBuiltins", true)
                .get());
        targetWithBuiltins.setArchive(new FileArchive(new File("src/test/resources/filevault/oneNtRef")), writeBack);
        builder.build().initAndInspect(targetWithBuiltins::perform);

        TestUtil.assertFileContains(targetFile, "[foo:bar]");
        TestUtil.assertFileContains(targetFile, "[nt:folder]");
    }
}