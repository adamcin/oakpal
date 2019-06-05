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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.jcr.Session;

import net.adamcin.oakpal.webster.ArchiveAware;
import net.adamcin.oakpal.webster.FileVaultNameFinder;
import net.adamcin.oakpal.webster.PrivilegeXmlExporter;
import net.adamcin.oakpal.core.QName;
import net.adamcin.oakpal.webster.WebsterTarget;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
final class WebsterPrivilegesTarget implements WebsterTarget, ArchiveAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsterPrivilegesTarget.class);

    private File targetFile;
    private Archive archive;
    private File writeBackDir;

    WebsterPrivilegesTarget(final File targetFile) {
        this.targetFile = targetFile;
    }

    @Override
    public void setArchive(final Archive archive, final File writeBackDir) {
        this.archive = archive;
        this.writeBackDir = writeBackDir;
    }

    @Override
    public void perform(final Session session) throws Exception {
        final File privilegesXmlFile = targetFile == null
                ? new File(writeBackDir, Constants.META_DIR + File.separator + Constants.PRIVILEGES_XML)
                : targetFile;

        final List<String> privNames = new ArrayList<>();

        Set<QName> qNames = new FileVaultNameFinder().search(archive);
        qNames.stream()
                .filter(qName -> qName.getType() == QName.Type.PRIVILEGE)
                .map(QName::toString).forEachOrdered(privNames::add);

        privilegesXmlFile.getParentFile().mkdirs();

        PrivilegeXmlExporter.writePrivileges(() -> new OutputStreamWriter(
                        new FileOutputStream(privilegesXmlFile), StandardCharsets.UTF_8),
                session, privNames, false);
        LOGGER.info("Privileges XML written to {}", privilegesXmlFile.getAbsolutePath());
    }
}
