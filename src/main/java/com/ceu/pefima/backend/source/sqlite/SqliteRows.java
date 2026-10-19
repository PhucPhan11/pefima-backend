package com.ceu.pefima.backend.source.sqlite;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.jspecify.annotations.Nullable;

import com.ceu.pefima.backend.source.PefimaSourceException;
import com.ceu.pefima.backend.source.model.Provenance;

/**
 * Column readers shared by the SQLite adapters. Parse errors name the column
 * but never echo the source value.
 */
final class SqliteRows {

    /** Length of an ISO date-only value such as {@code 2026-08-31}. */
    private static final int ISO_DATE_LENGTH = 10;

    private SqliteRows() {
    }

    static @Nullable BigDecimal decimal(ResultSet rs, String column) throws SQLException {
        String text = rs.getString(column);
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        }
        catch (NumberFormatException ex) {
            throw new PefimaSourceException("Column " + column + " is not a fixed-point decimal", ex);
        }
    }

    static LocalDate date(ResultSet rs, String column) throws SQLException {
        String text = rs.getString(column);
        if (text == null) {
            throw new PefimaSourceException("Column " + column + " is unexpectedly null");
        }
        try {
            return LocalDate.parse(text);
        }
        catch (DateTimeParseException ex) {
            throw new PefimaSourceException("Column " + column + " is not an ISO date", ex);
        }
    }

    /**
     * Reads a column holding either an ISO date or an ISO date-time.
     * @return the date-time, or {@code null} for date-only and blank values
     */
    static @Nullable LocalDateTime dateTimeOrNull(ResultSet rs, String column) throws SQLException {
        String text = rs.getString(column);
        if (text == null || text.length() == ISO_DATE_LENGTH) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME);
        }
        catch (DateTimeParseException ex) {
            throw new PefimaSourceException("Column " + column + " is not an ISO date-time", ex);
        }
    }

    /** Reads the date part of a column holding either an ISO date or an ISO date-time. */
    static @Nullable LocalDate datePartOrNull(ResultSet rs, String column) throws SQLException {
        String text = rs.getString(column);
        if (text == null) {
            return null;
        }
        if (text.length() == ISO_DATE_LENGTH) {
            return date(rs, column);
        }
        LocalDateTime dateTime = dateTimeOrNull(rs, column);
        return (dateTime != null) ? dateTime.toLocalDate() : null;
    }

    /** Reads {@code snapshot_id}, {@code export_date}, {@code year}, and {@code source_root_id}. */
    static Provenance provenance(ResultSet rs) throws SQLException {
        return new Provenance(rs.getString("snapshot_id"), date(rs, "export_date"), rs.getInt("year"),
                rs.getString("source_root_id"));
    }

}
