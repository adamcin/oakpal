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

package net.adamcin.oakpal.api;

import java.util.EnumSet;

/**
 * Enumeration of progress tracker path import action types.
 */
public enum PathAction {
    NOOP("-"), MODIFIED("U"), REPLACED("R"),
    ERROR("E"), ADDED("A"), DELETED("D"),
    MISSING("!"), UNKNOWN("?");

    private final String shortCode;

    PathAction(final String shortCode) {
        this.shortCode = shortCode;
    }

    /**
     * Get the short code reported by filevault.
     *
     * @return the action type short code
     */
    public String getShortCode() {
        return shortCode;
    }

    /**
     * Returns true if this is the NOOP action type.
     *
     * @return true if noop
     */
    public boolean isNoop() {
        return this == NOOP;
    }

    /**
     * Returns true if this is the {@link #UNKNOWN} fallback action type. This type may be reported in the future if a
     * new filevault action code is introduced that isn't recognized by oakpal.
     *
     * @return true if unknown action type
     */
    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    /**
     * Returns true if it is safe to get the item at the referenced path based on this action type.
     *
     * @return true if safe to get item from session
     */
    public boolean canGetItem() {
        return EnumSet.of(NOOP, MODIFIED, REPLACED, ADDED).contains(this);
    }

    @Override
    public String toString() {
        return shortCode;
    }

    /**
     * Lookup an action type by the short code.
     *
     * @param shortCode the short code
     * @return the representative action type or {@link #UNKNOWN}
     */
    public static PathAction fromShortCode(final String shortCode) {
        for (PathAction value : values()) {
            if (value.getShortCode().equals(shortCode)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
