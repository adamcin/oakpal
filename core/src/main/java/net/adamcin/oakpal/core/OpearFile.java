package net.adamcin.oakpal.core;

import org.apache.jackrabbit.oak.commons.FileIOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.result1;

/**
 * The default implemenation of {@link Opear}. This is backed by an extracted JAR directory, including
 * META-INF/MANIFEST.MF.
 */
public final class OpearFile implements Opear {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpearFile.class);

    static final class OpearMetadata {
        private final String[] plans;
        private final String[] planClassPath;
        private final boolean defaultBasic;

        OpearMetadata(final @NotNull String[] plans,
                      final @NotNull String[] planClassPath,
                      final boolean defaultBasic) {
            this.plans = plans;
            this.planClassPath = planClassPath;
            this.defaultBasic = defaultBasic;
        }

        @NotNull String[] getPlans() {
            return plans;
        }

        @NotNull String[] getPlanClassPath() {
            return planClassPath;
        }

        public boolean isDefaultBasic() {
            return defaultBasic;
        }
    }

    public static final Attributes.Name NAME_BUNDLE_SYMBOLICNAME = new Attributes.Name(MF_BUNDLE_SYMBOLICNAME);
    public static final Attributes.Name NAME_BUNDLE_VERSION = new Attributes.Name(MF_BUNDLE_VERSION);
    public static final Attributes.Name NAME_CLASS_PATH = new Attributes.Name(MF_CLASS_PATH);
    public static final Attributes.Name NAME_OAKPAL_VERSION = new Attributes.Name(MF_OAKPAL_VERSION);
    public static final Attributes.Name NAME_OAKPAL_PLAN = new Attributes.Name(MF_OAKPAL_PLAN);

    final File cacheDir;
    final OpearMetadata metadata;

    OpearFile(final File cacheDir, final OpearMetadata metadata) {
        this.cacheDir = cacheDir;
        this.metadata = metadata;
    }

    @Override
    public URL getDefaultPlan() {
        return Stream.of(metadata.getPlans()).findFirst()
                .map(result1((String name) -> new File(cacheDir, name).toURI().toURL()))
                .flatMap(compose(Result::stream, Stream::findFirst))
                .orElse(metadata.isDefaultBasic() ? OakpalPlan.BASIC_PLAN_URL : OakpalPlan.EMPTY_PLAN_URL);
    }

    @Override
    public Result<URL> getSpecificPlan(final @NotNull String planName) {
        if (Arrays.asList(metadata.getPlans()).contains(planName)) {
            return result1((String name) -> new File(cacheDir, name).toURI().toURL()).apply(planName);
        }
        return Result.failure("Opear does not export a plan named " + planName);
    }

    @Override
    public ClassLoader getPlanClassLoader(final @NotNull ClassLoader parent) {
        final URL[] urls = Stream.of(metadata.getPlanClassPath())
                .map(name -> new File(cacheDir, name)).flatMap(file -> {
                    if (file.isDirectory() || file.getName().endsWith(".jar")) {
                        return compose(File::toURI, result1(URI::toURL)).apply(file).stream();
                    } else {
                        return Stream.empty();
                    }
                })
                .toArray(URL[]::new);

        if (urls.length > 0) {
            return new URLClassLoader(urls, parent);
        } else {
            return parent;
        }
    }

    static Result<Manifest> readExpectedManifest(final @NotNull File mfFile) {
        try (InputStream input = new FileInputStream(mfFile)) {
            return Result.success(new Manifest(input));
        } catch (IOException e) {
            return Result.failure(e);
        }
    }

    static OpearMetadata metaForSimpleDir(final @NotNull File directory) {
        LOGGER.trace("[OpearFile#metaForSimpleDir] returning simple opear metadata for directory: {}", directory);
        final String[] plans = new File(directory, Opear.SIMPLE_DIR_PLAN).exists()
                ? new String[]{Opear.SIMPLE_DIR_PLAN}
                : new String[0];
        return new OpearMetadata(plans, new String[]{"."}, true);
    }

    static Result<String[]> validateUriHeaderValuesWithDefaultForMissing(final @NotNull Manifest manifest,
                                                                         final @NotNull Attributes.Name headerName,
                                                                         final @NotNull String... defaultValue) {
        if (!manifest.getMainAttributes().containsKey(headerName)) {
            return Result.success(defaultValue);
        } else {
            return validateUriHeaderValues(manifest, headerName);
        }
    }

    static Result<String[]> validateUriHeaderValues(final @NotNull Manifest manifest,
                                                    final @NotNull Attributes.Name headerName) {

        final List<Result<String>> cpResult = Util.getManifestHeaderValues(manifest, headerName.toString())
                .stream()
                .map(result1(URI::new))
                .map(uriResult -> uriResult.flatMap(uri -> {
                    final String normal = uri.normalize().getPath().replaceFirst("^/", "");
                    if ("..".equals(normal) || normal.startsWith("../")) {
                        return Result.failure("Illegal parent path selector in " + normal);
                    } else {
                        return Result.success(normal);
                    }
                }))
                .collect(Collectors.toList());
        return cpResult.stream()
                .filter(Result::isFailure).findFirst()
                .map(failed ->
                        failed.getError()
                                .map(Result::<String[]>failure)
                                .orElseGet(() -> Result.<String[]>failure(format("invalid %s header", headerName))))
                .orElseGet(() -> Result.success(cpResult.stream().flatMap(Result::stream).toArray(String[]::new)));
    }

    static Result<OpearMetadata> validateOpearManifest(final @Nullable Manifest manifest) {
        return Result.success(manifest)
                .map(Optional::ofNullable)
                .flatMap(optMan -> optMan.map(Result::success).orElseGet(() ->
                        Result.failure("specified opear file does not contain manifest."))
                        .flatMap(man -> ofNullable(man.getMainAttributes().getValue(NAME_BUNDLE_SYMBOLICNAME))
                                .map(Result::success)
                                .orElseGet(() -> Result.failure("opear manifest does not specify "
                                        + NAME_BUNDLE_SYMBOLICNAME.toString()))
                                .map(bsn -> ofNullable(man.getMainAttributes().getValue(NAME_BUNDLE_VERSION))
                                        .map(bvn -> bsn + "_" + bvn).orElse(bsn))
                                .flatMap(bid ->
                                        validateUriHeaderValuesWithDefaultForMissing(man, NAME_CLASS_PATH, ".")
                                                .flatMap(planClassPath ->
                                                        validateUriHeaderValues(man, NAME_OAKPAL_PLAN).flatMap(plans ->
                                                                Result.success(new OpearMetadata(plans,
                                                                        planClassPath, false))
                                                        )))));
    }

    public static Result<OpearFile> fromDirectory(final @NotNull File directory) {
        return Result.success(Optional.of(new File(directory, JarFile.MANIFEST_NAME)))
                .flatMap(oMfFile -> oMfFile
                        // non-existent manifest should indicate simple directory.
                        // see .orElseGet() at the end of this chain
                        .filter(File::exists)
                        // if the manifest file exists, expect a valid manifest object.
                        .map(OpearFile::readExpectedManifest)
                        .map(manResult -> manResult.flatMap(OpearFile::validateOpearManifest))
                        // only when manifest file does not exist, return success with simple metadata
                        .orElseGet(() -> Result.success(metaForSimpleDir(directory)))
                )
                .map(meta -> new OpearFile(directory, meta));
    }

    static Result<String> getHashCacheKey(final @NotNull String path) {
        try (FileInputStream is = new FileInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] buf = new byte[1024];
            int nread;
            while ((nread = is.read(buf)) != -1) {
                digest.update(buf, 0, nread);
            }
            final byte[] mdbytes = digest.digest();
            return Result.success(Base64.getUrlEncoder().withoutPadding().encodeToString(mdbytes));
        } catch (Exception e) {
            return Result.failure(e);
        }
    }

    public static Result<OpearFile> fromJar(final @NotNull JarFile jarFile, final @NotNull File cacheBaseDir) {
        return getHashCacheKey(jarFile.getName()).flatMap(cacheKey -> {
            return result1(JarFile::getManifest).apply(jarFile)
                    .flatMap(OpearFile::validateOpearManifest)
                    .flatMap(metadata -> {
                        final File cacheDir = new File(cacheBaseDir, cacheKey);
                        if (cacheDir.isDirectory()) {
                            return fromDirectory(cacheDir);
                        } else {
                            if (!cacheDir.mkdirs()) {
                                return Result.failure(format("failed to create cache dir %s for specified opear file %s",
                                        cacheDir.getPath(), jarFile.getName()));
                            } else {
                                return cacheJar(jarFile, cacheDir).flatMap(OpearFile::fromDirectory);
                            }
                        }
                    });
        });
    }

    static Result<File> cacheJar(final @NotNull JarFile jarFile, final @NotNull File cacheDir) {
        return jarFile.stream()
                .map(entry -> {
                    final File cacheFile = new File(cacheDir, entry.getName());
                    if (entry.isDirectory()) {
                        if (cacheFile.isDirectory() || cacheFile.mkdirs()) {
                            return Result.success(cacheFile);
                        }
                    } else {
                        if (cacheFile.getParentFile().isDirectory() || cacheFile.getParentFile().mkdirs()) {
                            try (InputStream input = jarFile.getInputStream(entry)) {
                                FileIOUtils.copyInputStreamToFile(input, cacheFile);
                                return Result.success(cacheFile);
                            } catch (IOException e) {
                                return Result.<File>failure("failed to cache entry " + entry.getName(), e);
                            }
                        }
                    }
                    return Result.<File>failure("failed to cache entry " + entry.getName());
                })
                .filter(Result::isFailure).findFirst()
                .map(failed -> failed.getError().map(Result::<File>failure)
                        .orElseGet(() -> Result.<File>failure("failed to cache jarFile" + jarFile.getName())))
                .orElse(Result.success(cacheDir));
    }


}
