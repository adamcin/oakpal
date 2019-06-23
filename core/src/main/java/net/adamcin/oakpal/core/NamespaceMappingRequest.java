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

    public static final class Builder {
        private final Set<String> retainPrefixes = new HashSet<>();
        private final Set<String> retainUris = new HashSet<>();
        private boolean retainBuiltins;

        public Builder withRetainPrefix(final @NotNull String prefix) {
            this.retainPrefixes.add(prefix);
            return this;
        }

        public Builder withRetainUri(final @NotNull String uri) {
            this.retainUris.add(uri);
            return this;
        }

        public Builder withRetainBuiltins(final boolean retainBuiltins) {
            this.retainBuiltins = retainBuiltins;
            return this;
        }

        public Builder withQName(final @NotNull Name name) {
            if (!Name.NS_DEFAULT_URI.equals(name.getNamespaceURI())) {
                this.retainUris.add(name.getNamespaceURI());
            }
            return this;
        }

        public Builder withJCRName(final @NotNull String jcrName) {
            final int closeBrace = jcrName.indexOf("}");
            if (closeBrace > 1 && jcrName.startsWith("{")) {
                final String uri = jcrName.substring(1, closeBrace - 1);
                this.retainUris.add(uri);
            } else {
                JsonCnd.streamNsPrefix(jcrName).forEach(this.retainPrefixes::add);
            }
            return this;
        }

        public NamespaceMappingRequest build() {
            return new NamespaceMappingRequest(retainPrefixes, retainUris, retainBuiltins);
        }
    }

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
