package com.ceu.pefima.backend.source.model;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * One imported Notion export.
 *
 * @param view source view the values came from, such as {@code all}
 * @param sourceRootCount number of export roots compared in this snapshot
 */
public record ExportSnapshot(String snapshotId, LocalDate exportDate, int year, String view,
        @Nullable String sourceFormat, int sourceRootCount) {
}
