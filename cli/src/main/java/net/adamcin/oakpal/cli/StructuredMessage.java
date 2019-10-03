package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.api.JsonObjectConvertible;

/**
 * Extends {@link JsonObjectConvertible} to ensure conformance with --output json. When
 * rendered with --output text,
 * {@code toString()} will be called.
 */
public interface StructuredMessage extends JsonObjectConvertible {
}
