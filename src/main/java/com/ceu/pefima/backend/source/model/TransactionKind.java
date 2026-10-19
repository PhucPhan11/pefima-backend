package com.ceu.pefima.backend.source.model;

import org.jspecify.annotations.Nullable;

/**
 * Kind of a {@link Transaction}, derived from its source category.
 */
public enum TransactionKind {

    EXPENSE("expense"),

    INCOME("income"),

    INTERNAL_TRANSFER("internal_transfer"),

    UNKNOWN("unknown");

    private final String sourceValue;

    TransactionKind(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    /** The value the cleanup contract uses for this kind. */
    public String sourceValue() {
        return this.sourceValue;
    }

    /** Maps a contract value to a kind; anything unrecognized is {@link #UNKNOWN}. */
    public static TransactionKind fromSourceValue(@Nullable String value) {
        for (TransactionKind kind : values()) {
            if (kind.sourceValue.equals(value)) {
                return kind;
            }
        }
        return UNKNOWN;
    }

}
