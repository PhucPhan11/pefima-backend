package com.ceu.pefima.backend.source.model;

import org.jspecify.annotations.Nullable;

/**
 * A conflict or unknown field recorded during import. Conflicting source values
 * are deliberately not carried here.
 *
 * @param field canonical field for conflicts, raw source header for unknown fields
 * @param kind conflict kind such as {@code source_root_conflict}, or {@link #UNKNOWN_FIELD}
 * @param status review status, such as {@code open}
 */
public record ReviewIssue(String issueKey, String snapshotId, @Nullable String pageId, String category,
        @Nullable String field, String kind, String status, @Nullable String message) {

    /** {@link #kind()} for headers the field mapping does not recognize. */
    public static final String UNKNOWN_FIELD = "unknown_field";

    public boolean isOpen() {
        return "open".equals(this.status);
    }

}
