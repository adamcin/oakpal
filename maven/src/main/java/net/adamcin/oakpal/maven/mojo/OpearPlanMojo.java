package net.adamcin.oakpal.maven.mojo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import net.adamcin.oakpal.core.OakpalPlan;
import net.adamcin.oakpal.maven.component.OakpalComponentConfigurator;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Exports a plan builder configuration for inclusion in an opear file, using the {@link OpearPackageMojo}.
 *
 * @since 1.4.0
 */
@Mojo(name = "opear-plan",
        requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM,
        configurator = OakpalComponentConfigurator.HINT,
        defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class OpearPlanMojo extends AbstractCommonMojo implements MojoWithPlanParams {

    @Parameter(name = "planFile", defaultValue = "${project.build.directory}/opear-plans/plan.json", required = true)
    private File planFile;

    @Parameter(name = "planParams")
    private PlanParams planParams;

    @Override
    public void execute() throws MojoFailureException {
        planFile.getParentFile().mkdirs();

        final JsonWriterFactory writerFactory =
                Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(planFile), StandardCharsets.UTF_8);
             JsonWriter writer = writerFactory.createWriter(osw)) {
            Thread.currentThread().setContextClassLoader(createContainerClassLoader());
            OakpalPlan plan = buildPlan();
            writer.writeObject(plan.toJson());
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write plan json to file " + planFile.getAbsolutePath(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    @Override
    public PlanBuilderParams getPlanBuilderParams() {
        return Optional.ofNullable(planParams).orElse(new PlanParams());
    }
}
