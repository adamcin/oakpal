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

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.lang.reflect.Proxy;

/**
 * ClassLoader proxy for {@link PackagingService}, to side step the deprecated
 * {@link PackagingService#getPackageManager(Session)} method to avoid the nasty stack trace.
 * See recommendation here: https://issues.apache.org/jira/browse/JCRVLT-285
 */
final class DefaultPackagingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPackagingService.class);
    private static final String JCR_PACK_MAN_IMPL_CLASS = "org.apache.jackrabbit.vault.packaging.impl.JcrPackageManagerImpl";

    Class<? extends JcrPackageManager> jcrPackageManagerClazz;

    /**
     * Load the JcrPackageManagerImpl class from the oakpal classloader.
     */
    @SuppressWarnings("WeakerAccess")
    DefaultPackagingService() {
        this(DefaultPackagingService.class.getClassLoader());
    }

    /**
     * Load the JcrPackageManagerImpl class from the provided classloader.
     *
     * @param classLoader classloader to find the JcrPackageManagerImpl class
     */
    DefaultPackagingService(final @NotNull ClassLoader classLoader) {
        try {
            Class<?> clazz = classLoader.loadClass(JCR_PACK_MAN_IMPL_CLASS);
            if (JcrPackageManager.class.isAssignableFrom(clazz)) {
                jcrPackageManagerClazz = clazz.asSubclass(JcrPackageManager.class);
            } else {
                Throwable e = new IllegalStateException();
                LOGGER.warn("JcrPackageManagerImpl class from classLoader argument does not implement JcrPackageManager interface from oakpal classLoader.", e);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load the JcrPackageManagerImpl class from the provided classLoader.", e);
            jcrPackageManagerClazz = null;
        }
    }

    public PackageManager getPackageManager() {
        return PackagingService.getPackageManager();
    }

    public JcrPackageManager getPackageManager(final Session session) {
        if (jcrPackageManagerClazz != null) {
            try {
                return jcrPackageManagerClazz.getConstructor(Session.class, String[].class)
                        .newInstance(session, new String[0]);
            } catch (Exception e) {
                LOGGER.warn("failed to construct instance of " + JCR_PACK_MAN_IMPL_CLASS, e);
            }
        }
        return PackagingService.getPackageManager(session);
    }

    public JcrPackageDefinition createPackageDefinition(final Node defNode) {
        return PackagingService.createPackageDefinition(defNode);
    }

    public JcrPackage open(final Node node, final boolean allowInvalid) throws RepositoryException {
        return PackagingService.open(node, allowInvalid);
    }

    static Packaging newInstance(final @NotNull ClassLoader classLoader) {
        final DefaultPackagingService instance = new DefaultPackagingService(classLoader);
        return (Packaging) Proxy.newProxyInstance(classLoader, new Class<?>[]{Packaging.class},
                (proxy, method, args) -> {
                    if ("getPackageManager".equals(method.getName()) && (args == null || args.length == 0)) {
                        return instance.getPackageManager();
                    } else if ("getPackageManager".equals(method.getName()) && args[0] instanceof Session) {
                        return instance.getPackageManager((Session) args[0]);
                    } else if ("createPackageDefinition".equals(method.getName()) && args[0] instanceof Node) {
                        return instance.createPackageDefinition((Node) args[0]);
                    } else if ("open".equals(method.getName()) && args[0] instanceof Node && args[1] instanceof Boolean) {
                        return instance.open((Node) args[0], (Boolean) args[1]);
                    } else {
                        throw new UnsupportedOperationException("method not implemented: " + method);
                    }
                });
    }
}
