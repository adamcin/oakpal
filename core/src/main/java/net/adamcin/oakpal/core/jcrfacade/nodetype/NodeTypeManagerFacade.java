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

package net.adamcin.oakpal.core.jcrfacade.nodetype;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import net.adamcin.oakpal.core.ListenerReadOnlyException;

/**
 * Wraps {@link NodeTypeManager} to block node type registration.
 */
public class NodeTypeManagerFacade implements NodeTypeManager {
    private final NodeTypeManager delegate;

    public NodeTypeManagerFacade(NodeTypeManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public NodeType getNodeType(String nodeTypeName) throws RepositoryException {
        return delegate.getNodeType(nodeTypeName);
    }

    @Override
    public boolean hasNodeType(String name) throws RepositoryException {
        return delegate.hasNodeType(name);
    }

    @Override
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        return delegate.getAllNodeTypes();
    }

    @Override
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        return delegate.getPrimaryNodeTypes();
    }

    @Override
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        return delegate.getMixinNodeTypes();
    }

    @Override
    public NodeTypeTemplate createNodeTypeTemplate() throws RepositoryException {
        return delegate.createNodeTypeTemplate();
    }

    @Override
    public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd) throws RepositoryException {
        return delegate.createNodeTypeTemplate(ntd);
    }

    @Override
    public NodeDefinitionTemplate createNodeDefinitionTemplate() throws RepositoryException {
        return delegate.createNodeDefinitionTemplate();
    }

    @Override
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate() throws RepositoryException {
        return delegate.createPropertyDefinitionTemplate();
    }

    @Override
    public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public NodeTypeIterator registerNodeTypes(NodeTypeDefinition[] ntds, boolean allowUpdate) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void unregisterNodeType(String name) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }

    @Override
    public void unregisterNodeTypes(String[] names) throws RepositoryException {
        throw new ListenerReadOnlyException();
    }
}
