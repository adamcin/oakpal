package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.api.Nothing;
import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.core.InstallHookPolicy;
import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.core.opear.AdhocOpear;
import net.adamcin.oakpal.core.opear.Opear;
import net.adamcin.oakpal.core.opear.OpearFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static net.adamcin.oakpal.api.Fun.compose1;
import static net.adamcin.oakpal.api.Fun.result1;

final class Options {
    static final String CACHE_DIR_NAME = ".oakpal-cache";
    static final Function<StructuredMessage, IO<Nothing>> EMPTY_PRINTER = message -> IO.empty;
    static final Options DEFAULT_OPTIONS = new Options();
    private final boolean justHelp;
    private final boolean justVersion;
    private final boolean storeBlobs;
    private final URL planUrl;
    private final ClassLoader scanClassLoader;
    private final File cacheDir;
    private final File opearFile;
    private final String planName;
    private final File planFile;
    private final File planFileBaseDir;
    private final List<File> preInstallFiles;
    private final List<File> extendedClassPathFiles;
    private final boolean noHooks;
    private final List<File> scanFiles;
    private final Function<StructuredMessage, IO<Nothing>> printer;
    private final Severity failOnSeverity;

    Options() {
        this(true, true, false,
                OakpalPlan.BASIC_PLAN_URL, Options.class.getClassLoader(),
                new File(System.getProperty("java.io.tmpdir")),
                null, null, null, null,
                Collections.emptyList(),
                Collections.emptyList(), false,
                Collections.emptyList(),
                EMPTY_PRINTER,
                Severity.MAJOR);
    }

    Options(final boolean justHelp,
            final boolean justVersion,
            final boolean storeBlobs,
            final @NotNull URL planUrl,
            final @NotNull ClassLoader scanClassLoader,
            final @NotNull File cacheDir,
            final @Nullable File opearFile,
            final @Nullable String planName,
            final @Nullable File planFile,
            final @Nullable File planFileBaseDir,
            final @NotNull List<File> preInstallFiles,
            final @NotNull List<File> extendedClassPathFiles,
            final boolean noHooks,
            final @NotNull List<File> scanFiles,
            final @NotNull Function<StructuredMessage, IO<Nothing>> printer,
            final @NotNull Severity failOnSeverity) {
        this.justHelp = justHelp;
        this.justVersion = justVersion;
        this.storeBlobs = storeBlobs;
        this.planUrl = planUrl;
        this.scanClassLoader = scanClassLoader;
        this.cacheDir = cacheDir;
        this.opearFile = opearFile;
        this.planName = planName;
        this.planFile = planFile;
        this.planFileBaseDir = planFileBaseDir;
        this.preInstallFiles = preInstallFiles;
        this.extendedClassPathFiles = extendedClassPathFiles;
        this.noHooks = noHooks;
        this.scanFiles = scanFiles;
        this.printer = printer;
        this.failOnSeverity = failOnSeverity;
    }

    public boolean isJustHelp() {
        return justHelp;
    }

    public boolean isJustVersion() {
        return justVersion;
    }

    public boolean isStoreBlobs() {
        return storeBlobs;
    }

    public boolean isNoHooks() {
        return noHooks;
    }

    public URL getPlanUrl() {
        return planUrl;
    }

    public ClassLoader getScanClassLoader() {
        return scanClassLoader;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public @Nullable File getOpearFile() {
        return opearFile;
    }

    public @Nullable String getPlanName() {
        return planName;
    }

    public @Nullable File getPlanFile() {
        return planFile;
    }

    public @Nullable File getPlanFileBaseDir() {
        return planFileBaseDir;
    }

    public @NotNull List<File> getPreInstallFiles() {
        return preInstallFiles;
    }

    public @NotNull List<File> getExtendedClassPathFiles() {
        return extendedClassPathFiles;
    }

    public List<File> getScanFiles() {
        return scanFiles;
    }

    public Function<StructuredMessage, IO<Nothing>> getPrinter() {
        return printer;
    }

    public Severity getFailOnSeverity() {
        return failOnSeverity;
    }

    boolean hasOverrides() {
        return noHooks || !getPreInstallFiles().isEmpty();
    }

    public OakpalPlan applyOverrides(final @NotNull OakpalPlan basePlan) {
        if (hasOverrides()) {
            final OakpalPlan.Builder overridePlan = new OakpalPlan.Builder(basePlan.getBase(), basePlan.getName())
                    .startingWithPlan(basePlan);
            if (!getPreInstallFiles().isEmpty()) {
                List<URL> allUrls = new ArrayList<>(basePlan.getPreInstallUrls());
                getPreInstallFiles().stream().map(compose1(File::toURI, result1(URI::toURL)))
                        .collect(Result.tryCollect(Collectors.toList())).forEach(allUrls::addAll);
                overridePlan.withPreInstallUrls(allUrls);
            }
            if (isNoHooks()) {
                overridePlan.withInstallHookPolicy(InstallHookPolicy.SKIP);
                overridePlan.withEnablePreInstallHooks(false);
            }
            return overridePlan.build();
        } else {
            return basePlan;
        }
    }

    static final class Builder {
        private boolean justHelp;
        private boolean justVersion;
        private boolean storeBlobs;
        private boolean outputJson;
        private boolean noPlan;
        private boolean noHooks;
        private String planName;
        private File planFile;
        private File planFileBaseDir;
        private List<File> preInstallFiles = new ArrayList<>();
        private List<File> extendedClassPathFiles = new ArrayList<>();
        private File outFile;
        private File cacheDir;
        private File opearFile;
        private List<File> scanFiles = new ArrayList<>();
        private Severity failOnSeverity;

        public Builder setJustHelp(final boolean justHelp) {
            this.justHelp = justHelp;
            return this;
        }

        public Builder setJustVersion(final boolean justVersion) {
            this.justVersion = justVersion;
            return this;
        }

        public Builder setStoreBlobs(final boolean storeBlobs) {
            this.storeBlobs = storeBlobs;
            return this;
        }

        public Builder setOutputJson(final boolean outputJson) {
            this.outputJson = outputJson;
            return this;
        }

        public Builder setNoPlan(final boolean noPlan) {
            this.noPlan = noPlan;
            return this;
        }

        public Builder setNoHooks(final boolean noHooks) {
            this.noHooks = noHooks;
            return this;
        }

        public Builder setPlanName(final @Nullable String planName) {
            this.planName = planName;
            return this;
        }

        public Builder setPlanFileBaseDir(final @Nullable File planFileBaseDir) {
            this.planFileBaseDir = planFileBaseDir;
            return this;
        }

        public Builder setPlanFile(final @Nullable File planFile) {
            this.planFile = planFile;
            return this;
        }

        public Builder setPreInstallFiles(final @NotNull List<File> preInstallFiles) {
            this.preInstallFiles = preInstallFiles;
            return this;
        }

        public Builder addPreInstallFile(final @NotNull File preInstallFile) {
            this.preInstallFiles.add(preInstallFile);
            return this;
        }

        public Builder setExtendedClassPathFiles(final @NotNull List<File> extendedClassPathFiles) {
            this.extendedClassPathFiles = new ArrayList<>(extendedClassPathFiles);
            return this;
        }

        public Builder addExtendedClassPathFile(final @NotNull File extendedClassPathFile) {
            this.extendedClassPathFiles.add(extendedClassPathFile);
            return this;
        }

        public Builder setOutFile(final @Nullable File outFile) {
            this.outFile = outFile;
            return this;
        }

        public Builder setCacheDir(final @Nullable File cacheDir) {
            this.cacheDir = cacheDir;
            return this;
        }

        public Builder setOpearFile(final @Nullable File opearFile) {
            this.opearFile = opearFile;
            return this;
        }

        public Builder addScanFile(final @NotNull File scanFile) {
            this.scanFiles.add(scanFile);
            return this;
        }

        public Builder setFailOnSeverity(final @Nullable Severity failOnSeverity) {
            this.failOnSeverity = failOnSeverity;
            return this;
        }

        Result<Opear> buildOpear(final @NotNull Console console, final @NotNull File opearCache) {
            final Result<Opear> baseOpear;
            if (planFile != null) {
                baseOpear = buildAdhocOpear(console).map(Function.identity());
            } else {
                baseOpear = buildOpearFile(console, opearCache).map(Function.identity());
            }
            return baseOpear;
        }

        Result<AdhocOpear> buildAdhocOpear(final @NotNull Console console) {
            return AdhocOpear.fromPlanFile(planFile, planFileBaseDir);
        }

        Result<OpearFile> buildOpearFile(final @NotNull Console console, final @NotNull File opearCache) {
            final File opearResolved = Optional.ofNullable(opearFile).orElseGet(() ->
                    console.getCwd().toPath().resolve(
                            console.getEnv().getOrDefault(Console.ENV_OAKPAL_OPEAR, "."))
                            .toFile());

            return Result.success(opearResolved.getAbsoluteFile())
                    .flatMap(file -> {
                        if (file.isFile()) {
                            try (JarFile jarFile = new JarFile(file, true)) {
                                return OpearFile.fromJar(jarFile, opearCache);
                            } catch (IOException e) {
                                return Result.failure(String.format("%s is not a jar format file", file.getPath()), e);
                            }
                        } else {
                            return OpearFile.fromDirectory(file);
                        }
                    });
        }

        Result<ClassLoader> getExtendedClassLoader(final @NotNull Opear opear,
                                                   final @NotNull ClassLoader parentClassLoader) {
            if (this.extendedClassPathFiles.isEmpty()) {
                return Result.success(opear.getPlanClassLoader(getClass().getClassLoader()));
            } else {
                final URL[] urls = this.extendedClassPathFiles.stream()
                        .filter(File::exists)
                        .filter(file -> file.isDirectory() || file.getName().endsWith(".jar"))
                        .flatMap(compose1(compose1(File::toURI, result1(URI::toURL)), Result::stream))
                        .toArray(URL[]::new);
                return Result.success(new URLClassLoader(urls, opear.getPlanClassLoader(parentClassLoader)));
            }
        }

        Result<Options> build(final @NotNull Console console) {
            final File realCacheDir = this.cacheDir != null
                    ? this.cacheDir
                    : console.getCwd().toPath().resolve(CACHE_DIR_NAME).toFile().getAbsoluteFile();
            final File opearCache = new File(realCacheDir, "opears");
            opearCache.mkdirs();

            Result<Opear> opearResult = buildOpear(console, opearCache);

            return opearResult.flatMap(opear -> Optional.ofNullable(planName).map(opear::getSpecificPlan)
                    .orElse(Result.success(noPlan ? OakpalPlan.EMPTY_PLAN_URL : opear.getDefaultPlan()))
                    .flatMap(planUrl -> getExtendedClassLoader(opear, getClass().getClassLoader())
                            .flatMap(classLoader -> messageWriter(console, outputJson, outFile).map(writer ->
                                    new Options(justHelp, justVersion, storeBlobs, planUrl,
                                            classLoader, realCacheDir, opearFile, planName, planFile,
                                            planFileBaseDir, preInstallFiles, extendedClassPathFiles,
                                            noHooks, scanFiles, writer, Optional.ofNullable(failOnSeverity)
                                            .orElse(DEFAULT_OPTIONS.failOnSeverity))))));
        }
    }

    /**
     * Writes a structured message to the appropriate stream in the appropriate format.
     *
     * @param outputJson whether to output json or not
     * @return a nothing IO monad.
     */
    static @NotNull Result<Function<StructuredMessage, IO<Nothing>>> messageWriter(final @NotNull Console console,
                                                                                   final boolean outputJson,
                                                                                   final @Nullable File outFile) {
        final Function<StructuredMessage, Object> objectifier = outputJson
                ? StructuredMessage::toJson : message -> message;
        final Result<Function<Object, IO<Nothing>>> printerResult = Optional.ofNullable(outFile)
                .map(file -> console.openPrinter(file).map(printer -> (Function<Object, IO<Nothing>>) printer))
                .orElse(Result.success(console::printLine));
        return printerResult.map(objectifier::andThen);
    }

}
