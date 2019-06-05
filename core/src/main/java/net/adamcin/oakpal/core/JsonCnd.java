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

package net.adamcin.oakpal.core;

import static java.util.Optional.ofNullable;
import static net.adamcin.oakpal.core.Fun.composeTest;
import static net.adamcin.oakpal.core.Fun.infer1;
import static net.adamcin.oakpal.core.Fun.inferTest1;
import static net.adamcin.oakpal.core.Fun.mapKey;
import static net.adamcin.oakpal.core.Fun.mapValue;
import static net.adamcin.oakpal.core.Fun.onEntry;
import static net.adamcin.oakpal.core.Fun.testValue;
import static net.adamcin.oakpal.core.Fun.toEntry;
import static net.adamcin.oakpal.core.Fun.uncheck1;
import static net.adamcin.oakpal.core.Fun.uncheck2;
import static net.adamcin.oakpal.core.Fun.uncheckVoid1;
import static net.adamcin.oakpal.core.Fun.uncheckVoid2;
import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfStrings;
import static net.adamcin.oakpal.core.JavaxJson.unwrap;
import static net.adamcin.oakpal.core.JavaxJson.wrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonCollectors;

import org.apache.jackrabbit.commons.cnd.Lexer;
import org.apache.jackrabbit.commons.query.qom.Operator;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.apache.jackrabbit.spi.commons.nodetype.QItemDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QPropertyDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Methods and types used to encode/decode QNodeTypeDefinitions as JSON for use in checklists.
 *
 * @see DefinitionToken
 * @see NodeTypeDefinitionQAdapter
 */
public final class JsonCnd {
    private JsonCnd() {
        // no instantiation
    }

    /**
     * Read a serialized JSON CND into a list of qualified node type definitions.
     *
     * @param json    the jcrNodetypes object, organized by "typeName": { node type definition }
     * @param mapping the mapping to use to resolve JCR namespaces from prefixes, see {@link #toNamespaceMapping(List)}
     * @return a list of qualified node type definitions
     */
    public static List<QNodeTypeDefinition>
    getQTypesFromJson(@NotNull final JsonObject json,
                      @NotNull final NamespaceMapping mapping) {
        return JavaxJson.mapObjectValues(json, uncheck2(qDefinitionMapper(mapping)), true);
    }

    /**
     * Write a list of qualified node types to a JSON CND object.
     *
     * @param ntDefs  the list of qualified node type definitions to write
     * @param mapping a JCR namespace mapping to resolve prefixes for qualified names
     * @return a JSON CND object
     */
    public static JsonObject toJson(@NotNull final List<QNodeTypeDefinition> ntDefs,
                                    @NotNull final NamespaceMapping mapping) {
        final NamePathResolver resolver = new DefaultNamePathResolver(mapping);
        return ntDefs.stream()
                .map(def -> toEntry(def, NodeTypeDefinitionKey.writeAllJson(def, resolver)))
                .filter(testValue(JavaxJson::nonEmptyValue))
                .map(mapKey(uncheck1(jcrNameOrResidual(resolver)).compose(QNodeTypeDefinition::getName)))
                .sorted(Map.Entry.comparingByKey())
                .collect(JsonCollectors.toJsonObject());
    }

    /**
     * Function to adapt a Session-linked NodeTypeDefinition as a qualified node type definition for writing to JSON.
     *
     * @param session a JCR session to use for resolving JCR names and paths
     * @return a map function for stream transformation
     */
    public static Function<NodeTypeDefinition, QNodeTypeDefinition> adaptToQ(@NotNull final Session session) {
        final NamePathResolver resolver = new DefaultNamePathResolver(session);
        return nodeTypeDefinition -> new NodeTypeDefinitionQAdapter(nodeTypeDefinition, resolver);
    }

    /**
     * Stream the qualified names referenced in a node type definition in order to determine which namespaces must be
     * represented in a serialized mapping.
     *
     * @param def a qualified node type definition
     * @return a stream of qualified names
     */
    public static Stream<Name> namedBy(@NotNull final QNodeTypeDefinition def) {
        return Stream.concat(Stream.of(def.getName()), def.getDependencies().stream());
    }

    /**
     * Aggregate a list of Oakpal {@link JcrNs} prefix to uri mappings into a JCR {@link NamespaceMapping} object.
     *
     * @param jcrNsList the list of oakpal prefix to uri mappings
     * @return the aggregated NamespaceMapping object
     */
    public static NamespaceMapping toNamespaceMapping(final List<JcrNs> jcrNsList) {
        final NamespaceMapping nsMapping = new NamespaceMapping(QName.BUILTIN_MAPPINGS);
        jcrNsList.forEach(uncheckVoid1(
                jcrNs -> nsMapping.setMapping(jcrNs.getPrefix(), jcrNs.getUri())));
        return nsMapping;
    }

    /**
     * The "*" token represents a placeholder, known as a "residual", where you would otherwise expect a parseable JCR
     * name. NamePathResolvers don't like it. We have to handle it as a special case in our JSON CND serialization and
     * parsing logic to avoid parse exceptions.
     */
    static final String TOKEN_RESIDUAL = "*";

    /**
     * The Jackrabbit SPI CND reader resolves the residual token as a "*" in the default namespace. We follow this
     * convention as well.
     */
    static final Name QNAME_RESIDUAL = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, TOKEN_RESIDUAL);

    /**
     * Returns a Name to String mapping function that checks if the Name is the residual identifier before attempting to
     * resolve a JCR name for it.
     *
     * @param resolver the NamePathResolver that provides JCR name resolution
     * @return the Name to String mapping function
     */
    static Fun.ThrowingFunction<Name, String> jcrNameOrResidual(@NotNull final NamePathResolver resolver) {
        return qName -> {
            if (QNAME_RESIDUAL.equals(qName)) {
                return TOKEN_RESIDUAL;
            } else {
                return resolver.getJCRName(qName);
            }
        };
    }

    /**
     * Returns a String to Name mapping function that checks if the String is the residual token before attempting to
     * resolve a qualified name for it.
     *
     * @param resolver the NamePathResolver that provides QName resolution
     * @return the String to Name mapping function
     */
    static Fun.ThrowingFunction<String, Name> qNameOrResidual(@NotNull final NamePathResolver resolver) {
        return jcrName -> {
            if (TOKEN_RESIDUAL.equals(jcrName)) {
                return QNAME_RESIDUAL;
            } else {
                return resolver.getQName(jcrName);
            }
        };
    }

    /**
     * a String comparator with a check for residual tokens to push them to the end of a list.
     */
    private static final transient Comparator<String> COMPARATOR_PUSH_RESIDUALS;
    static {
        final Comparator<String> keyComparator = Comparator.comparing(String::toString);
        COMPARATOR_PUSH_RESIDUALS = (s1, s2) -> {
            if (TOKEN_RESIDUAL.equals(s2)) {
                return -1;
            } else if (TOKEN_RESIDUAL.equals(s1)) {
                return 1;
            } else {
                return keyComparator.compare(s1, s2);
            }
        };
    }

    /**
     * Returns a throwing bi-function that maps a JSON key and associated JSON Object value to a constructed node type definition.
     *
     * @param resolver a NamePathResolver, such as a DefaultNamePathResolver built around a NamespaceMapping
     * @return a throwing bi-function mapping a JSON key and object value to a constructed node type definition
     */
    static Fun.ThrowingBiFunction<String, JsonObject, QNodeTypeDefinition>
    nodeTypeDefinitionMapper(@NotNull final NamePathResolver resolver) {
        return (key, json) -> {
            final QNodeTypeDefinitionBuilder def = new QNodeTypeDefinitionBuilder();
            def.setName(qNameOrResidual(resolver).tryApply(key));
            NodeTypeDefinitionKey.readAllTo(resolver, def, json);
            return def.build();
        };
    }

    /**
     * Constructs a {@link QDefinitionBuilderFactory} using the provided NamespaceMapping and returns the appropriately
     * parameterized result of {@link #nodeTypeDefinitionMapper(NamePathResolver)}.
     *
     * @param mapping an aggregated NamespaceMapping
     * @return a throwing bi-function mapping a JSON key and object value to a constructed {@link QNodeTypeDefinition}
     */
    static Fun.ThrowingBiFunction<String, JsonObject, QNodeTypeDefinition>
    qDefinitionMapper(@NotNull final NamespaceMapping mapping) {
        return nodeTypeDefinitionMapper(new DefaultNamePathResolver(mapping));
    }

    /**
     * Maps a QValue to a proper string for serializing to the JSON CND.
     *
     * @param qValue   a {@link QValue}, such as from {@link QPropertyDefinition#getDefaultValues()}
     * @param resolver a {@link NamePathResolver} that can map namespaces to JCR name prefixes
     * @return the serializable string
     * @throws RepositoryException when {@link NamePathResolver} throws.
     */
    static String qValueString(@NotNull final QValue qValue,
                               @NotNull NamePathResolver resolver) throws RepositoryException {
        switch (qValue.getType()) {
            case PropertyType.NAME:
                return resolver.getJCRName(qValue.getName());
            case PropertyType.PATH:
                return resolver.getJCRPath(qValue.getPath());
            default:
                return qValue.getString();
        }
    }

    /**
     * Top interface defining CND tokens for both keys and attributes. Altogether there are 7 kinds of tokens:
     * NodeType 1) keys and 2) attributes
     * Item Definition 3) attributes (super kind of Property Definition and Child Node Definition)
     * Property Definition 4) keys and 5) attributes
     * Child Node Definition 6) keys and 7) attributes
     *
     * @see NodeTypeDefinitionKey
     * @see TypeDefinitionAttribute
     * @see ItemDefinitionAttribute
     * @see PropertyDefinitionKey
     * @see PropertyDefinitionAttribute
     * @see ChildNodeDefinitionKey
     * @see ChildNodeDefinitionAttribute
     */
    interface DefinitionToken {
        /**
         * Get the preferred token string as serialized in a JSON stream.
         *
         * @return the preferred token string
         */
        String getToken();

        /**
         * Each token enum may define an UNKNOWN("?") element, which is the default representation of an unidentified token
         * in a particular token context. This method is used to filter each enum's values() stream to exclude this
         * element from execution, since it is basically a noop placeholder.
         *
         * @return true if not the UNKNOWN element
         */
        default boolean nonUnknown() {
            return !"?".equals(getToken());
        }
    }

    /**
     * Attribute token enums implement this interface to read and write tokens based on presence and boolean values.
     *
     * @param <B> arbitrary builder type parameter
     * @param <D> arbitrary definition type parameter
     */
    interface AttributeDefinitionToken<B, D> extends DefinitionToken {
        /**
         * Checks for whether the token should be included in an attributes array in the JSON stream for the context of
         * the definition.
         *
         * @param def an arbitrary definition
         * @return true if the attribute applies to the definition and should be included in the "@" array.
         */
        boolean isWritable(D def);

        /**
         * Consume the attribute token to mutate the provided builder.
         *
         * @param builder a builder of arbitrary type
         */
        void readTo(B builder);
    }

    /**
     * Key token enums implement this interface to read and write tokens and associated values.
     *
     * @param <B> arbitrary builder type parameter
     * @param <D> arbitrary definition type parameter
     */
    interface KeyDefinitionToken<B, D> extends DefinitionToken {
        /**
         * Construct a JSON value appropriate for this key, based on the provided definition.
         *
         * @param def      an arbitrary definition
         * @param resolver the NamePathResolver to use for resolving namespaces from prefixes embedded in values
         * @return a JsonValue to write as part of this token-value pair, or null/empty if this key should not be written
         * for this definition
         */
        JsonValue writeJson(D def, NamePathResolver resolver);

        /**
         * Consume the key token and provided JSON value to mutate the provided builder.
         *
         * @param resolver the NamePathResolver to use for resolving namespaces from prefixes embedded in values
         * @param builder  a builder of arbitrary type
         * @param value    the value associated with this key in the JSON stream currently being read
         */
        void readTo(NamePathResolver resolver, B builder, JsonValue value);
    }

    /**
     * Common function method for all {@link KeyDefinitionToken} enum's static readAllTo methods. The only things that
     * differ between them are the type parameters.
     *
     * @param resolver    a NamePathResolver
     * @param builder     the arbitrary builder
     * @param parentValue the JsonObject value associated with the parent object in the JSON stream currently being read,
     *                    of which all the tokens are assumed to be children keys
     * @param tokens      the key tokens, provided by the enum's {@code values()} method
     * @param <B>         arbitrary builder type parameter
     * @param <D>         arbitrary definition type parameter
     * @param <K>         the {@link KeyDefinitionToken} enum type, appropriately parameterized to {@link B} and {@link D}
     */
    private static <B, D, K extends KeyDefinitionToken<B, D>> void
    internalReadAllTo(@NotNull final NamePathResolver resolver,
                      @NotNull final B builder,
                      @NotNull final JsonValue parentValue,
                      @NotNull final K[] tokens) {
        if (parentValue.getValueType() == JsonValue.ValueType.OBJECT) {
            JsonObject json = parentValue.asJsonObject();
            Stream.of(tokens)
                    .filter(composeTest(DefinitionToken::getToken, json::containsKey).and(DefinitionToken::nonUnknown))
                    .map(key -> toEntry(key, json.get(key.getToken())))
                    .filter(testValue(JavaxJson::nonEmptyValue))
                    .forEachOrdered(onEntry((key, keyValue) -> key.readTo(resolver, builder, keyValue)));
        }
    }

    /**
     * Common function method for all {@link KeyDefinitionToken} enum's static writeAllJson methods. The only things that
     * differ between them are the type parameters.
     *
     * @param def      the arbitrary definition
     * @param resolver the {@link NamePathResolver} used to resolve prefixes for namespaces in qualified names and values.
     * @param tokens   the key tokens, provided by the enum's {@code values()} method
     * @param <D>      arbitrary definition type parameter
     * @param <K>      the {@link KeyDefinitionToken} enum type, appropriately parameterized to {@link D}. The builder parameter
     *                 is ignored.
     * @return a JsonObject to write as an aggregrate of all the key tokens supported for this definition
     */
    private static <D, K extends KeyDefinitionToken<?, D>> JsonValue
    internalWriteAllJson(@NotNull final D def,
                         @NotNull final NamePathResolver resolver,
                         @NotNull final K[] tokens) {
        return Stream.of(tokens)
                .filter(DefinitionToken::nonUnknown)
                .map(key -> toEntry(key.getToken(), key.writeJson(def, resolver)))
                .filter(testValue(JavaxJson::nonEmptyValue))
                .collect(JsonCollectors.toJsonObject());
    }

    /**
     * This is needed to easily map untyped {@link javax.json.JsonString} instances to {@link String}, since we don't use
     * the {@link JsonObject#getString(String)} methods.
     */
    static final Function<JsonValue, String> JSON_VALUE_STRING = infer1(Object::toString).compose(JavaxJson::unwrap);

    /**
     * The DefinitionToken enum representing a serialized {@link NodeTypeDefinition}'s properties that are associated with
     * values. Boolean properties of the definition are represented by {@link TypeDefinitionAttribute} values.
     */
    enum NodeTypeDefinitionKey implements KeyDefinitionToken<QNodeTypeDefinitionBuilder, QNodeTypeDefinition> {
        /**
         * Supertypes.
         */
        EXTENDS("" + Lexer.EXTENDS,
                // write json
                (def, resolver) -> ofNullable(def.getSupertypes()).map(Stream::of).orElse(Stream.empty())
                        .map(uncheck1(jcrNameOrResidual(resolver)))
                        .map(JavaxJson::wrap)
                        .sorted(Comparator.comparing(JSON_VALUE_STRING))
                        .collect(JsonCollectors.toJsonArray()),
                // read definition
                resolver -> (def, value) -> def.setSupertypes(mapArrayOfStrings(value.asJsonArray()).stream()
                        .map(uncheck1(qNameOrResidual(resolver))).toArray(Name[]::new))),
        /**
         * Boolean attributes.
         *
         * @see TypeDefinitionAttribute#readAttributes(QNodeTypeDefinitionBuilder, JsonValue)
         * @see TypeDefinitionAttribute#getAttributeTokens(QNodeTypeDefinition)
         */
        ATTRIBUTES("@",
                // write json
                (def, resolver) -> wrap(TypeDefinitionAttribute.getAttributeTokens(def)),
                // read definition
                resolver -> TypeDefinitionAttribute::readAttributes),
        /**
         * Primary Item Name, i.e. nt:file's "jcr:content".
         */
        PRIMARYITEM(Lexer.PRIMARYITEM[0],
                // write json
                (def, resolver) -> ofNullable(def.getPrimaryItemName())
                        .map(infer1(JavaxJson::wrap).compose(uncheck1(jcrNameOrResidual(resolver))))
                        .orElse(null),
                // read definition
                resolver -> uncheckVoid2((def, value) ->
                        def.setPrimaryItemName(
                                uncheck1(qNameOrResidual(resolver)).apply(unwrap(value).toString())))),
        /**
         * Property definitions.
         *
         * @see PropertyDefinitionKey
         */
        PROPERTY_DEFINITION("" + Lexer.PROPERTY_DEFINITION,
                // write json
                (def, resolver) -> ofNullable(def.getPropertyDefs()).map(Stream::of).orElse(Stream.empty())
                        .map(pDef -> toEntry(pDef, PropertyDefinitionKey.writeAllJson(pDef, resolver)))
                        .filter(testValue(JavaxJson::nonEmptyValue))
                        .map(mapKey(uncheck1(jcrNameOrResidual(resolver)).compose(QItemDefinition::getName)))
                        .sorted(Map.Entry.comparingByKey(COMPARATOR_PUSH_RESIDUALS))
                        .collect(JsonCollectors.toJsonObject()),
                // read definition
                resolver -> (def, value) -> def.setPropertyDefs(value.asJsonObject().entrySet().stream()
                        .filter(testValue(JavaxJson::nonEmptyValue))
                        .map(mapValue((name, pValue) -> {
                            QPropertyDefinitionBuilder pDef = new QPropertyDefinitionBuilder();
                            pDef.setName(uncheck1(qNameOrResidual(resolver)).apply(name));
                            pDef.setDeclaringNodeType(def.getName());
                            PropertyDefinitionKey.readAllTo(resolver, pDef, pValue);
                            return pDef.build();
                        })).map(Map.Entry::getValue).toArray(QPropertyDefinition[]::new))
        ),
        /**
         * Child Node Definitions.
         *
         * @see ChildNodeDefinitionKey
         */
        CHILD_NODE_DEFINITION("" + Lexer.CHILD_NODE_DEFINITION,
                // write json
                (def, resolver) -> ofNullable(def.getChildNodeDefs()).map(Stream::of).orElse(Stream.empty())
                        .map(nDef -> toEntry(nDef, ChildNodeDefinitionKey.writeAllJson(nDef, resolver)))
                        .filter(testValue(JavaxJson::nonEmptyValue))
                        .map(mapKey(uncheck1(jcrNameOrResidual(resolver)).compose(QItemDefinition::getName)))
                        .sorted(Map.Entry.comparingByKey(COMPARATOR_PUSH_RESIDUALS))
                        .collect(JsonCollectors.toJsonObject()),
                // read definition
                resolver -> (def, value) -> def.setChildNodeDefs(value.asJsonObject().entrySet().stream()
                        .filter(testValue(JavaxJson::nonEmptyValue))
                        .map(mapValue((name, pValue) -> {
                            QNodeDefinitionBuilder nDef = new QNodeDefinitionBuilder();
                            nDef.setName(uncheck1(qNameOrResidual(resolver)).apply(name));
                            nDef.setDeclaringNodeType(def.getName());
                            ChildNodeDefinitionKey.readAllTo(resolver, nDef, pValue);
                            return nDef.build();
                        })).map(Map.Entry::getValue).toArray(QNodeDefinition[]::new)));

        /**
         * Read all of this type's key tokens from the parent json object to mutate the provided builder.
         *
         * @param resolver a NamePathResolver to use for resolving prefixes for namespaces in qualified names and paths
         * @param builder  the node type definition builder
         * @param value    the untyped JsonObject parent (will be typed by
         *                 {@link #internalReadAllTo(NamePathResolver, Object, JsonValue, KeyDefinitionToken[])})
         * @see #nodeTypeDefinitionMapper(NamePathResolver)
         */
        static void readAllTo(@NotNull final NamePathResolver resolver,
                              @NotNull final QNodeTypeDefinitionBuilder builder,
                              @NotNull final JsonValue value) {
            internalReadAllTo(resolver, builder, value, values());
        }

        /**
         * Write all of this type's key tokens to a new JSON object value.
         *
         * @param def      the node type definition
         * @param resolver a NamePathResolver to use for resolving prefixes for namespaces in qualified names and paths
         * @return an untyped JsonObject parent of all of this type's key tokens
         * @see #toJson(List, NamespaceMapping)
         */
        static JsonValue writeAllJson(@NotNull final QNodeTypeDefinition def,
                                      @NotNull final NamePathResolver resolver) {
            return internalWriteAllJson(def, resolver, values());
        }

        final String token;
        final BiFunction<QNodeTypeDefinition, NamePathResolver, JsonValue> writeJsonValue;
        final Function<NamePathResolver, BiConsumer<QNodeTypeDefinitionBuilder, JsonValue>> readToBuilder;

        NodeTypeDefinitionKey(final String token,
                              final BiFunction<QNodeTypeDefinition, NamePathResolver, JsonValue> writeJsonValue,
                              final Function<NamePathResolver, BiConsumer<QNodeTypeDefinitionBuilder, JsonValue>> readToBuilder) {
            this.token = token;
            this.writeJsonValue = writeJsonValue;
            this.readToBuilder = readToBuilder;
        }

        @Override
        public void readTo(final NamePathResolver resolver,
                           final QNodeTypeDefinitionBuilder builder,
                           final JsonValue value) {
            this.readToBuilder.apply(resolver).accept(builder, value);
        }

        @Override
        public JsonValue writeJson(final QNodeTypeDefinition def, final NamePathResolver resolver) {
            return writeJsonValue.apply(def, resolver);
        }

        @Override
        public String getToken() {
            return token;
        }
    }

    /**
     * The DefinitionToken enum representing a serialized {@link NodeTypeDefinition}'s attributes, which are associated
     * only with boolean values.
     *
     * @see NodeTypeDefinitionKey#ATTRIBUTES
     */
    enum TypeDefinitionAttribute
            implements AttributeDefinitionToken<QNodeTypeDefinitionBuilder, QNodeTypeDefinition> {
        /**
         * isAbstract(true)
         */
        ABSTRACT(Lexer.ABSTRACT[0],
                // write json
                QNodeTypeDefinition::isAbstract,
                // read definition
                uncheckVoid1(def -> def.setAbstract(true))),
        /**
         * isMixin(true)
         */
        MIXIN(Lexer.MIXIN[0],
                // write json
                QNodeTypeDefinition::isMixin,
                // read definition
                uncheckVoid1(def -> def.setMixin(true))),
        /**
         * hasOrderableChildNodes(true)
         */
        ORDERABLE(Lexer.ORDERABLE[0],
                // write json
                QNodeTypeDefinition::hasOrderableChildNodes,
                // read definition
                uncheckVoid1(def -> def.setOrderableChildNodes(true))),
        /**
         * isQueryable(false)
         */
        NOQUERY(Lexer.NOQUERY[0],
                // write json
                inferTest1(QNodeTypeDefinition::isQueryable).negate(),
                // read definition
                uncheckVoid1(def -> def.setQueryable(false))),
        /**
         * @see DefinitionToken#nonUnknown()
         */
        UNKNOWN("?",
                // write json
                def -> false,
                // read definition
                def -> {
                });

        /**
         * Read an "@"-key array value as a list of boolean attributes for the provided node type definition builder.
         *
         * @param builder         the node type definition builder
         * @param attributesValue the untyped JsonArray of attribute tokens
         * @see NodeTypeDefinitionKey#ATTRIBUTES
         */
        static void readAttributes(@NotNull final QNodeTypeDefinitionBuilder builder,
                                   @NotNull final JsonValue attributesValue) {
            if (attributesValue.getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArray attributes = attributesValue.asJsonArray();
                attributes.stream()
                        .map(infer1(TypeDefinitionAttribute::forToken).compose(JSON_VALUE_STRING))
                        .forEachOrdered(attr -> attr.readTo(builder));
            }
        }

        /**
         * Return a list of applicable attribute tokens that can be written to the "@" array of this definition's JSON object.
         *
         * @param def the node type definition to check attributes against
         * @return a list of writable attribute tokens
         */
        static List<String> getAttributeTokens(@NotNull final QNodeTypeDefinition def) {
            return Stream.of(values())
                    .filter(value -> value.isWritable(def))
                    .map(DefinitionToken::getToken)
                    .collect(Collectors.toList());
        }

        /**
         * Method for mapping String tokens to {@link TypeDefinitionAttribute} values when reading a "@" JsonArray.
         *
         * @param token a String representation of a token
         * @return the matching enum value or {@link #UNKNOWN}
         */
        static TypeDefinitionAttribute forToken(@NotNull final String token) {
            for (TypeDefinitionAttribute value : values()) {
                if (value.token.equalsIgnoreCase(token)) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        final String token;
        final Predicate<QNodeTypeDefinition> checkWritable;
        final Consumer<QNodeTypeDefinitionBuilder> readToBuilder;

        TypeDefinitionAttribute(final String token,
                                final Predicate<QNodeTypeDefinition> checkWritable,
                                final Consumer<QNodeTypeDefinitionBuilder> readToBuilder) {
            this.token = token;
            this.checkWritable = checkWritable;
            this.readToBuilder = readToBuilder;
        }

        @Override
        public String getToken() {
            return token;
        }

        @Override
        public boolean isWritable(@NotNull final QNodeTypeDefinition def) {
            return checkWritable.test(def);
        }

        @Override
        public void readTo(@NotNull final QNodeTypeDefinitionBuilder builder) {
            readToBuilder.accept(builder);
        }
    }

    /**
     * The DefinitionToken enum representing a serialized {@link ItemDefinition}'s attributes, which are representable
     * as boolean values. These attributes are common to both {@link PropertyDefinition} and {@link NodeDefinition}.
     *
     * @see PropertyDefinitionAttribute
     * @see ChildNodeDefinitionAttribute
     */
    enum ItemDefinitionAttribute
            implements AttributeDefinitionToken<QItemDefinitionBuilder, QItemDefinition> {
        /**
         * {@link QItemDefinition#isMandatory()}
         */
        MANDATORY(Lexer.MANDATORY[0],
                // write json
                QItemDefinition::isMandatory,
                // read definition
                uncheckVoid1(def -> def.setMandatory(true))),
        /**
         * {@link QItemDefinition#isAutoCreated()}
         */
        AUTOCREATED(Lexer.AUTOCREATED[0],
                // write json
                QItemDefinition::isAutoCreated,
                // read definition
                uncheckVoid1(def -> def.setAutoCreated(true))),
        /**
         * {@link QItemDefinition#isProtected()}
         */
        PROTECTED(Lexer.PROTECTED[0],
                // write json
                QItemDefinition::isProtected,
                // read definition
                uncheckVoid1(def -> def.setProtected(true))),
        /**
         * {@link OnParentVersionAction#COPY}. this is the default value.
         *
         * @see QItemDefinition#getOnParentVersion()
         */
        COPY(Lexer.COPY[0].toLowerCase(),
                // write json (don't write the default value)
                def -> false,
                // read definition
                uncheckVoid1(def -> def.setOnParentVersion(OnParentVersionAction.COPY))),
        /**
         * {@link OnParentVersionAction#VERSION}.
         *
         * @see QItemDefinition#getOnParentVersion()
         */
        VERSION(Lexer.VERSION[0].toLowerCase(),
                // write json
                def -> def.getOnParentVersion() == OnParentVersionAction.VERSION,
                // read definition
                uncheckVoid1(def -> def.setOnParentVersion(OnParentVersionAction.VERSION))),
        /**
         * {@link OnParentVersionAction#INITIALIZE}.
         *
         * @see QItemDefinition#getOnParentVersion()
         */
        INITIALIZE(Lexer.INITIALIZE[0].toLowerCase(),
                // write json
                def -> def.getOnParentVersion() == OnParentVersionAction.INITIALIZE,
                // read definition
                uncheckVoid1(def -> def.setOnParentVersion(OnParentVersionAction.INITIALIZE))),
        /**
         * {@link OnParentVersionAction#COMPUTE}.
         *
         * @see QItemDefinition#getOnParentVersion()
         */
        COMPUTE(Lexer.COMPUTE[0].toLowerCase(),
                // write json
                def -> def.getOnParentVersion() == OnParentVersionAction.COMPUTE,
                // read definition
                uncheckVoid1(def -> def.setOnParentVersion(OnParentVersionAction.COMPUTE))),
        /**
         * {@link OnParentVersionAction#IGNORE}.
         *
         * @see QItemDefinition#getOnParentVersion()
         */
        IGNORE(Lexer.IGNORE[0].toLowerCase(),
                // write json
                def -> def.getOnParentVersion() == OnParentVersionAction.IGNORE,
                // read definition
                uncheckVoid1(def -> def.setOnParentVersion(OnParentVersionAction.IGNORE))),
        /**
         * {@link OnParentVersionAction#ABORT}.
         *
         * @see QItemDefinition#getOnParentVersion()
         */
        ABORT(Lexer.ABORT[0].toLowerCase(),
                // write json
                def -> def.getOnParentVersion() == OnParentVersionAction.ABORT,
                // read definition
                uncheckVoid1(def -> def.setOnParentVersion(OnParentVersionAction.ABORT))),
        UNKNOWN("?",
                // write json
                def -> false,
                // read definition
                def -> {
                });

        /**
         * Return a list of base item definition attribute tokens to include in an "@" array, appropriate for the
         * provided item definition.
         *
         * @param def the item definition
         * @return a list of base item definition attribute tokens
         */
        static List<String> getAttributeTokens(@NotNull final QItemDefinition def) {
            return Stream.of(values())
                    .filter(value -> value.isWritable(def))
                    .map(DefinitionToken::getToken)
                    .collect(Collectors.toList());
        }

        /**
         * Internal method used by {@link PropertyDefinitionAttribute} and {@link ChildNodeDefinitionAttribute} to
         * aggregate attribute tokens for super and subtype in the same list.
         *
         * @param def           the item definition (property or child node)
         * @param subtypeTokens token enum values() provided by the subtype
         * @param <D>           the item definition type parameter
         * @param <T>           the subtype definition token type parameter
         * @return a list of base item definition attribute tokens followed by subtype item definition attribute tokens
         */
        static <D extends QItemDefinition, T extends AttributeDefinitionToken<?, D>> List<String>
        getMoreItemDefAttributeTokens(@NotNull final D def, @NotNull T[] subtypeTokens) {
            final List<String> attrs = new ArrayList<>(ItemDefinitionAttribute.getAttributeTokens(def));
            Stream.of(subtypeTokens).filter(value -> value.isWritable(def)).map(T::getToken).forEachOrdered(attrs::add);
            return attrs;
        }

        /**
         * Read base item definition attributes from JSON to mutate the provided builder. Should only be called indirectly,
         * by {@link #readAttributes(QItemDefinitionBuilder, JsonValue, Function)}.
         *
         * @param builder    an item definition builder
         * @param attributes a JsonArray of attributes
         */
        private static void readAttributes(@NotNull final QItemDefinitionBuilder builder,
                                           @NotNull final JsonArray attributes) {
            attributes.stream().map(infer1(ItemDefinitionAttribute::forToken).compose(JSON_VALUE_STRING))
                    .filter(DefinitionToken::nonUnknown).forEachOrdered(attr -> attr.readTo(builder));
        }

        /**
         * Read item definition attributes from JSON to mutate the provided builder. Should only be called indirectly, by
         * {@link PropertyDefinitionAttribute#readAttributes(QPropertyDefinitionBuilder, JsonValue)} and
         * {@link ChildNodeDefinitionAttribute#readAttributes(QNodeDefinitionBuilder, JsonValue)}.
         *
         * @param builder         the item definition builder
         * @param attributesValue the untyped attributes array value
         * @param tokenMapper     a function to map attribute tokens to enum elements
         * @param <B>             the builder type parameter
         * @param <T>             item definition subtype token type parameter
         */
        static <B extends QItemDefinitionBuilder, T extends AttributeDefinitionToken<B, ?>> void
        readAttributes(@NotNull final B builder,
                       @NotNull final JsonValue attributesValue,
                       @NotNull final Function<String, T> tokenMapper) {
            if (attributesValue.getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArray attributes = attributesValue.asJsonArray();
                ItemDefinitionAttribute.readAttributes(builder, attributes);
                attributes.stream().map(tokenMapper.compose(JSON_VALUE_STRING))
                        .filter(DefinitionToken::nonUnknown).forEachOrdered(attr -> attr.readTo(builder));
            }
        }

        /**
         * Method for mapping String tokens to {@link ItemDefinitionAttribute} values when reading a "@" JsonArray.
         *
         * @param token a String representation of a token
         * @return the matching enum value or {@link #UNKNOWN}
         */
        static ItemDefinitionAttribute forToken(@NotNull final String token) {
            for (ItemDefinitionAttribute value : values()) {
                if (value.token.equalsIgnoreCase(token)) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        final String token;
        final Predicate<QItemDefinition> checkWritable;
        final Consumer<QItemDefinitionBuilder> readToBuilder;

        ItemDefinitionAttribute(final String token,
                                final Predicate<QItemDefinition> checkWritable,
                                final Consumer<QItemDefinitionBuilder> readToBuilder) {
            this.token = token;
            this.checkWritable = checkWritable;
            this.readToBuilder = readToBuilder;
        }

        @Override
        public boolean isWritable(@NotNull final QItemDefinition def) {
            return checkWritable.test(def);
        }

        @Override
        public String getToken() {
            return token;
        }

        @Override
        public void readTo(@NotNull final QItemDefinitionBuilder builder) {
            readToBuilder.accept(builder);
        }
    }

    /**
     * The DefinitionToken enum representing a serialized {@link PropertyDefinition}'s properties that are associated with
     * values. Boolean properties of the definition are represented by {@link PropertyDefinitionAttribute} values.
     *
     * @see NodeTypeDefinitionKey#PROPERTY_DEFINITION
     */
    enum PropertyDefinitionKey implements KeyDefinitionToken<QPropertyDefinitionBuilder, QPropertyDefinition> {
        /**
         * Required Property Type.
         *
         * @see PropertyType
         */
        REQUIREDTYPE("type",
                // write json
                (def, resolver) -> wrap(PropertyType.nameFromValue(def.getRequiredType())),
                // read definition
                resolver -> uncheckVoid2((def, value) ->
                        def.setRequiredType(PropertyType.valueFromName(JSON_VALUE_STRING.apply(value))))),
        /**
         * Property Definition Attributes.
         *
         * @see PropertyDefinitionAttribute
         */
        ATTRIBUTES("@",
                // write json
                (def, resolver) -> JavaxJson.wrap(PropertyDefinitionAttribute.getAttributeTokens(def)),
                // read definition
                resolver -> PropertyDefinitionAttribute::readAttributes),
        /**
         * Available Query Operators.
         *
         * @see Operator#getAllQueryOperators()
         */
        QUERYOPS(Lexer.QUERYOPS[0],
                // write json
                (def, resolver) -> ofNullable(def.getAvailableQueryOperators())
                        .filter(ops -> ops.length != Operator.getAllQueryOperators().length).map(JavaxJson::wrap)
                        .orElse(null),
                // read definition
                resolver -> uncheckVoid2((def, value) -> def.setAvailableQueryOperators(value.asJsonArray().stream()
                        .map(JSON_VALUE_STRING).toArray(String[]::new)))),
        /**
         * Property Default Values.
         *
         * @see PropertyDefinition#getDefaultValues()
         */
        DEFAULT("" + Lexer.DEFAULT,
                // write json
                (def, resolver) -> ofNullable(def.getDefaultValues()).map(Stream::of).orElse(Stream.empty())
                        .map(uncheck1((value) -> qValueString(value, resolver)))
                        .map(JavaxJson::wrap).collect(JsonCollectors.toJsonArray()),
                // read definition
                resolver -> (def, value) -> def.setDefaultValues(value.asJsonArray().stream()
                        .map(JSON_VALUE_STRING).map(uncheck1(jcrValue -> ValueFormat
                                .getQValue(jcrValue, def.getRequiredType(), resolver, QValueFactoryImpl.getInstance())))
                        .toArray(QValue[]::new))),
        /**
         * Property Value Constraints
         *
         * @see PropertyDefinition#getValueConstraints()
         */
        CONSTRAINTS("" + Lexer.CONSTRAINT,
                // write json
                (def, resolver) ->
                        ofNullable(def.getValueConstraints()).map(Stream::of).orElse(Stream.empty())
                                .map(QValueConstraint::getString)
                                .map(JavaxJson::wrap)
                                .collect(JsonCollectors.toJsonArray()),
                // read definition
                resolver -> (def, value) -> def.setValueConstraints(value.asJsonArray().stream()
                        .map(JSON_VALUE_STRING).map(uncheck1(jcrValue -> ValueConstraint
                                .create(def.getRequiredType(), jcrValue, resolver)))
                        .toArray(QValueConstraint[]::new)));

        /**
         * Read all PropertyDefinitionKey values from the provided JsonValue, representing the parent JsonObject. Calls
         * {@link #internalReadAllTo(NamePathResolver, Object, JsonValue, KeyDefinitionToken[])}.
         *
         * @param resolver the NamePathResolver to use for mapping namespaces to JCR name prefixes
         * @param builder  the property definition builder
         * @param value    the untyped parent JsonObject
         */
        static void readAllTo(@NotNull final NamePathResolver resolver,
                              @NotNull final QPropertyDefinitionBuilder builder,
                              @NotNull final JsonValue value) {
            internalReadAllTo(resolver, builder, value, values());
        }

        /**
         * Write all PropertyDefinitionKey values to a new JsonObject. Calls
         * {@link #internalWriteAllJson(Object, NamePathResolver, KeyDefinitionToken[])}.
         *
         * @param def      the property definition
         * @param resolver the NamePathResolver to use for mapping namespaces to JCR name prefixes
         * @return the new parent JsonObject representing the property definition
         */
        static JsonValue writeAllJson(@NotNull final QPropertyDefinition def,
                                      @NotNull final NamePathResolver resolver) {
            return internalWriteAllJson(def, resolver, values());
        }

        final String token;
        final BiFunction<QPropertyDefinition, NamePathResolver, JsonValue> writeJsonValue;
        final Function<NamePathResolver, BiConsumer<QPropertyDefinitionBuilder, JsonValue>> readToBuilder;

        PropertyDefinitionKey(@NotNull final String token,
                              @NotNull final BiFunction<QPropertyDefinition, NamePathResolver, JsonValue> writeJsonValue,
                              @NotNull final Function<NamePathResolver, BiConsumer<QPropertyDefinitionBuilder, JsonValue>> readToBuilder) {
            this.token = token;
            this.writeJsonValue = writeJsonValue;
            this.readToBuilder = readToBuilder;
        }

        @Override
        public String getToken() {
            return token;
        }

        @Override
        public void readTo(@NotNull final NamePathResolver resolver,
                           @NotNull final QPropertyDefinitionBuilder builder,
                           @NotNull final JsonValue value) {
            this.readToBuilder.apply(resolver).accept(builder, value);
        }

        @Override
        public JsonValue writeJson(@NotNull final QPropertyDefinition def,
                                   @NotNull final NamePathResolver resolver) {
            return writeJsonValue.apply(def, resolver);
        }
    }

    /**
     * The DefinitionToken enum representing a serialized {@link PropertyDefinition}'s subtype-specific attributes, which
     * are representable as boolean values. These attributes extend the set of common attributes defined by
     * {@link ItemDefinitionAttribute}.
     *
     * @see ItemDefinitionAttribute
     * @see PropertyDefinitionKey#ATTRIBUTES
     */
    enum PropertyDefinitionAttribute
            implements AttributeDefinitionToken<QPropertyDefinitionBuilder, QPropertyDefinition> {
        /**
         * Multivalued property.
         *
         * @see PropertyDefinition#isMultiple()
         */
        MULTIPLE(Lexer.MULTIPLE[0],
                // write json
                QPropertyDefinition::isMultiple,
                // read definition
                uncheckVoid1(def -> def.setMultiple(true))),
        /**
         * Not full text searchable.
         *
         * @see PropertyDefinition#isFullTextSearchable()
         */
        NOFULLTEXT(Lexer.NOFULLTEXT[0],
                // write json
                inferTest1(QPropertyDefinition::isFullTextSearchable).negate(),
                // read definition
                uncheckVoid1(def -> def.setFullTextSearchable(false))),
        /**
         * Not query orderable.
         *
         * @see PropertyDefinition#isQueryOrderable()
         */
        NOQUERYORDER(Lexer.NOQUERYORDER[0],
                // write json
                inferTest1(QPropertyDefinition::isQueryOrderable).negate(),
                // read definition
                uncheckVoid1(def -> def.setQueryOrderable(false))),
        /**
         * Unknown attribute token.
         *
         * @see DefinitionToken#nonUnknown()
         */
        UNKNOWN("?",
                // write json
                def -> false,
                // read definition
                def -> {
                });

        /**
         * Read item and property definition attributes from the untyped attributes json array to mutate the builder.
         *
         * @param builder         the property definition builder
         * @param attributesValue the untyped json array of attribute tokens
         * @see ItemDefinitionAttribute#readAttributes(QItemDefinitionBuilder, JsonValue, Function)
         */
        static void readAttributes(@NotNull final QPropertyDefinitionBuilder builder,
                                   @NotNull final JsonValue attributesValue) {
            ItemDefinitionAttribute.readAttributes(builder, attributesValue, PropertyDefinitionAttribute::forToken);
        }

        /**
         * Write an array of property definition attribute tokens for serialization to the "@" array of the property
         * definition JSON object.
         *
         * @param def the property definition
         * @return the list of property definition attribute tokens
         */
        static List<String> getAttributeTokens(@NotNull final QPropertyDefinition def) {
            return ItemDefinitionAttribute.getMoreItemDefAttributeTokens(def, values());
        }

        /**
         * Method for mapping String tokens to {@link PropertyDefinitionAttribute} values when reading a "@" JsonArray.
         *
         * @param token a String representation of a token
         * @return the matching enum value or {@link #UNKNOWN}
         */
        static PropertyDefinitionAttribute forToken(@NotNull final String token) {
            for (PropertyDefinitionAttribute value : values()) {
                if (value.token.equalsIgnoreCase(token)) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        final String token;
        final Predicate<QPropertyDefinition> checkWritable;
        final Consumer<QPropertyDefinitionBuilder> readToBuilder;

        PropertyDefinitionAttribute(final String token,
                                    final Predicate<QPropertyDefinition> checkWritable,
                                    final Consumer<QPropertyDefinitionBuilder> readToBuilder) {
            this.token = token;
            this.checkWritable = checkWritable;
            this.readToBuilder = readToBuilder;
        }

        @Override
        public boolean isWritable(@NotNull final QPropertyDefinition def) {
            return checkWritable.test(def);
        }

        @Override
        public void readTo(@NotNull final QPropertyDefinitionBuilder builder) {
            readToBuilder.accept(builder);
        }

        @Override
        public String getToken() {
            return token;
        }
    }

    /**
     * The DefinitionToken enum representing a serialized {@link NodeDefinition}'s properties that are associated with
     * values. Boolean properties of the definition are represented by {@link ChildNodeDefinitionAttribute} values.
     *
     * @see NodeTypeDefinitionKey#CHILD_NODE_DEFINITION
     */
    enum ChildNodeDefinitionKey
            implements KeyDefinitionToken<QNodeDefinitionBuilder, QNodeDefinition> {
        /**
         * Required Primary Types.
         *
         * @see NodeDefinition#getRequiredPrimaryTypeNames()
         */
        REQUIREDTYPES("types",
                // write json
                (def, resolver) -> ofNullable(def.getRequiredPrimaryTypes()).map(Stream::of).orElse(Stream.empty())
                        .map(infer1(JavaxJson::wrap).compose(uncheck1(jcrNameOrResidual(resolver))))
                        .sorted(Comparator.comparing(JSON_VALUE_STRING))
                        .collect(JsonCollectors.toJsonArray()),
                // read definition
                resolver -> (def, value) -> mapArrayOfStrings(value.asJsonArray(), uncheck1(qNameOrResidual(resolver)))
                        .forEach(def::addRequiredPrimaryType)),
        /**
         * Default Primary Type.
         *
         * @see NodeDefinition#getDefaultPrimaryTypeName()
         */
        DEFAULTTYPE("" + Lexer.DEFAULT,
                // write json
                (def, resolver) -> ofNullable(def.getDefaultPrimaryType())
                        .map(name -> wrap(uncheck1(jcrNameOrResidual(resolver)).apply(name))).orElse(null),
                // read definition
                resolver -> (def, value) -> ofNullable(unwrap(value).toString())
                        .map(uncheck1(qNameOrResidual(resolver))).ifPresent(def::setDefaultPrimaryType)),
        /**
         * Child NodeDefinition Attributes.
         *
         * @see ChildNodeDefinitionAttribute
         */
        ATTRIBUTES("@",
                // write json
                (def, resolver) -> wrap(ChildNodeDefinitionAttribute.getAttributeTokens(def)),
                // read definition
                resolver -> ChildNodeDefinitionAttribute::readAttributes);

        /**
         * Read all ChildNodeDefinitionKey values from the provided JsonValue, representing the parent JsonObject. Calls
         * {@link #internalReadAllTo(NamePathResolver, Object, JsonValue, KeyDefinitionToken[])}.
         *
         * @param resolver the NamePathResolver to use for mapping namespaces to JCR name prefixes
         * @param builder  the property definition builder
         * @param value    the untyped parent JsonObject
         */
        static void readAllTo(@NotNull final NamePathResolver resolver,
                              @NotNull final QNodeDefinitionBuilder builder,
                              @NotNull final JsonValue value) {
            internalReadAllTo(resolver, builder, value, values());
        }

        /**
         * Write all ChildNodeDefinitionKey values to a new JsonObject. Calls
         * {@link #internalWriteAllJson(Object, NamePathResolver, KeyDefinitionToken[])}.
         *
         * @param def      the node definition
         * @param resolver the NamePathResolver to use for mapping namespaces to JCR name prefixes
         * @return the new parent JsonObject representing the node definition
         */
        static JsonValue writeAllJson(@NotNull final QNodeDefinition def,
                                      @NotNull final NamePathResolver resolver) {
            return internalWriteAllJson(def, resolver, values());
        }

        final String token;
        final BiFunction<QNodeDefinition, NamePathResolver, JsonValue> writeJsonValue;
        final Function<NamePathResolver, BiConsumer<QNodeDefinitionBuilder, JsonValue>> readToBuilder;

        ChildNodeDefinitionKey(final String token,
                               final BiFunction<QNodeDefinition, NamePathResolver, JsonValue> writeJsonValue,
                               final Function<NamePathResolver, BiConsumer<QNodeDefinitionBuilder, JsonValue>> readToBuilder) {
            this.token = token;
            this.writeJsonValue = writeJsonValue;
            this.readToBuilder = readToBuilder;
        }

        @Override
        public void readTo(@NotNull final NamePathResolver resolver,
                           @NotNull final QNodeDefinitionBuilder builder,
                           @NotNull final JsonValue value) {
            readToBuilder.apply(resolver).accept(builder, value);
        }

        @Override
        public JsonValue writeJson(@NotNull final QNodeDefinition def, @NotNull final NamePathResolver resolver) {
            return writeJsonValue.apply(def, resolver);
        }

        @Override
        public String getToken() {
            return token;
        }
    }

    /**
     * The DefinitionToken enum representing a serialized {@link NodeDefinition}'s subtype-specific attributes, which
     * are representable as boolean values. These attributes extend the set of common attributes defined by
     * {@link ItemDefinitionAttribute}.
     *
     * @see ItemDefinitionAttribute
     * @see ChildNodeDefinitionKey#ATTRIBUTES
     */
    enum ChildNodeDefinitionAttribute
            implements AttributeDefinitionToken<QNodeDefinitionBuilder, QNodeDefinition> {
        /**
         * Allows Same-Name Siblings.
         *
         * @see NodeDefinition#allowsSameNameSiblings()
         */
        SNS(Lexer.SNS[0],
                // write json
                QNodeDefinition::allowsSameNameSiblings,
                // read definition
                uncheckVoid1(def -> def.setAllowsSameNameSiblings(true))),
        /**
         * Unknown attribute token.
         *
         * @see DefinitionToken#nonUnknown()
         */
        UNKNOWN("?",
                // write json
                def -> false,
                // read definition
                def -> {
                });

        /**
         * Read item and child node definition attributes from the untyped attributes json array to mutate the builder.
         *
         * @param builder         the node definition builder
         * @param attributesValue the untyped json array of attribute tokens
         * @see ItemDefinitionAttribute#readAttributes(QItemDefinitionBuilder, JsonValue, Function)
         */
        static void readAttributes(@NotNull final QNodeDefinitionBuilder builder,
                                   @NotNull final JsonValue attributesValue) {
            ItemDefinitionAttribute.readAttributes(builder, attributesValue, ChildNodeDefinitionAttribute::forToken);
        }

        /**
         * Write an array of node definition attribute tokens for serialization to the "@" array of the property
         * definition JSON object.
         *
         * @param def the child node definition
         * @return the list of node definition attribute tokens
         */
        static List<String> getAttributeTokens(@NotNull final QNodeDefinition def) {
            return ItemDefinitionAttribute.getMoreItemDefAttributeTokens(def, values());
        }

        /**
         * Method for mapping String tokens to {@link ChildNodeDefinitionAttribute} values when reading a "@" JsonArray.
         *
         * @param token a String representation of a token
         * @return the matching enum value or {@link #UNKNOWN}
         */
        static ChildNodeDefinitionAttribute forToken(@NotNull final String token) {
            for (ChildNodeDefinitionAttribute value : values()) {
                if (value.token.equalsIgnoreCase(token)) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        final String token;
        final Predicate<QNodeDefinition> checkWritable;
        final Consumer<QNodeDefinitionBuilder> readToBuilder;

        ChildNodeDefinitionAttribute(final String token,
                                     final Predicate<QNodeDefinition> checkWritable,
                                     final Consumer<QNodeDefinitionBuilder> readToBuilder) {
            this.token = token;
            this.checkWritable = checkWritable;
            this.readToBuilder = readToBuilder;
        }

        @Override
        public boolean isWritable(final QNodeDefinition def) {
            return checkWritable.test(def);
        }

        @Override
        public void readTo(final QNodeDefinitionBuilder builder) {
            readToBuilder.accept(builder);
        }

        @Override
        public String getToken() {
            return token;
        }
    }

    /**
     * Adapts a {@link NodeTypeDefinition} as a {@link QNodeTypeDefinition}.
     *
     * @see #adaptToQ(Session)
     * @see PropertyDefinitionQAdapter
     * @see NodeDefinitionQAdapter
     */
    static class NodeTypeDefinitionQAdapter implements QNodeTypeDefinition {
        final NodeTypeDefinition wrapped;
        final NamePathResolver resolver;
        final List<PropertyDefinitionQAdapter> propDefs = new ArrayList<>();
        final List<NodeDefinitionQAdapter> childDefs = new ArrayList<>();
        transient volatile Collection<Name> dependencies = null;

        NodeTypeDefinitionQAdapter(@NotNull final NodeTypeDefinition wrapped,
                                   @NotNull final NamePathResolver resolver) {
            this.wrapped = wrapped;
            this.resolver = resolver;
            if (wrapped.getDeclaredPropertyDefinitions() != null) {
                Stream.of(wrapped.getDeclaredPropertyDefinitions())
                        .map(propDef -> new PropertyDefinitionQAdapter(propDef, resolver))
                        .forEachOrdered(propDefs::add);
            }
            if (wrapped.getDeclaredChildNodeDefinitions() != null) {
                Stream.of(wrapped.getDeclaredChildNodeDefinitions())
                        .map(childDef -> new NodeDefinitionQAdapter(childDef, resolver))
                        .forEachOrdered(childDefs::add);
            }
        }

        @Override
        public Name getName() {
            try {
                return this.resolver.getQName(this.wrapped.getName());
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Name[] getSupertypes() {
            if (wrapped.getDeclaredSupertypeNames() != null) {
                return Stream.of(wrapped.getDeclaredSupertypeNames())
                        .map(uncheck1(qNameOrResidual(resolver))).toArray(Name[]::new);
            }
            return new Name[0];
        }

        @Override
        public Name[] getSupportedMixinTypes() {
            return null;
        }

        @Override
        public boolean isMixin() {
            return wrapped.isMixin();
        }

        @Override
        public boolean isAbstract() {
            return wrapped.isAbstract();
        }

        @Override
        public boolean isQueryable() {
            return wrapped.isQueryable();
        }

        @Override
        public boolean hasOrderableChildNodes() {
            return wrapped.hasOrderableChildNodes();
        }

        @Override
        public Name getPrimaryItemName() {
            if (wrapped.getPrimaryItemName() != null) {
                try {
                    return resolver.getQName(wrapped.getPrimaryItemName());
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }

        @Override
        public QPropertyDefinition[] getPropertyDefs() {
            return propDefs.toArray(new QPropertyDefinition[0]);
        }

        @Override
        public QNodeDefinition[] getChildNodeDefs() {
            return childDefs.toArray(new QNodeDefinition[0]);
        }

        /**
         * This is mostly lifted from {@link QNodeTypeDefinitionImpl#getDependencies()}
         *
         * @return a collection of named dependencies
         * @see #namedBy(QNodeTypeDefinition)
         */
        @Override
        public Collection<Name> getDependencies() {
            if (dependencies == null) {
                // supertypes
                Collection<Name> deps = new HashSet<>(Arrays.asList(getSupertypes()));
                // child node definitions
                for (QNodeDefinition childNodeDef : getChildNodeDefs()) {
                    // default primary type
                    Name ntName = childNodeDef.getDefaultPrimaryType();
                    if (ntName != null && !getName().equals(ntName)) {
                        deps.add(ntName);
                    }
                    // required primary type
                    Name[] ntNames = childNodeDef.getRequiredPrimaryTypes();
                    for (Name ntName1 : ntNames) {
                        if (ntName1 != null && !getName().equals(ntName1)) {
                            deps.add(ntName1);
                        }
                    }
                }
                // property definitions
                for (QPropertyDefinition propertyDef : getPropertyDefs()) {
                    // [WEAK]REFERENCE value constraints
                    if (propertyDef.getRequiredType() == PropertyType.REFERENCE
                            || propertyDef.getRequiredType() == PropertyType.WEAKREFERENCE) {
                        QValueConstraint[] ca = propertyDef.getValueConstraints();
                        if (ca != null) {
                            for (QValueConstraint aCa : ca) {
                                NameFactory factory = NameFactoryImpl.getInstance();
                                Name ntName = factory.create(aCa.getString());
                                if (!getName().equals(ntName)) {
                                    deps.add(ntName);
                                }
                            }
                        }
                    }
                }
                dependencies = Collections.unmodifiableCollection(deps);
            }
            return dependencies;
        }
    }

    /**
     * Abstract base adapter of {@link ItemDefinition}s as subtypes of {@link QItemDefinition}.
     *
     * @param <T> the ItemDefinition subtype parameter
     * @see PropertyDefinitionQAdapter
     * @see NodeDefinitionQAdapter
     */
    static abstract class ItemDefinitionQAdaptor<T extends ItemDefinition> implements QItemDefinition {
        protected final T wrapped;
        protected final NamePathResolver resolver;

        ItemDefinitionQAdaptor(@NotNull final T wrapped,
                               @NotNull final NamePathResolver resolver) {
            this.wrapped = wrapped;
            this.resolver = resolver;
        }

        @Override
        public Name getName() {
            try {
                final String jcrName = wrapped.getName();
                if (TOKEN_RESIDUAL.equals(jcrName)) {
                    return QNAME_RESIDUAL;
                } else {
                    return resolver.getQName(jcrName);
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Name getDeclaringNodeType() {
            if (wrapped.getDeclaringNodeType() != null) {
                try {
                    return resolver.getQName(wrapped.getDeclaringNodeType().getName());
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }

        @Override
        public boolean isAutoCreated() {
            return wrapped.isAutoCreated();
        }

        @Override
        public int getOnParentVersion() {
            return wrapped.getOnParentVersion();
        }

        @Override
        public boolean isProtected() {
            return wrapped.isProtected();
        }

        @Override
        public boolean isMandatory() {
            return wrapped.isMandatory();
        }

        @Override
        public boolean definesResidual() {
            final Name name = getName();
            return QNAME_RESIDUAL.equals(name);
        }
    }

    /**
     * Adapts a {@link PropertyDefinition} as a {@link QPropertyDefinition}.
     *
     * @see NodeTypeDefinitionQAdapter#getPropertyDefs()
     * @see ItemDefinitionQAdaptor
     */
    static class PropertyDefinitionQAdapter extends ItemDefinitionQAdaptor<PropertyDefinition> implements QPropertyDefinition {
        PropertyDefinitionQAdapter(final @NotNull PropertyDefinition wrapped,
                                   final @NotNull NamePathResolver resolver) {
            super(wrapped, resolver);
        }

        @Override
        public int getRequiredType() {
            return wrapped.getRequiredType();
        }

        @Override
        public QValueConstraint[] getValueConstraints() {
            if (wrapped.getValueConstraints() != null) {
                try {
                    ValueConstraint.create(getRequiredType(), wrapped.getValueConstraints(), resolver);
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }

        @Override
        public QValue[] getDefaultValues() {
            if (wrapped.getDefaultValues() != null) {
                try {
                    return ValueFormat.getQValues(wrapped.getDefaultValues(), resolver, QValueFactoryImpl.getInstance());
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }

        @Override
        public boolean definesNode() {
            return false;
        }

        @Override
        public boolean isMultiple() {
            return wrapped.isMultiple();
        }

        @Override
        public String[] getAvailableQueryOperators() {
            return wrapped.getAvailableQueryOperators();
        }

        @Override
        public boolean isFullTextSearchable() {
            return wrapped.isFullTextSearchable();
        }

        @Override
        public boolean isQueryOrderable() {
            return wrapped.isQueryOrderable();
        }
    }

    /**
     * Adapts a {@link NodeDefinition} as a {@link QNodeDefinition}.
     *
     * @see NodeTypeDefinitionQAdapter#getChildNodeDefs()
     * @see ItemDefinitionQAdaptor
     */
    static class NodeDefinitionQAdapter extends ItemDefinitionQAdaptor<NodeDefinition> implements QNodeDefinition {
        NodeDefinitionQAdapter(final @NotNull NodeDefinition wrapped,
                               final @NotNull NamePathResolver resolver) {
            super(wrapped, resolver);
        }

        @Override
        public Name getDefaultPrimaryType() {
            if (wrapped.getDefaultPrimaryTypeName() != null) {
                try {
                    return resolver.getQName(wrapped.getDefaultPrimaryTypeName());
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }

        @Override
        public Name[] getRequiredPrimaryTypes() {
            if (wrapped.getRequiredPrimaryTypeNames() != null) {
                return Stream.of(wrapped.getRequiredPrimaryTypeNames())
                        .map(uncheck1(qNameOrResidual(resolver)))
                        .toArray(Name[]::new);
            }
            return null;
        }

        @Override
        public boolean allowsSameNameSiblings() {
            return wrapped.allowsSameNameSiblings();
        }

        @Override
        public boolean definesNode() {
            return true;
        }
    }
}
