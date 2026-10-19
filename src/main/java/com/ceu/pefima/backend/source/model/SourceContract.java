package com.ceu.pefima.backend.source.model;

import org.jspecify.annotations.Nullable;

/**
 * The data contract a source was produced with.
 *
 * @param schemaVersion version of the source schema
 * @param mappingFileHash SHA-256 of the field-mapping file used by the latest import
 */
public record SourceContract(String schemaVersion, @Nullable String mappingFileHash) {
}
