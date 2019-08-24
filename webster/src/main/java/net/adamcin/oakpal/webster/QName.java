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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import java.util.Objects;

import static net.adamcin.oakpal.core.Fun.result1;

/**
 * Representation of a Qualified Name.
 */
public final class QName {
    private final Type type;
    private final String prefix;
    private final String localName;
    private final String uri;

    /**
     * Create a new QName.
     *
     * @param type      namespace or nodetype
     * @param prefix    the associated namespace prefix
     * @param localName the unqualified local name
     * @param uri       the namespace URI
     */
    public QName(final @NotNull Type type,
                 final @Nullable String prefix,
                 final @NotNull String localName,
                 final @Nullable String uri) {
        this.type = type;
        this.prefix = prefix;
        this.localName = localName;
        this.uri = uri;
    }

    public static final class UnqualifiedNameException extends RuntimeException {
        UnqualifiedNameException(final @NotNull String jcrName) {
            super(jcrName + " is not a qualified name (expected prefix:localName or {uri}localName format)");
        }
    }

    public static QName parseQName(final @NotNull NamespaceMapping mapping,
                                   final @NotNull Type type,
                                   final @NotNull String qName) {
        if (qName.contains(":")) {
            final String[] parts = qName.split(":", 2);
            final String prefix = parts[0];
            final String localName = parts[1];
            return new QName(type, prefix, localName,
                    result1(mapping::getURI).apply(prefix).toOptional().orElse(null));
        } else if (qName.startsWith("{") && qName.contains("}")) {
            final int lastBrace = qName.indexOf("}");
            final String uri = qName.substring(1, lastBrace);
            final String localName = qName.substring(lastBrace + 1);
            return new QName(type,
                    result1(mapping::getPrefix).apply(uri).toOptional().orElse(null),
                    localName, uri);
        }
        throw new UnqualifiedNameException(qName);
    }

    public static QName adaptName(final @NotNull NamespaceMapping mapping,
                                  final @NotNull Type type,
                                  final @NotNull Name name) {
        return new QName(type, result1(mapping::getPrefix)
                .apply(name.getNamespaceURI()).toOptional().orElse(null),
                name.getLocalName(), name.getNamespaceURI());
    }

    public Type getType() {
        return type;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLocalName() {
        return localName;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        if (this.prefix != null) {
            return getPrefix() + ":" + getLocalName();
        } else {
            return toExpandedForm();
        }
    }

    public String toExpandedForm() {
        return "{" + getUri() + "}" + getLocalName();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QName qName = (QName) o;
        return type == qName.type &&
                localName.equals(qName.localName) &&
                uri.equals(qName.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, localName, uri);
    }

    public enum Type {
        PRIVILEGE, NODETYPE
    }
}
