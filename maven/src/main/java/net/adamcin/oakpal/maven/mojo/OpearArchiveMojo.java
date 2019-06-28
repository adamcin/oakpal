package net.adamcin.oakpal.maven.mojo;

import static net.adamcin.oakpal.core.Fun.composeTest;
import static net.adamcin.oakpal.core.Fun.inSet;
import static net.adamcin.oakpal.core.Fun.testValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.adamcin.oakpal.core.Opear;
import net.adamcin.oakpal.maven.component.OakpalComponentConfigurator;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.jetbrains.annotations.NotNull;

/**
 * Exports the plan builder configuration as an opear (OakPAL Encapsulated Archive) file, and attaches
 * it to the project.
 *
 * @since 1.4.0
 */
@Mojo(name = "opear",
        requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM,
        configurator = OakpalComponentConfigurator.HINT,
        defaultPhase = LifecyclePhase.PACKAGE)
public class OpearArchiveMojo extends AbstractCommonMojo {

    private static final String OAKPAL_GROUP_ID = "net.adamcin.oakpal";
    private static final String OAKPAL_CORE_ARTIFACT_ID = "oakpal-core";

    private static final Predicate<Artifact> TEST_IS_OAKPAL_CORE =
            composeTest(Artifact::getGroupId, OAKPAL_GROUP_ID::equals)
                    .and(composeTest(Artifact::getArtifactId, OAKPAL_CORE_ARTIFACT_ID::equals));

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String finalName;

    @Parameter(defaultValue = "${project.build.directory}/opear-plans", required = true)
    private File plansDirectory;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        resolveDependencies(project.getDependencies(), true);
        try {
            final Artifact projectArtifact = this.project.getArtifact();
            final ArtifactHandler opearHandler = this.artifactHandlerManager.getArtifactHandler("opear");
            final File finalFile = assembleOpear();
            if ("opear".equals(project.getPackaging())) {
                // set project artifact
                projectArtifact.setFile(finalFile);
                projectArtifact.setArtifactHandler(opearHandler);
            } else {
                // attach artifact
                Artifact attachment = new DefaultArtifact(
                        projectArtifact.getGroupId(),
                        projectArtifact.getArtifactId(),
                        projectArtifact.getVersion(),
                        null,
                        "opear",
                        null,
                        opearHandler);
                project.addAttachedArtifact(attachment);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to assemble the opear file.", e);
        }
    }

    String getOwnVersion() {
        final Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("version.properties")) {
            properties.load(input);
            return properties.getProperty("version");
        } catch (IOException e) {
            throw new RuntimeException("failed to read version.properties", e);
        }
    }

    String getBundleSymbolicName() {
        if ("opear".equals(project.getPackaging())) {
            return project.getArtifactId();
        } else {
            return project.getArtifactId() + "-opear";
        }
    }

    String getOakpalCoreVersion() throws MojoExecutionException {
        return resolveArtifacts(project.getDependencies(), true).stream().filter(TEST_IS_OAKPAL_CORE)
                .findFirst()
                .map(Artifact::getVersion).orElse(getOwnVersion());
    }

    List<File> getEmbeddedLibraries() throws MojoExecutionException {
        final Set<String> NOT_EMBEDDABLE = new HashSet<>(
                Arrays.asList(Artifact.SCOPE_IMPORT, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_TEST));
        return resolveArtifacts(project.getDependencies(), true).stream().filter(TEST_IS_OAKPAL_CORE.negate())
                .filter(composeTest(Artifact::getScope, inSet(NOT_EMBEDDABLE).negate())).map(Artifact::getFile)
                .collect(Collectors.toList());
    }

    File assembleOpear() throws Exception {
        final String prefix = "lib/";
        final File finalFile = new File(this.outputDirectory, this.finalName + ".opear");
        MavenArchiveConfiguration conf = new MavenArchiveConfiguration();
        conf.addManifestEntry(Opear.MF_BUNDLE_SYMBOLICNAME, getBundleSymbolicName());
        conf.addManifestEntry(Opear.MF_OAKPAL_VERSION, getOakpalCoreVersion());

        OpearArchiver opear = new OpearArchiver();
        List<File> embeddedFiles = new ArrayList<>();
        if ("jar".equals(project.getPackaging()) && project.getArtifact().getFile().exists()) {
            if (project.getArtifact().getFile().isDirectory()) {
                throw new Exception("cannot embed project artifact while it is a directory.");
            }
            embeddedFiles.add(project.getFile());
        }
        embeddedFiles.addAll(getEmbeddedLibraries());
        Map<String, File> destFiles = mapDestFileNames(embeddedFiles);
        final String scanClassPath = String.join(",",
                destFiles.entrySet().stream()
                        .map(entry -> prefix + entry.getKey())
                        .toArray(String[]::new));
        conf.addManifestEntry(Opear.MF_CLASS_PATH, scanClassPath);

        destFiles.entrySet().stream().filter(testValue(File::isFile)).forEachOrdered(entry -> {
            opear.addFile(entry.getValue(), prefix + entry.getKey());
        });

        destFiles.entrySet().stream().filter(testValue(File::isDirectory)).forEachOrdered(entry -> {
            opear.addFileSet(new DefaultFileSet(entry.getValue()).prefixed(prefix + entry.getKey()));
        });

        MavenArchiver mavenArchiver = new MavenArchiver();
        mavenArchiver.setArchiver(opear);
        mavenArchiver.setOutputFile(finalFile);
        mavenArchiver.createArchive(getSession(), project, conf);

        return finalFile;
    }

    static Map<String, File> mapDestFileNames(final @NotNull List<File> files) {
        final Map<String, File> acc = new LinkedHashMap<>();
        for (File file : files) {
            String name = file.getName();
            final int lastDot = name.lastIndexOf(".");
            final String base = lastDot >= 1 ? name.substring(0, lastDot) : name;
            final String ext = lastDot >= 1 ? name.substring(lastDot) : "";
            int siblingCount = 0;
            while (acc.containsKey(name)) {
                name = base + "_" + ++siblingCount + ext;
            }
            acc.put(name, file);
        }
        return acc;
    }

}
