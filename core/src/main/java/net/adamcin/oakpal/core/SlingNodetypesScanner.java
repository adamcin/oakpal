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
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Singleton class that fetches all node type definitions from OSGi bundle MANIFEST.MF files
 * with "Sling-Nodetypes" definitions in the classpath.
 * Additionally it support registering them to a JCR repository.
 */
public final class SlingNodetypesScanner {
    /**
     * Find all node type definition classpath paths by searching all MANIFEST.MF files in the classpath and reading
     * the paths from the "Sling-Nodetypes" entry.
     * The order of the paths from each entry is preserved, but the overall order when multiple bundles define such an entry
     * is not deterministic and may not be correct according to the dependencies between the node type definitions.
     *
     * @return List of node type definition class paths
     */
    public static List<URL> findNodeTypeDefinitions() throws IOException {
        return findNodeTypeDefinitions(SlingNodetypesScanner.class.getClassLoader());
    }

    /**
     * Find all node type definition classpath paths by searching all MANIFEST.MF files in the classpath and reading
     * the paths from the "Sling-Nodetypes" entry.
     * The order of the paths from each entry is preserved, but the overall order when multiple bundles define such an entry
     * is not deterministic and may not be correct according to the dependencies between the node type definitions.
     *
     * @return List of node type definition class paths
     */
    public static List<URL> findNodeTypeDefinitions(final ClassLoader classLoader) throws IOException {
        List<String> resourceNames = new ArrayList<>();
        Enumeration<URL> resEnum = classLoader.getResources(JarFile.MANIFEST_NAME);
        while (resEnum.hasMoreElements()) {
            URL url = resEnum.nextElement();
            try (InputStream is = url.openStream()) {
                Manifest manifest = new Manifest(is);
                resourceNames.addAll(getSlingNodetypesFromManifest(manifest));
            }
        }

        return new ArrayList<>(resolveNodeTypeDefinitions(classLoader, resourceNames).values());
    }

    /**
     * Find all node type definition classpath paths by searching all MANIFEST.MF files in the classpath and reading
     * the paths from the "Sling-Nodetypes" entry.
     * The order of the paths from each entry is preserved, but the overall order when multiple bundles define such an entry
     * is not deterministic and may not be correct according to the dependencies between the node type definitions.
     *
     * @return List of node type definition class paths
     */
    public static List<URL> findNodeTypeDefinitions(final List<File> zipFiles) throws IOException {
        List<String> resourceNames = new ArrayList<>();
        for (File zipFile : zipFiles) {
            if (!zipFile.exists() || zipFile.isDirectory()) {
                File manifestFile = new File(zipFile, JarFile.MANIFEST_NAME);
                if (manifestFile.exists()) {
                    try (InputStream fis = new FileInputStream(manifestFile)) {
                        Manifest manifest = new Manifest(fis);
                        resourceNames.addAll(getSlingNodetypesFromManifest(manifest));
                    }
                }
            } else {
                try (JarFile jar = new JarFile(zipFile)) {
                    Manifest manifest = jar.getManifest();
                    resourceNames.addAll(getSlingNodetypesFromManifest(manifest));
                }
            }
        }

        return new ArrayList<>(resolveNodeTypeDefinitions(zipFiles, resourceNames).values());
    }


    public static List<String> getSlingNodetypesFromManifest(final Manifest manifest) {
        List<String> resourceNames = new ArrayList<>();
        Attributes mainAttribs = manifest.getMainAttributes();
        String nodeTypeDefinitionList = mainAttribs.getValue("Sling-Nodetypes");
        String[] nodeTypeDefinitionArray = nodeTypeDefinitionList != null
                ? nodeTypeDefinitionList.split(",")
                : null;
        if (nodeTypeDefinitionArray != null) {
            for (String nodeTypeDefinition : nodeTypeDefinitionArray) {
                if (!nodeTypeDefinition.trim().isEmpty()) {
                    resourceNames.add(nodeTypeDefinition.trim());
                }
            }
        }
        return resourceNames;
    }

    public static Map<String, URL> resolveNodeTypeDefinitions(final ClassLoader classLoader,
                                                              final List<String> resourceNames) {
        Map<String, URL> cndUrls = new LinkedHashMap<>();
        for (String name : resourceNames) {
            final URL cndUrl = classLoader.getResource(name);
            if (cndUrl != null) {
                cndUrls.put(name, cndUrl);
            }
        }

        return cndUrls;
    }

    public static Map<String, URL> resolveNodeTypeDefinitions(final List<File> zipFiles,
                                                              final List<String> resourceNames) throws IOException {
        Map<String, URL> cndUrls = new LinkedHashMap<>();
        for (File zipFile : zipFiles) {
            if (!zipFile.exists() || zipFile.isDirectory()) {
                for (String name : resourceNames) {
                    if (cndUrls.containsKey(name)) {
                        continue;
                    }
                    File entryFile = new File(zipFile, name);
                    if (entryFile.exists()) {
                        cndUrls.put(name, entryFile.toURI().toURL());
                    }
                }
            } else {
                try (ZipFile zip = new JarFile(zipFile)) {
                    for (String name : resourceNames) {
                        if (cndUrls.containsKey(name)) {
                            continue;
                        }
                        ZipEntry zipEntry = zip.getEntry(name);
                        if (zipEntry != null) {
                            URL cndUrl = new URL(String.format("jar:%s!/%s", zipFile.toURI().toURL().toExternalForm(), name));
                            cndUrls.put(name, cndUrl);
                        }
                    }
                }
            }
        }

        return cndUrls;
    }
}

