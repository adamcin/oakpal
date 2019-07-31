package net.adamcin.oakpal.core;

import static net.adamcin.oakpal.core.Fun.inSet;
import static net.adamcin.oakpal.core.Fun.result1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.jetbrains.annotations.NotNull;

/**
 * Given all the JCR namespace gymnastics that have to be performed for {@link JsonCnd} and webster, dealing with
 * builtins and aggregation and references by prefix or uri, this class was created to encapsulate the resolution logic
 * to map {@link NamespaceMapping} objects to lists of {@link JcrNs} objects in a standard way.
 *
 * @see JsonCnd#toJcrNsList(NamespaceMapping, NamespaceMappingRequest) for how this type might be used
 */
public final class NamespaceMappingRequest {
    private static final Set<String> BUILTIN_PREFIXES = QName.BUILTIN_MAPPINGS.getPrefixToURIMapping().keySet();
    private static final Set<String> BUILTIN_URIS = QName.BUILTIN_MAPPINGS.getURIToPrefixMapping().keySet();
    private final Set<String> retainPrefixes;
    private final Set<String> retainUris;

    private NamespaceMappingRequest(final @NotNull Set<String> retainPrefixes,
                                    final @NotNull Set<String> retainUris,
                                    final boolean retainBuiltins) {
        this.retainPrefixes = retainBuiltins ? retainPrefixes
                : retainPrefixes.stream().filter(inSet(BUILTIN_PREFIXES).negate()).collect(Collectors.toSet());
        this.retainUris = retainBuiltins ? retainUris
                : retainUris.stream().filter(inSet(BUILTIN_URIS).negate()).collect(Collectors.toSet());
    }

    /**
     * Build a {@link NamespaceMappingRequest} by specifying which namespace mappings (by prefix or by uri) should be
     * retained. Builtin namespace mappings are not retained by default, because they are added back in when
     * converting a list of {@link JcrNs} mappings to a {@link NamespaceMapping} using
     * {@link JsonCnd#toNamespaceMapping(List)}.
     */
    public static final class Builder {
        private final Set<String> retainPrefixes = new HashSet<>();
        private final Set<String> retainUris = new HashSet<>();
        private boolean retainBuiltins;

        /**
         * Specify a namespace prefix that should be retained by the request.
         *
         * @param prefix the namespace prefix
         * @return this builder
         */
        public Builder withRetainPrefix(final @NotNull String prefix) {
            this.retainPrefixes.add(prefix);
            return this;
        }

        /**
         * Specify a namespace uri that should be retained by the request.
         *
         * @param uri the namespace uri
         * @return this builder
         */
        public Builder withRetainUri(final @NotNull String uri) {
            this.retainUris.add(uri);
            return this;
        }

        /**
         * Specify whether Oak-builtin namespace mappings (i.e. "jcr", "nt", "mix", "oak", etc.) should be retained if
         * identified for individual retention using {@link #withRetainPrefix(String)}
         * is generally NOT necessary.
         *
         * @param retainBuiltins true to retain builtin namespace mappings
         * @return this builder
         */
        public Builder withRetainBuiltins(final boolean retainBuiltins) {
            this.retainBuiltins = retainBuiltins;
            return this;
        }

        /**
         * Retain the namespace uri associated with the provided qualified name.
         *
         * @param name the qualified name
         * @return this builder
         */
        public Builder withQName(final @NotNull Name name) {
            if (!Name.NS_DEFAULT_URI.equals(name.getNamespaceURI())) {
                this.retainUris.add(name.getNamespaceURI());
            }
            return this;
        }

        /**
         * Retain the namespace prefix or uri associated with the provided JCR name. Depending on the qualification
         * syntax of this string, it will either include the URI ({@code "{" + uri + "}" + localName}), or the prefix
         * ({@code prefix + ":" + localName}).
         *
         * @param jcrName the JCR name
         * @return this builder
         */
        public Builder withJCRName(final @NotNull String jcrName) {
            final int closeBrace = jcrName.indexOf("}");
            if (closeBrace > 1 && jcrName.startsWith("{")) {
                final String uri = jcrName.substring(1, closeBrace);
                this.retainUris.add(uri);
            } else {
                JsonCnd.streamNsPrefix(jcrName).forEach(this.retainPrefixes::add);
            }
            return this;
        }

        /**
         * Build the {@link NamespaceMappingRequest}.
         *
         * @return the built request
         */
        public NamespaceMappingRequest build() {
            return new NamespaceMappingRequest(retainPrefixes, retainUris, retainBuiltins);
        }
    }

    /**
     * Resolve the retained prefixes and URIs from the provided {@link NamespaceMapping} object to produce a list of
     * {@link JcrNs} objects (wrapped in {@link Result} types) suitable for serialization to Json.
     *
     * @param mapping the JCR namespace mapping to resolve against
     * @return a list of {@link Result}-wrapped {@link JcrNs} types
     */
    public List<Result<JcrNs>> resolveToJcrNs(final @NotNull NamespaceMapping mapping) {
        final List<Result<JcrNs>> results = new ArrayList<>();

        retainPrefixes.stream()
                .map(result1(prefix -> JcrNs.create(prefix, mapping.getURI(prefix))))
                .forEach(results::add);

        final Set<String> retainedUris = results.stream().flatMap(Result::stream).map(JcrNs::getUri)
                .collect(Collectors.toSet());

        retainUris.stream()
                .filter(inSet(retainedUris).negate())
                .map(result1(uri -> JcrNs.create(mapping.getPrefix(uri), uri)))
                .forEach(results::add);

        return results;
    }
}
