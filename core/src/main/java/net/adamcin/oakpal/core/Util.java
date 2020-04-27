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

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Domain;
import net.adamcin.oakpal.api.Fun;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private Util() {
        // do nothing
    }

    static boolean isEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    /**
     * Public utility method to wrap an existing session with a facade that blocks writes.
     *
     * @param session the existing session to wrap
     * @return a read-only session
     */
    public static Session wrapSessionReadOnly(final Session session) {
        return SessionFacade.findBestWrapper(session, false);
    }

    public static List<String> getManifestHeaderValues(final Manifest manifest, final String headerName) {
        Domain domain = Domain.domain(manifest);
        Parameters params = domain.getParameters(headerName);
        return new ArrayList<>(params.keySet());
    }

    public static String escapeManifestHeaderValue(final @NotNull String... values) {
        return escapeManifestHeaderValues(Arrays.asList(values));
    }

    public static String escapeManifestHeaderValues(final @NotNull List<String> values) {
        Parameters parameters = new Parameters();
        values.stream().forEachOrdered(value -> parameters.put(value, new Attrs()));
        return parameters.toString();
    }

    public static List<URL> resolveManifestResources(final URL manifestUrl, final List<String> resources) {
        return resources.stream()
                .map(name -> name.contains(":") ? name : "../" + name)
                .flatMap(composeTry(Stream::of, Stream::empty,
                        (relPath) -> new URL(manifestUrl, relPath),
                        (relPath, error) ->
                                LOGGER.debug("[resolveManifestResources] malformed url: manifestUrl={}, relPath={}, error={}",
                                        manifestUrl, relPath, error.getMessage())))
                .collect(Collectors.toList());
    }

    public static Map<URL, List<URL>> mapManifestHeaderResources(final String headerName, final ClassLoader classLoader) throws IOException {
        Map<URL, List<URL>> map = new LinkedHashMap<>();
        Enumeration<URL> resEnum = classLoader.getResources(JarFile.MANIFEST_NAME);
        while (resEnum.hasMoreElements()) {
            URL url = resEnum.nextElement();
            try (InputStream is = url.openStream()) {
                Manifest manifest = new Manifest(is);
                List<URL> headerResources = resolveManifestResources(url, getManifestHeaderValues(manifest, headerName));
                map.put(url, headerResources);
            }
        }

        return map;
    }

    public static Map<URL, List<URL>> mapManifestHeaderResources(final String headerName, final List<File> files) throws IOException {
        Map<URL, List<URL>> map = new LinkedHashMap<>();
        for (File zipFile : files) {
            if (!zipFile.exists() || zipFile.isDirectory()) {
                File manifestFile = new File(zipFile, JarFile.MANIFEST_NAME);
                if (manifestFile.exists()) {
                    try (InputStream fis = new FileInputStream(manifestFile)) {
                        Manifest manifest = new Manifest(fis);
                        final URL manifestUrl = manifestFile.toURI().toURL();
                        map.put(manifestUrl, resolveManifestResources(manifestUrl,
                                getManifestHeaderValues(manifest, headerName)));
                    }
                }
            } else {
                try (JarFile jar = new JarFile(zipFile)) {
                    Manifest manifest = jar.getManifest();
                    final URL manifestUrl = new URL(String.format("jar:%s!/%s",
                            zipFile.toURI().toURL().toExternalForm(), JarFile.MANIFEST_NAME));
                    map.put(manifestUrl, resolveManifestResources(manifestUrl,
                            getManifestHeaderValues(manifest, headerName)));
                }
            }
        }
        return map;
    }

    public static Map<URL, List<URL>> mapManifestHeaderResources(final String headerName, final URL manifestUrl) throws IOException {
        Map<URL, List<URL>> map = new LinkedHashMap<>();
        try (InputStream is = manifestUrl.openStream()) {
            Manifest manifest = new Manifest(is);
            map.put(manifestUrl, resolveManifestResources(manifestUrl,
                    getManifestHeaderValues(manifest, headerName)));
        }
        return map;
    }

    static ClassLoader getDefaultClassLoader() {
        return Thread.currentThread().getContextClassLoader() != null
                ? Thread.currentThread().getContextClassLoader()
                : Util.class.getClassLoader();
    }

    /**
     * Logger function to inject into a stream by way of {@code filter()} method. Calls {@code logger.debug()} with the
     * provided {@code format} string and the current stream element as a format argument.
     *
     * @param logger the logger to use
     * @param format the SLF4j format string (use curly braces for placeholders)
     * @param <T>    the captured type parameter for the stream element
     * @return a single-argument predicate for use in a {@code Stream.filter()} method
     */
    public static <T> Predicate<T> debugFilter(final Logger logger, final String format) {
        return item -> {
            logger.debug(format, item);
            return true;
        };
    }

    /**
     * Logger function to inject into a stream by way of {@code filter()} method. Calls {@code logger.trace()} with the
     * provided {@code format} string and the current stream element as a format argument.
     *
     * @param logger the logger to use
     * @param format the SLF4j format string (use curly braces for placeholders)
     * @param <T>    the captured type parameter for the stream element
     * @return a single-argument predicate for use in a {@code Stream.filter()} method
     */
    public static <T> Predicate<T> traceFilter(final Logger logger, final String format) {
        return item -> {
            logger.trace(format, item);
            return true;
        };
    }

    /**
     * Composes four lambdas into a single function for use with flatMap() defined by {@link Stream},
     * {@link java.util.Optional}, etc. Useful for eliminating clumsy try/catch blocks from lambdas.
     *
     * @param monadUnit the "unit" (or "single") function defined by the appropriate monad. I.E. Stream::of,
     *                  Optional::of, or Optional::ofNullable.
     * @param monadZero the "zero" (or "empty") function defined by the appropriate monad, as in Stream::empty,
     *                  or Optional::empty
     * @param onElement some function that produces type {@code R} when given an object of type {@code T}, or fails
     *                  with an Exception.
     * @param onError   an optional consumer function to perform some logic when the parser function throws.
     *                  Receives both the failing input element and the caught Exception.
     * @param <M>       The captured monad type, which must match the return types of the {@code monadUnit} and
     *                  {@code monadZero} functions, but which is not involved in the {@code onElement} or
     *                  {@code onError} functions.
     * @param <T>       The input type mapped by the monad, i.e. the String type in {@code Stream<String>}.
     * @param <R>       The output type mapped by the monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a flatMappable function
     * @see Fun#composeTry1(Function, Supplier, Fun.ThrowingFunction, BiConsumer)
     * @deprecated 1.3.0 use {@link Fun#composeTry1(Function, Supplier, Fun.ThrowingFunction, BiConsumer)}
     */
    @Deprecated
    public static <M, T, R> Function<T, M> composeTry(final Function<R, M> monadUnit,
                                                      final Supplier<M> monadZero,
                                                      final TryFunction<T, R> onElement,
                                                      final BiConsumer<T, Exception> onError) {
        return Fun.composeTry1(monadUnit, monadZero, onElement, onError);
    }

    /**
     * Functional interface similar to {@link Function}, but which allows for throwing exceptions. Use with
     * {@link #composeTry(Function, Supplier, TryFunction, BiConsumer)} to make suitable for {@code flatMap}.
     *
     * @param <T> input type
     * @param <R> output type
     * @deprecated 1.3.0 use {@link Fun.ThrowingFunction}
     */
    @Deprecated
    public interface TryFunction<T, R> extends Fun.ThrowingFunction<T, R> {
    }

    /**
     * Compose a function with {@code Optional::ofNullable} to wrap the output type.
     *
     * @param inputFunc the input function
     * @param <T>       input type
     * @param <R>       input function return type
     * @return a function returning an optional of the input function return type
     * @deprecated 1.3.0 use {@code Fun.compose(Optional::ofNullable, inputFunc)}
     */
    @Deprecated
    public static <T, R> Function<T, Optional<R>> optFunc(final Function<T, R> inputFunc) {
        return ((Function<R, Optional<R>>) Optional::ofNullable).compose(inputFunc);
    }

    /**
     * Compose two functions, left-to-right.
     *
     * @param before the left function
     * @param after  the right function
     * @param <T>    the input type
     * @param <I>    the intermediate type
     * @param <R>    the output type
     * @return a composed function from {@code T} to {@code R}
     * @deprecated 1.3.0 use {@link Fun#compose1(Function, Function)} instead
     */
    @Deprecated
    public static <T, I, R> Function<T, R> compose(final Function<T, I> before, final Function<I, R> after) {
        return after.compose(before);
    }

}
