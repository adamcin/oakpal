/*
 * Copyright 2019 Mark Adamcin
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

package net.adamcin.oakpal.webster.targets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.jcr.Session;
import javax.json.JsonObject;

import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.Rules;
import net.adamcin.oakpal.webster.ArchiveAware;
import net.adamcin.oakpal.webster.CndExporter;
import net.adamcin.oakpal.webster.FileVaultNameFinder;
import net.adamcin.oakpal.webster.QName;
import net.adamcin.oakpal.webster.WebsterTarget;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports a nodetypes.cnd file from a source Oak repository to support a filevault archive.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code scopeExportNames}</dt>
 * <dd>A list of {@link Rule}s with patterns matched against nodetype JCR names (like "nt:folder", "nt:unstructured"),
 * which restricts the scope for the export. Rules are evaluated top-to-bottom. The type of the last rule to match is
 * the effective action taken for the element. Export scope includes all nodetypes by default.</dd>
 * <dt>{@code scopeReplaceNames}</dt>
 * <dd>A list of {@link Rule}s with patterns matched against nodetype JCR names (like "nt:folder", "nt:unstructured"),
 * which restricts the scope of replacement of nodetype definitions that are already defined in the target CND file, if
 * it exists. By excluding certain nodetypes from replacement, you can make the target CND file the authoritative source
 * for their definitions. Replacement scope includes all nodetypes by default.</dd>
 * <dt>{@code includeBuiltins}</dt>
 * <dd>({@code boolean}, defaults to {@code false}). Oak defines a set of builtin nodetypes which are not normally
 * included in application package CNDs. If you wish to export and include builtin nodetypes for package deployment on
 * a non-Oak JCR repository, you will need to set this to {@code true}.</dd>
 * </dl>
 */
final class WebsterNodetypesTarget implements WebsterTarget, ArchiveAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsterNodetypesTarget.class);

    static final String KEY_SCOPE_EXPORT_NAMES = "scopeExportNames";
    static final String KEY_SCOPE_REPLACE_NAMES = "scopeReplaceNames";
    static final String KEY_INCLUDE_BUILTINS = "includeBuiltins";

    private final File targetFile;
    private final CndExporter exporter;
    private Archive archive;
    private File writeBackDir;

    private WebsterNodetypesTarget(final File targetFile, final CndExporter exporter) {
        this.targetFile = targetFile;
        this.exporter = exporter;
    }

    @Override
    public void setArchive(final Archive archive, final File writeBackDir) {
        this.archive = archive;
        this.writeBackDir = writeBackDir;
    }

    @Override
    public void perform(final Session session) throws Exception {
        final File nodetypesCnd = targetFile == null
                ? new File(writeBackDir, Constants.META_DIR + File.separator + Constants.NODETYPES_CND)
                : targetFile;

        final List<String> ntNames = new ArrayList<>();

        Set<QName> qNames = new FileVaultNameFinder().search(archive);
        qNames.stream()
                .filter(qName -> qName.getType() == QName.Type.NODETYPE)
                .map(QName::toString).forEachOrdered(ntNames::add);

        nodetypesCnd.getParentFile().mkdirs();

        exporter.writeNodetypes(nodetypesCnd, session, ntNames);
        LOGGER.info("Nodetypes CND written to {}", nodetypesCnd.getAbsolutePath());
    }

    static WebsterNodetypesTarget fromJson(final File target, final JsonObject config) {
        LOGGER.debug("[fromJson] fromJson: {}", config.toString());

        CndExporter.Builder builder = new CndExporter.Builder();

        if (config.containsKey(KEY_SCOPE_EXPORT_NAMES)) {
            List<Rule> scopeExportNames = Rules.fromJsonArray(config.getJsonArray(KEY_SCOPE_EXPORT_NAMES));
            builder.withScopeExportNames(scopeExportNames);
        }

        if (config.containsKey(KEY_SCOPE_REPLACE_NAMES)) {
            List<Rule> scopeReplaceNames = Rules.fromJsonArray(config.getJsonArray(KEY_SCOPE_REPLACE_NAMES));
            builder.withScopeReplaceNames(scopeReplaceNames);
        }

        if (config.containsKey(KEY_INCLUDE_BUILTINS)) {
            builder.withIncludeBuiltins(config.getBoolean(KEY_INCLUDE_BUILTINS, false));
        }

        return new WebsterNodetypesTarget(target, builder.build());

    }
}
