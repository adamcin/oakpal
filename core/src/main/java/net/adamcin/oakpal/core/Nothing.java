package net.adamcin.oakpal.core;

/**
 * A sentinel type for functional parameters representing Nothing, like Void.
 */
public final class Nothing {
    /**
     * This is the singleton sentinel value.
     */
    public static final Nothing instance = new Nothing();

    private Nothing() {
        /* prevent instantiation */
    }

    public Nothing combine(final Nothing nothing) {
        return this;
    }
}
