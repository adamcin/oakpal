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

package net.adamcin.oakpal.core.checks;

import net.adamcin.oakpal.api.EmbeddedPackageInstallable;
import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maintains a graph of package relationships for assigning responsibility for changes. Package-Private for now.
 * Use event methods defined by {@link ProgressCheck} to build the internal datastructures, and use the boolean methods
 * to query the graph.
 */
final class PackageGraph implements ProgressCheck {

    private static final class PackageGraphNode {
        private final PackageId id;
        private PackageGraphNode parent;
        private final Map<PackageId, PackageGraphNode> children = new LinkedHashMap<>();

        public PackageGraphNode(final PackageId id) {
            this.id = id;
        }

        PackageId getId() {
            return id;
        }

        PackageGraphNode getParent() {
            return parent;
        }

        Map<PackageId, PackageGraphNode> getChildren() {
            return children;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final PackageGraphNode that = (PackageGraphNode) o;
            return id.equals(that.id);
        }

        void removeAncestor(final PackageGraphNode ancestor) {
            if (ancestor != null && this.parent != null) {
                if (!ancestor.equals(this)) {
                    this.parent.removeAncestor(ancestor);
                }
                if (ancestor.equals(this.parent)) {
                    this.parent.getChildren().remove(this.id);
                    this.parent = null;
                }
            }
        }

        void setParent(final PackageGraphNode parent) {
            if (this.parent != null) {
                this.parent.getChildren().remove(this.id);
            }
            this.parent = parent;
            if (this.parent != null) {
                this.parent.removeAncestor(this);
                this.parent.getChildren().put(this.id, this);
            }
        }
    }

    final LinkedList<PackageId> identified = new LinkedList<>();
    final Map<PackageId, PackageGraphNode> nodes = new HashMap<>();

    private PackageGraphNode getOrCreateNode(final @NotNull PackageId packageId) {
        if (!nodes.containsKey(packageId)) {
            nodes.put(packageId, new PackageGraphNode(packageId));
        }
        return nodes.get(packageId);
    }

    @Override
    public Collection<Violation> getReportedViolations() {
        return Collections.emptyList();
    }

    @Override
    public void startedScan() {
        identified.clear();
        nodes.clear();
    }

    @Override
    public void identifyPackage(final PackageId packageId, final File file) {
        if (packageId != null) {
            identified.add(packageId);
            // set parent to null in case the package is first extracted as a nested package,
            // then scanned by itself explicitly
            getOrCreateNode(packageId).setParent(null);
        }
    }

    @Override
    public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
        if (packageId != null && parentId != null) {
            identified.add(packageId);
            getOrCreateNode(packageId).setParent(getOrCreateNode(parentId));
        }
    }

    @Override
    public void identifyEmbeddedPackage(final PackageId packageId, final PackageId parentId,
                                        final EmbeddedPackageInstallable slingInstallable) {
        final PackageId self = Optional.ofNullable(slingInstallable).map(EmbeddedPackageInstallable::getEmbeddedId).orElse(packageId);
        final PackageId parent = Optional.ofNullable(slingInstallable).map(EmbeddedPackageInstallable::getParentId).orElse(parentId);
        if (self != null && parent != null) {
            identified.add(self);
            getOrCreateNode(self).setParent(getOrCreateNode(parent));
        }
    }

    /**
     * Return true if the package specified by the provided Id has been identified by this graph. If it hasn't been
     * identified, the results of the other query methods are not authoritative answers.
     *
     * @param packageId the subject packageId
     * @return true if the packageId has been identified by this graph
     */
    public boolean isIdentified(final @NotNull PackageId packageId) {
        return identified.contains(packageId);
    }

    /**
     * Returns the packageId of the last identified package.
     *
     * @return the last identified packageId
     */
    public @Nullable PackageId getLastIdentified() {
        return identified.peekLast();
    }

    /**
     * Returns true if the packageId is not known to have a parent package.
     *
     * @param packageId the package id
     * @return true if packageId has no known parent in the graph
     */
    public boolean isRoot(final @NotNull PackageId packageId) {
        return getOrCreateNode(packageId).getParent() == null;
    }

    /**
     * Return true if the left packageId is a descendant of the right packageId in any way.
     *
     * @param left  the left (possible descendant) packageId
     * @param right the right (possible ancestor) packageId
     * @return true if the left is contained by the right
     */
    public boolean isLeftDescendantOfRight(final @NotNull PackageId left, final @NotNull PackageId right) {
        if (left.equals(right)) {
            return true;
        }
        return Optional.ofNullable(getOrCreateNode(left).getParent())
                .map(parent -> isLeftDescendantOfRight(parent.getId(), right))
                .orElse(false);
    }

    /**
     * Return a Stream for internal recursive use.
     *
     * @param self the leaf id
     * @return a stream containing self, followed by 0-many ancestors
     */
    private Stream<PackageId> internalGetSelfAndAncestors(final @NotNull PackageId self) {
        return Stream.concat(Stream.of(self), Optional.ofNullable(getOrCreateNode(self).getParent())
                .map(PackageGraphNode::getId)
                .map(this::internalGetSelfAndAncestors)
                .orElse(Stream.empty()));
    }

    /**
     * Get a collection of package ids that is iterable from self to root ancestor.
     *
     * @param self the leaf id
     * @return a collection of package ids that at least contains the provided packageId
     */
    public Collection<PackageId> getSelfAndAncestors(final @NotNull PackageId self) {
        return internalGetSelfAndAncestors(self).collect(Collectors.toList());
    }

    /**
     * Return a Stream for internal recursive use.
     *
     * @param self the root id
     * @return a stream containing self, followed by 0-many descendants, depth first
     */
    private Stream<PackageId> internalGetSelfAndDescendents(final @NotNull PackageId self) {
        return Stream.concat(Stream.of(self), getOrCreateNode(self).getChildren().keySet().stream()
                .flatMap(this::internalGetSelfAndDescendents));
    }

    /**
     * Get a collection of package ids comprising self and all known descendants of self.
     *
     * @param self the ancestor id
     * @return a collection of package ids starting with self and 0-N nested packages
     */
    public Collection<PackageId> getSelfAndDescendants(final @NotNull PackageId self) {
        return internalGetSelfAndDescendents(self).collect(Collectors.toList());
    }

}
