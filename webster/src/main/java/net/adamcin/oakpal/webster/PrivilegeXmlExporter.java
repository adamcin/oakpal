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

package net.adamcin.oakpal.webster;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.security.Privilege;

import net.adamcin.oakpal.core.Fun;
import net.adamcin.oakpal.core.JsonCnd;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionWriter;
import org.jetbrains.annotations.NotNull;

/**
 * Interface independent logic for exporting privileges.xml files from a JCR session.
 *
 * @see org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionReader
 */
public final class PrivilegeXmlExporter {

    private PrivilegeXmlExporter() {
        // no construct
    }

    /**
     * Function type that provides a Writer.
     */
    @FunctionalInterface
    public interface WriterOpener {
        @NotNull Writer open() throws IOException;
    }

    /**
     * Serialize the desired privileges as an XML document to the provided stream writer.
     *
     * @param writerOpener    an opener function that provides the desired output stream.
     * @param session         the JCR session
     * @param onlyNames       the list of privilege names to export
     * @param includeBuiltins false to exclude builtin privileges from the output (even when specified in onlyNames)
     * @throws RepositoryException for missing nodetypes or any other repository exception
     * @throws IOException         for I/O errors
     */
    public static void writePrivileges(final WriterOpener writerOpener,
                                       final Session session,
                                       final List<String> onlyNames,
                                       final boolean includeBuiltins)
            throws RepositoryException, IOException {

        final Workspace workspace = session.getWorkspace();
        if (!(workspace instanceof JackrabbitWorkspace)) {
            throw new RepositoryException("Workspace must be instance of JackrabbitWorkspace, but isn't. type: " +
                    workspace.getClass().getName());
        }

        // get a name mapper function
        final NamePathResolver resolver = new DefaultNamePathResolver(session);
        final Function<String, Name> mapper = Fun.tryOrDefault1(resolver::getQName, null);
        // first resolve the builtins to qualified Names
        final Set<Name> builtinPrivileges = JsonCnd.BUILTIN_PRIVILEGES.stream()
                .map(mapper).filter(Objects::nonNull).collect(toSet());
        // create the builtinFilter for reuse
        final Predicate<Name> builtinFilter = includeBuiltins
                ? name -> true
                : ((Predicate<Name>) builtinPrivileges::contains).negate();
        // construct the desiredPrivileges set based on the names argument, filtering by the above builtinFilter
        final Set<Name> explicitNames = (onlyNames != null)
                ? onlyNames.stream().map(mapper).filter(Objects::nonNull).filter(builtinFilter).collect(toSet())
                : Collections.emptySet();
        // finally construct the privilegeFilter, which either includes only the desired
        final Predicate<Name> shouldInclude = !explicitNames.isEmpty()
                ? explicitNames::contains
                : builtinFilter;

        final PrivilegeManager privilegeManager = ((JackrabbitWorkspace) workspace).getPrivilegeManager();
        final List<PrivilegeDefinition> privilegeDefinitions = new ArrayList<>();
        for (Privilege privilege : privilegeManager.getRegisteredPrivileges()) {
            if (!shouldInclude.test(mapper.apply(privilege.getName()))) {
                continue;
            }
            final PrivilegeDefinitionImpl privilegeDefinition =
                    new PrivilegeDefinitionImpl(
                            mapper.apply(privilege.getName()),
                            privilege.isAbstract(),
                            Stream.of(privilege.getDeclaredAggregatePrivileges())
                                    .map(Privilege::getName)
                                    .map(mapper)
                                    .filter(Objects::nonNull)
                                    .collect(toSet()));
            privilegeDefinitions.add(privilegeDefinition);
        }

        // identify only those namespaces referenced by included privilege names or their aggregate members
        final Set<String> uris = privilegeDefinitions.stream()
                .flatMap(def -> Stream.concat(Stream.of(def.getName()), def.getDeclaredAggregateNames().stream()))
                .map(Name::getNamespaceURI).collect(toSet());

        final Map<String, String> namespaces = uris.stream()
                .collect(toMap(Fun.tryOrDefault1(session::getNamespacePrefix, null),
                        Function.identity()));

        PrivilegeDefinitionWriter pdWriter = new PrivilegeDefinitionWriter("text/xml");
        try (Writer writer = writerOpener.open()) {
            pdWriter.writeDefinitions(writer, privilegeDefinitions.toArray(new PrivilegeDefinition[0]), namespaces);
        }
    }
}
