package net.adamcin.oakpal.maven.mojo;

import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.composeTest;
import static net.adamcin.oakpal.core.Fun.entriesToMap;
import static net.adamcin.oakpal.core.Fun.inSet;
import static net.adamcin.oakpal.core.Fun.result1;
import static net.adamcin.oakpal.core.Fun.testValue;
import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.Fun.zipKeysWithValueFunc;
import static net.adamcin.oakpal.core.Fun.zipValuesWithKeyFunc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.core.Opear;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.Util;
import net.adamcin.oakpal.maven.component.OakpalComponentConfigurator;
import org.apache.jackrabbit.oak.commons.FileIOUtils;
import org.apache.jackrabbit.util.Text;
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
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.jetbrains.annotations.NotNull;

/**
 * Bundles up project dependencies an exported plans as an opear (OakPAL Encapsulated Archive) file, and attaches
 * it to the project.
 *
 * @since 1.4.0
 */
@Mojo(name = "opear-package",
        requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM,
        configurator = OakpalComponentConfigurator.HINT,
        defaultPhase = LifecyclePhase.PACKAGE)
public class OpearPackageMojo extends AbstractCommonMojo {

    private static final String OAKPAL_GROUP_ID = "net.adamcin.oakpal";
    private static final String OAKPAL_CORE_ARTIFACT_ID = "oakpal-core";

    private static final Predicate<Artifact> TEST_IS_OAKPAL_CORE =
            composeTest(Artifact::getGroupId, OAKPAL_GROUP_ID::equals)
                    .and(composeTest(Artifact::getArtifactId, OAKPAL_CORE_ARTIFACT_ID::equals));

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String finalName;

    @Parameter(defaultValue = "${project.build.directory}/opear-plans/plan.json", required = true)
    private File planFile;

    @Parameter
    private List<File> additionalPlans = new ArrayList<>();

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

    String getOakpalCoreVersion() throws MojoFailureException {
        return resolveArtifacts(project.getDependencies(), true).stream().filter(TEST_IS_OAKPAL_CORE)
                .findFirst()
                .map(Artifact::getVersion).orElse(getOwnVersion());
    }

    List<File> getEmbeddedLibraries() throws MojoFailureException {
        final Set<String> NOT_EMBEDDABLE = new HashSet<>(
                Arrays.asList(Artifact.SCOPE_IMPORT, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_TEST));
        return resolveArtifacts(project.getDependencies(), true).stream().filter(TEST_IS_OAKPAL_CORE.negate())
                .filter(composeTest(Artifact::getScope, inSet(NOT_EMBEDDABLE).negate())).map(Artifact::getFile)
                .collect(Collectors.toList());
    }

    File assembleOpear() throws Exception {
        final String prefix = "lib/";
        final File finalFile = new File(this.outputDirectory, this.finalName + ".opear");

        final File planPrep = new File(this.outputDirectory, "plans-tmp-" + this.finalName);
        FileUtils.deleteDirectory(planPrep);
        planPrep.mkdirs();
        final Result<List<String>> planNames = this.shrinkWrapPlans(planPrep);
        planNames.throwCause(Exception.class);

        final MavenArchiveConfiguration conf = new MavenArchiveConfiguration();
        conf.addManifestEntry(Opear.MF_BUNDLE_SYMBOLICNAME, getBundleSymbolicName());
        conf.addManifestEntry(Opear.MF_OAKPAL_VERSION, getOakpalCoreVersion());
        conf.addManifestEntry(Opear.MF_OAKPAL_PLAN,
                Util.escapeManifestHeaderValues(planNames.getOrDefault(Collections.emptyList())));

        OpearArchiver opear = new OpearArchiver();
        opear.addFileSet(DefaultFileSet.fileSet(planPrep));

        List<File> embeddedFiles = new ArrayList<>();
        if ("jar".equals(project.getPackaging()) && project.getArtifact().getFile().exists()) {
            if (project.getArtifact().getFile().isDirectory()) {
                throw new Exception("cannot embed project artifact while it is a directory.");
            }
            embeddedFiles.add(project.getFile());
        } else if ("opear".equals(project.getPackaging())
                && new File(project.getBuild().getOutputDirectory()).isDirectory()) {
            embeddedFiles.add(new File(project.getBuild().getOutputDirectory()));
        }
        embeddedFiles.addAll(getEmbeddedLibraries());
        Map<String, File> destFiles = mapDestFileNames(new LinkedHashMap<>(), embeddedFiles, File::getName);
        final String scanClassPath = Util.escapeManifestHeaderValue(destFiles.entrySet().stream()
                .map(entry -> prefix + entry.getKey())
                .toArray(String[]::new));
        conf.addManifestEntry(Opear.MF_CLASS_PATH, scanClassPath);

        destFiles.entrySet().stream().filter(testValue(File::isFile)).forEachOrdered(entry -> {
            opear.addFile(entry.getValue(), prefix + entry.getKey());
        });

        destFiles.entrySet().stream().filter(testValue(File::isDirectory)).forEachOrdered(entry -> {
            opear.addFileSet(new DefaultFileSet(entry.getValue()).prefixed(prefix + entry.getKey() + "/"));
        });

        MavenArchiver mavenArchiver = new MavenArchiver();
        mavenArchiver.setArchiver(opear);
        mavenArchiver.setOutputFile(finalFile);
        mavenArchiver.createArchive(getSession(), project, conf);

        return finalFile;
    }

    static <T> Map<String, T> mapDestFileNames(final @NotNull Map<String, T> acc,
                                               final @NotNull List<T> files,
                                               final @NotNull Function<T, String> namer) {
        for (T file : files) {
            String name = namer.apply(file);
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

    /**
     * Collect external resources and rewrite plans to produce a self-contained opear.
     *
     * @param toDir the directory to write to.
     * @return result containing list of plan names
     */
    Result<List<String>> shrinkWrapPlans(final @NotNull File toDir) {
        final Result<List<OakpalPlan>> initialResult =
                !planFile.isFile()
                        ? Result.success(new ArrayList<>())
                        : Result.success(planFile)
                        .flatMap(compose(File::toURI, result1(URI::toURL)))
                        .flatMap(OakpalPlan::fromJson)
                        .map(compose(Collections::singletonList, ArrayList::new));

        final Result<List<OakpalPlan>> addedResult = initialResult.flatMap(plans ->
                additionalPlans.stream()
                        .map(compose(File::toURI, result1(URI::toURL)))
                        .collect(Result.tryCollect(Collectors.toCollection(LinkedHashSet::new)))
                        .flatMap(planUrls -> planUrls.stream()
                                .map(OakpalPlan::fromJson)
                                .collect(Result.logAndRestream())
                                .collect(Result.tryCollect(Collectors.toList()))));


        final Result<List<OakpalPlan>> allPlansResult = initialResult.flatMap(initial ->
                addedResult.map(added -> {
                    initial.addAll(added);
                    return initial;
                }));

        final Result<Map<URL, String>> renamedResult = allPlansResult
                .flatMap(plans -> copyUrlStreams(toDir, plans.stream()
                        .flatMap(compose(OakpalPlan::getPreInstallUrls, List::stream))
                        .collect(Collectors.toList())));
        final Result<Map<String, OakpalPlan>> rewrittenResult = renamedResult.flatMap(renamed ->
                allPlansResult.map(allPlans -> allPlans.stream()
                        .map(plan -> rewritePlan(toDir, renamed, plan, plan.getName()))
                        .collect(Collectors.toList()))
                        .flatMap(allPlans -> copyPlans(toDir, allPlans)));

        return rewrittenResult.map(compose(Map::keySet, ArrayList::new));
    }

    static OakpalPlan rewritePlan(final @NotNull File toDir,
                                  final @NotNull Map<URL, String> renamed,
                                  final @NotNull OakpalPlan plan,
                                  final @NotNull String filename) {
        return new OakpalPlan.Builder(
                compose(File::toURI, uncheck1(URI::toURL))
                        .apply(new File(toDir, filename)), filename)
                .startingWithPlan(plan)
                .withPreInstallUrls(plan.getPreInstallUrls().stream()
                        .map(compose(renamed::get, uncheck1(name -> new File(toDir, name).toURI().toURL())))
                        .collect(Collectors.toList()))
                .build();
    }

    static Result<Map<String, OakpalPlan>> copyPlans(final @NotNull File toDir, final @NotNull List<OakpalPlan> plans) {
        final Map<String, OakpalPlan> renamed = mapDestFileNames(new LinkedHashMap<>(), plans, OakpalPlan::getName);

        final JsonWriterFactory writerFactory =
                Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        final Result<List<String>> filenameResults = renamed.entrySet().stream().map(result1(entry -> {
            try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(new File(toDir, entry.getKey())));
                 JsonWriter writer = writerFactory.createWriter(osw)) {
                writer.writeObject(entry.getValue().toJson());
                return entry.getKey();
            }
        })).collect(Result.tryCollect(Collectors.toList()));

        return filenameResults.map(filenames ->
                filenames.stream()
                        .map(zipKeysWithValueFunc(renamed::get))
                        .collect(entriesToMap()));
    }

    static Result<Map<URL, String>> copyUrlStreams(final @NotNull File toDir,
                                                   final @NotNull List<URL> urls) {
        final List<URL> dedupedUrls = new ArrayList<>(new LinkedHashSet<>(urls));
        final Map<String, URL> renamed =
                mapDestFileNames(new LinkedHashMap<>(), dedupedUrls, url -> Text.getName(url.getPath()));

        final Result<List<String>> filenameResults = renamed.entrySet().stream().map(result1(entry -> {
            try (InputStream is = entry.getValue().openStream()) {
                FileIOUtils.copyInputStreamToFile(is, new File(toDir, entry.getKey()));
                return entry.getKey();
            }
        })).collect(Result.tryCollect(Collectors.toList()));

        return filenameResults.map(filenames ->
                filenames.stream()
                        .map(zipValuesWithKeyFunc(renamed::get))
                        .collect(entriesToMap((v1, v2) -> v1)));
    }

    public static class OpearArchiver extends JarArchiver {
        public static final String ARCHIVE_TYPE = "opear";

        public OpearArchiver() {
            this.archiveType = ARCHIVE_TYPE;
        }
    }
}
