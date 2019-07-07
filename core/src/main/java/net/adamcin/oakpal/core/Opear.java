package net.adamcin.oakpal.core;

import java.io.File;
import java.net.URL;

import org.jetbrains.annotations.NotNull;

/**
 * OPEAR stands for "OakPal Encapsulated ARchive".
 */
public interface Opear {

    String MF_BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
    String MF_BUNDLE_VERSION = "Bundle-Version";
    String MF_CLASS_PATH = "Bundle-ClassPath";
    String MF_OAKPAL_VERSION = "Oakpal-Version";
    String MF_OAKPAL_PLAN = "Oakpal-Plan";
    String SIMPLE_DIR_PLAN = "plan.json";

    /**
     * Get the default plan url specfied by the opear manifest, which may be {@link OakpalPlan#EMPTY_PLAN_URL} if no
     * other plan is exported.
     *
     * @return the default plan url
     */
    URL getDefaultPlan();

    /**
     * When the opear exports multiple plans, use this method to request a specific plan other than the default. Will
     * return {@link Result#failure(String)} if the specified plan name is not found.
     *
     * @param planName the specified plan name (relative path within opear)
     * @return a URL if successful, or an error if plan is not found
     */
    Result<URL> getSpecificPlan(final @NotNull String planName);

    /**
     * Get a URL classloader constructed for this opear using the provided classloader as the parent.
     *
     * @param parent the parent classloader
     * @return a classloader with permission to load all classes and resources in the opear
     */
    ClassLoader getPlanClassLoader(final @NotNull ClassLoader parent);


}
