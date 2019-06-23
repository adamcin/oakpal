package net.adamcin.oakpal.cli;

import static net.adamcin.oakpal.core.Fun.compose;
import static net.adamcin.oakpal.core.Fun.result1;
import static net.adamcin.oakpal.core.Fun.streamIt;
import static net.adamcin.oakpal.core.Fun.uncheck1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import net.adamcin.oakpal.core.Chart;
import net.adamcin.oakpal.core.Result;
import net.adamcin.oakpal.core.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class Loader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class);
    public static final String MF_HEADER_OAKPAL_CLASS_PATH = "Oakpal-Class-Path";
    public static final String ENTRY_OAKPAL_CHART = "oakpal-chart.json";

    private Loader() {
    }

    public static ClassLoader getPathClassLoader(final @NotNull Console console,
                                                 final @NotNull ClassLoader parent) {
        final Stream<URL> workspacePath = getClassPathUrlsForElement(console.getCwd());
        final URL[] urls = Stream.concat(workspacePath, Stream.of(console.getOakpalPath())
                .flatMap(uncheck1(Loader::getClassPathUrlsForElement)))
                .toArray(URL[]::new);
        return new URLClassLoader(urls, parent);
    }

    static URL toJarUrl(final @NotNull URL url) throws MalformedURLException {
        return new URL("jar:" + url.toExternalForm() + "!/");
    }

    static Stream<URL> getClassPathUrlsForElement(final @NotNull File pathElement) {
        if (pathElement.isFile()) {
            return compose(File::toURI, result1(URI::toURL)).apply(pathElement)
                    .flatMap(baseUrl -> result1((File file) -> new JarFile(file)).apply(pathElement)
                            .flatMap(jarFile -> getOarClassPath(baseUrl, jarFile))
                            .orElse(() -> Result.success(Stream.of(baseUrl))))
                    .getOrElse(Stream.empty());
        } else if (!pathElement.exists()) {
            return Stream.empty();
        } else {
            return getDirClassPath(pathElement);
        }
    }

    static Result<Stream<URL>> getOarClassPath(final @NotNull URL fileUrl, final @NotNull JarFile jarFile) {
        return result1(JarFile::getManifest).apply(jarFile)
                .map(compose(Optional::ofNullable, optMan ->
                        optMan.filter(man -> man.getMainAttributes().containsKey(MF_HEADER_OAKPAL_CLASS_PATH))))
                .map(optMan -> optMan.map(manifest ->
                        Util.getManifestHeaderValues(manifest, MF_HEADER_OAKPAL_CLASS_PATH))
                        .orElse(Collections.singletonList(".")))
                .flatMap(names -> result1(Loader::toJarUrl).apply(fileUrl)
                        .map(jarUrl -> names.stream().map(result1(name -> new URL(jarUrl, name)))
                                .flatMap(Result::stream)));
    }

    static Stream<URL> getDirClassPath(final @NotNull File pathElement) {
        final List<String> defaultRelPaths = Collections.singletonList(".");
        final Result<Stream<URL>> relPathResult =
                compose(File::toURI, result1(URI::toURL)).apply(pathElement).map(baseUrl -> {
                    final Supplier<Result<Manifest>> mfSupplier = () -> {
                        final File mfFile = new File(pathElement, JarFile.MANIFEST_NAME);
                        if (mfFile.exists()) {
                            try (InputStream input = new FileInputStream(mfFile)) {
                                return Result.success(new Manifest(input));
                            } catch (IOException e) {
                                return Result.failure(e);
                            }
                        } else {
                            return Result.failure("Manifest does not exist: " + mfFile.getAbsolutePath());
                        }
                    };
                    return mfSupplier.get().stream()
                            .findFirst()
                            .filter(man -> man.getMainAttributes().containsKey(MF_HEADER_OAKPAL_CLASS_PATH))
                            .map(man -> Util.getManifestHeaderValues(man, MF_HEADER_OAKPAL_CLASS_PATH))
                            .orElse(defaultRelPaths).stream()
                            .map(result1((String relPath) -> new URL(baseUrl, relPath))).flatMap(Result::stream);
                });
        return relPathResult.getOrElse(Stream.empty());
    }

    static Function<File, Stream<URL>> findChartInPathFile(final @NotNull String chartName) {
        return oakpalPathFile -> {
            if (oakpalPathFile.isFile()) {
                return result1((File file) -> new JarFile(file)).apply(oakpalPathFile)
                        .map(jar -> streamIt(jar.getJarEntry(chartName)))
                        .map(entry -> entry.map(value -> chartName))
                        .flatMap(names -> result1(URI::toURL).apply(oakpalPathFile.toURI())
                                .flatMap(result1(Loader::toJarUrl))
                                .map(jarUrl -> names.map(uncheck1((String name) -> new URL(jarUrl, name)))))
                        .getOrElse(Stream.empty());
            } else if (oakpalPathFile.isDirectory()) {
                final File chartFile = new File(oakpalPathFile, ENTRY_OAKPAL_CHART);
                if (chartFile.isFile()) {
                    return Stream.of(chartFile.toURI()).map(uncheck1(URI::toURL));
                }
            }
            return Stream.empty();
        };
    }

    public static URL findChartLocation(final @NotNull Console console, final @Nullable String chartName) {
        return Stream.of(console.getOakpalPath())
                .flatMap(Loader.findChartInPathFile(Optional.ofNullable(chartName).orElse(ENTRY_OAKPAL_CHART)))
                .findFirst()
                .orElse(Chart.DEFAULT_CHART_URL);
    }
}
