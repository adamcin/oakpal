/*
 * Copyright 2020 Mark Adamcin
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

package net.adamcin.oakpal.core.opear;

import net.adamcin.oakpal.api.Result;
import net.adamcin.oakpal.core.OakpalPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.function.Function;

import static net.adamcin.oakpal.api.Fun.compose;
import static net.adamcin.oakpal.api.Fun.result1;

/**
 * Simpler opear implementation for CLI and other runtime-generated contexts.
 */
public final class AdhocOpear implements Opear {
    private final URL planFileUrl;
    private final URL baseUrl;

    public AdhocOpear(final @NotNull URL planFileUrl, final @NotNull URL baseUrl) {
        this.planFileUrl = planFileUrl;
        this.baseUrl = baseUrl;
    }

    @Override
    public URL getDefaultPlan() {
        return planFileUrl;
    }

    @Override
    public Result<URL> getSpecificPlan(final @NotNull String planName) {
        return Result.success(planFileUrl);
    }

    @Override
    public ClassLoader getPlanClassLoader(final @NotNull ClassLoader parent) {
        return new URLClassLoader(new URL[]{baseUrl}, parent);
    }

    public static Result<AdhocOpear> fromPlanFile(final @NotNull File planFile, final @Nullable File baseDir) {
        final Function<File, Result<URL>> fnFileUrl = compose(File::toURI, result1(URI::toURL));
        final Result<URL> planFileUrlResult = fnFileUrl.apply(planFile);
        return planFileUrlResult
                .flatMap(planFileUrl -> fnFileUrl.apply(Optional.ofNullable(baseDir).orElse(planFile.getParentFile()))
                        .flatMap(baseUrl ->
                                OakpalPlan.fromJson(planFileUrl).map(plan ->
                                        new AdhocOpear(planFileUrl, baseUrl))));
    }
}
