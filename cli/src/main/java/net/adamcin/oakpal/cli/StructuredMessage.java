package net.adamcin.oakpal.cli;

import net.adamcin.oakpal.core.JavaxJson;

/**
 * Extends {@link net.adamcin.oakpal.core.JavaxJson.ObjectConvertible} to ensure conformance with --output json. When
 * rendered with --output text,
 * {@link #toString()} will be called.
 */
public interface StructuredMessage extends JavaxJson.ObjectConvertible {
}
