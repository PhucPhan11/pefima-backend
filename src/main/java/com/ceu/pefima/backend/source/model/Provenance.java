package com.ceu.pefima.backend.source.model;

import java.time.LocalDate;

/**
 * Where a record was observed: which snapshot and which export source root.
 */
public record Provenance(String snapshotId, LocalDate exportDate, int year, String sourceRootId) {
}
