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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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
import java.util.stream.StreamSupport;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Domain;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private Util() {
        // do nothing
    }

    public static List<String> getManifestHeaderValues(final Manifest manifest, final String headerName) {
        Domain domain = Domain.domain(manifest);
        Parameters params = domain.getParameters(headerName);
        return new ArrayList<>(params.keySet());
    }

    public static List<URL> resolveManifestResources(final URL manifestUrl, final List<String> resources) {
        return resources.stream()
                .map(name -> "../" + name)
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
                List<URL> headerResources = resolveManifestResources(url, Util.getManifestHeaderValues(manifest, headerName));
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
                                Util.getManifestHeaderValues(manifest, headerName)));
                    }
                }
            } else {
                try (JarFile jar = new JarFile(zipFile)) {
                    Manifest manifest = jar.getManifest();
                    final URL manifestUrl = new URL(String.format("jar:%s!/%s",
                            zipFile.toURI().toURL().toExternalForm(), JarFile.MANIFEST_NAME));
                    map.put(manifestUrl, resolveManifestResources(manifestUrl,
                            Util.getManifestHeaderValues(manifest, headerName)));
                }
            }
        }
        return map;
    }

    static ClassLoader getDefaultClassLoader() {
        return Thread.currentThread().getContextClassLoader() != null
                ? Thread.currentThread().getContextClassLoader()
                : Util.class.getClassLoader();
    }

    public static <T> Predicate<T> debugFilter(final Logger logger, final String format) {
        return item -> {
            logger.debug(format, item);
            return true;
        };
    }

    public static <T> Predicate<T> traceFilter(final Logger logger, final String format) {
        return item -> {
            logger.trace(format, item);
            return true;
        };
    }

    public static <T> List<T> fromJSONArray(final JSONArray jsonArray, final Function<JSONObject, T> mapper) {
        List<JSONObject> onlyObjects = new ArrayList<>();
        List<T> results = new ArrayList<>();
        Optional.ofNullable(jsonArray).map(array -> StreamSupport.stream(array.spliterator(), false)
                .filter(json -> json instanceof JSONObject)
                .map(JSONObject.class::cast).collect(Collectors.toList()))
                .ifPresent(onlyObjects::addAll);

        for (JSONObject json : onlyObjects) {
            results.add(mapper.apply(json));
        }
        return Collections.unmodifiableList(results);
    }

    public static List<String> fromJSONArrayAsStrings(final JSONArray jsonArray) {
        return Optional.ofNullable(jsonArray)
                .map(array -> StreamSupport.stream(array.spliterator(), false)
                        .map(String::valueOf).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static <T> List<T> fromJSONArrayParsed(final JSONArray jsonArray,
                                                  final TryFunction<String, T> parser,
                                                  final BiConsumer<String, Exception> errorConsumer) {
        return Optional.ofNullable(jsonArray)
                .map(elements -> StreamSupport.stream(elements.spliterator(), false)
                        .map(String::valueOf)
                        .flatMap(composeTry(Stream::of, Stream::empty, parser, errorConsumer))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    /**
     * Composes four lambdas into a single function for use with flatMap() defined by {@link Stream}, {@link Optional},
     * etc. Useful for eliminating clumsy try/catch blocks from lambdas.
     *
     * @param monadUnit the "unit" (or "single") function defined by the appropriate monad. I.E. Stream::of,
     *                  Optional::of, or Optional::ofNullable.
     * @param monadZero the "zero" (or "empty") function defined by the appropriate monad, as in Stream::empty,
     *                  or Optional::empty
     * @param onNext    some function that produces type {@code R} when given an object of type {@code T}, or fails
     *                  with an Exception.
     * @param onError   an optional consumer function to perform some logic when the parser function throws.
     *                  Receives both the failing input and the caught Exception.
     * @param <M>       The captured monad type, which must match the return types of the {@code monadUnit} and
     *                  {@code monadEmpty} functions, but which is not involved in the {@code converter} or
     *                  {@code errorConsumer} functions.
     * @param <T>       The input type mapped by the monad, i.e. the String type in {@code Stream<String>}.
     * @param <R>       The output type mapped by the monad, i.e. the URL type in {@code Stream<URL>}.
     * @return a flatMappable function
     */
    public static <M, T, R> Function<T, M> composeTry(final Function<R, M> monadUnit,
                                                      final Supplier<M> monadZero,
                                                      final TryFunction<T, R> onNext,
                                                      final BiConsumer<T, Exception> onError) {
        final BiConsumer<T, Exception> consumeError = onError != null
                ? onError
                : (e, t) -> {
        };

        return (element) -> {
            try {
                return monadUnit.apply(onNext.apply(element));
            } catch (final Exception e) {
                consumeError.accept(element, e);
                return monadZero.get();
            }
        };
    }

    /**
     * Functional interface similar to {@link Function}, but which allows for throwing exceptions. Use with
     * {@link #composeTry(Function, Supplier, TryFunction, BiConsumer)} to make suitable for {@code flatMap}.
     *
     * @param <T> input type
     * @param <R> output type
     */
    @FunctionalInterface
    public interface TryFunction<T, R> {
        R apply(T element) throws Exception;
    }
}
