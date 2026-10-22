package com.ceu.pefima.backend.source.sqlite;

import java.nio.file.Path;

import org.sqlite.SQLiteDataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Builds a writable {@code notion_cleanup} v1.2 database filled with synthetic
 * values only. Never put real PeFiMa data here.
 */
final class SyntheticCleanupDatabase {

    static final String SNAPSHOT = "snapshot-0001";

    static final String EMPTY_SNAPSHOT = "snapshot-0002";

    static final String ROOT = "root-0001";

    static final String BUDGET = "000000000000000000000000000000b1";

    static final String EMERGENCY_TARGET = "000000000000000000000000000000f1";

    static final String WALLET = "000000000000000000000000000000a1";

    static final String EMERGENCY_WALLET = "000000000000000000000000000000a2";

    static final String FUND_COMPANY = "Synthetic Fund Co";

    static final String EXPENSE = "000000000000000000000000000000e1";

    static final String INCOME = "000000000000000000000000000000c1";

    static final String TRANSFER = "000000000000000000000000000000d1";

    static final String MARCH_BALANCE = "0000000000000000000000000000ba03";

    static final String YEAR_BALANCE = "0000000000000000000000000000ba00";

    private static final String NOW = "2026-01-05T00:00:00+00:00";

    private final JdbcClient jdbc;

    private int relationCount;

    private SyntheticCleanupDatabase(Path file) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + file.toAbsolutePath());
        new ResourceDatabasePopulator(new ClassPathResource("pefima-data/schema-v1.2.sql")).execute(dataSource);
        this.jdbc = JdbcClient.create(dataSource);
    }

    /** Creates the schema only, with no snapshots. */
    static SyntheticCleanupDatabase empty(Path file) {
        return new SyntheticCleanupDatabase(file);
    }

    /** Creates the schema plus one populated snapshot and one empty snapshot. */
    static SyntheticCleanupDatabase populated(Path file) {
        SyntheticCleanupDatabase database = new SyntheticCleanupDatabase(file);
        database.populate();
        return database;
    }

    JdbcClient jdbc() {
        return this.jdbc;
    }

    void setSchemaVersion(String version) {
        this.jdbc.sql("UPDATE schema_meta SET value = ? WHERE key = 'schema_version'").param(version).update();
    }

    private void populate() {
        this.jdbc.sql("INSERT INTO schema_meta(key, value) VALUES ('mapping_file_hash', 'mapping-hash-0001')").update();
        snapshot(EMPTY_SNAPSHOT, "2026-01-11");
        snapshot(SNAPSHOT, "2026-01-04");

        page(BUDGET, "Budget");
        page(EMERGENCY_TARGET, "Budget");
        field(BUDGET, "Budget", "Wallet", "Synthetic Envelope", null, null);
        field(BUDGET, "Budget", "Available Balance", "100.50", "100.50", null);
        field(BUDGET, "Budget", "Starting Balance", "10", "10", null);
        relation(BUDGET, "Budget", "EmerFund", EMERGENCY_TARGET);

        page(WALLET, "Wallet Balance");
        field(WALLET, "Wallet Balance", "Wallet", "Synthetic Wallet", null, null);
        field(WALLET, "Wallet Balance", "wallet_labels", "[\"Cash\"]", null, null);
        field(WALLET, "Wallet Balance", "wallet_active", "Yes", null, null);
        field(WALLET, "Wallet Balance", "Available Balance", "42.00", "42.00", null);
        field(WALLET, "Wallet Balance", "wallet_value", "42.00", "42.00", null);
        externalRelation(WALLET, "Wallet Balance", "etf_company", FUND_COMPANY);

        page(EMERGENCY_WALLET, "Wallet Balance");
        field(EMERGENCY_WALLET, "Wallet Balance", "Wallet", "Synthetic Bank", null, null);
        field(EMERGENCY_WALLET, "Wallet Balance", "Available Balance", "7.00", "7.00", null);
        // Like the real export, the allocation target is an untitled page outside the export.
        externalRelation(EMERGENCY_WALLET, "Wallet Balance", "emergency_fund_allocation_relation", null);

        page(EXPENSE, "Expenses");
        field(EXPENSE, "Expenses", "Name", "Synthetic lunch", null, null);
        field(EXPENSE, "Expenses", "Amount", "12.34", "12.34", null);
        field(EXPENSE, "Expenses", "Date", "January 3, 2026", null, "2026-01-03");
        field(EXPENSE, "Expenses", "Month", "January", null, null);
        relation(EXPENSE, "Expenses", "From", WALLET);

        page(INCOME, "Income");
        field(INCOME, "Income", "Name", "Synthetic salary", null, null);
        field(INCOME, "Income", "Amount", "1,000", "1000", null);
        field(INCOME, "Income", "Date", "January 2, 2026 9:30 AM", null, "2026-01-02T09:30:00");
        relation(INCOME, "Income", "To", WALLET);
        relation(INCOME, "Income", "income_default_budget_allocation", BUDGET);

        page(TRANSFER, "Internal Transactions");
        field(TRANSFER, "Internal Transactions", "Name", "Synthetic transfer", null, null);
        field(TRANSFER, "Internal Transactions", "Amount", "5", "5", null);
        relation(TRANSFER, "Internal Transactions", "From", WALLET);
        relation(TRANSFER, "Internal Transactions", "To", BUDGET);

        // Year row first: the source orders Balance rows arbitrarily.
        page(YEAR_BALANCE, "Balance");
        field(YEAR_BALANCE, "Balance", "Time", "Year", null, null);
        field(YEAR_BALANCE, "Balance", "Monthly Income", "1,000", "1000", null);
        field(YEAR_BALANCE, "Balance", "Monthly Expenses", "12.34", "12.34", null);
        field(YEAR_BALANCE, "Balance", "Monthly End Balance", "987.66", "987.66", null);
        field(YEAR_BALANCE, "Balance", "Year End Balance", "987.66", "987.66", null);
        page(MARCH_BALANCE, "Balance");
        field(MARCH_BALANCE, "Balance", "Time", "March", null, null);
        field(MARCH_BALANCE, "Balance", "Monthly Income", "1,000", "1000", null);
        field(MARCH_BALANCE, "Balance", "Monthly Expenses", "12.34", "12.34", null);
        field(MARCH_BALANCE, "Balance", "Monthly End Balance", "987.66", "987.66", null);

        this.jdbc.sql("""
                INSERT INTO conflicts(issue_key, snapshot_id, page_id, category, canonical_field, conflict_kind,
                    value_a, value_b, message, status)
                VALUES ('issue-conflict-1', ?, ?, 'Expenses', 'Amount', 'source_root_conflict',
                    'synthetic-a', 'synthetic-b', 'populated source observations disagree', 'open')
                """).params(SNAPSHOT, EXPENSE).update();
        this.jdbc.sql("""
                INSERT INTO unknown_fields(issue_key, snapshot_id, page_id, category, source_field, source_root_id,
                    source_view, source_file, first_seen, last_seen, status)
                VALUES ('issue-unknown-1', ?, NULL, 'Expenses', 'Synthetic Header', ?, 'all',
                    'Expenses synthetic_all.csv', ?, ?, 'open')
                """).params(SNAPSHOT, ROOT, NOW, NOW).update();
    }

    private void snapshot(String snapshotId, String exportDate) {
        this.jdbc.sql("""
                INSERT INTO export_snapshots(snapshot_id, export_date, year, source_manifest_hash, view,
                    source_format, source_root_count, created_at)
                VALUES (?, ?, 2026, 'manifest-hash', 'all', 'notion', 1, ?)
                """).params(snapshotId, exportDate, NOW).update();
    }

    private void page(String pageId, String category) {
        this.jdbc.sql("INSERT INTO pages(page_id, category, created_at, updated_at) VALUES (?, ?, ?, ?)")
            .params(pageId, category, NOW, NOW)
            .update();
    }

    private void field(String pageId, String category, String canonicalField, String rawText,
            String normalizedDecimal, String normalizedDate) {
        this.jdbc.sql("""
                INSERT INTO field_observations(snapshot_id, page_id, canonical_field, source_root_id, source_view,
                    category, source_field, raw_text, normalized_decimal, normalized_date, mapping_status,
                    resolution_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'all', ?, ?, ?, ?, ?, 'stable', 'resolved', ?, ?)
                """)
            .params(SNAPSHOT, pageId, canonicalField, ROOT, category, canonicalField, rawText, normalizedDecimal,
                    normalizedDate, NOW, NOW)
            .update();
    }

    private void relation(String sourcePageId, String category, String canonicalField, String targetPageId) {
        this.relationCount++;
        this.jdbc.sql("""
                INSERT INTO relations(relation_id, snapshot_id, source_page_id, source_root_id, source_view,
                    category, canonical_field, source_field, ordinal, target_page_id, resolution_status)
                VALUES (?, ?, ?, ?, 'all', ?, ?, ?, 0, ?, 'resolved')
                """)
            .params("relation-" + this.relationCount, SNAPSHOT, sourcePageId, ROOT, category, canonicalField,
                    canonicalField, targetPageId)
            .update();
    }

    /** A titled target outside the export, as the importer stores a "Name (link)" cell. */
    private void externalRelation(String sourcePageId, String category, String canonicalField, String title) {
        this.relationCount++;
        this.jdbc.sql("""
                INSERT INTO relations(relation_id, snapshot_id, source_page_id, source_root_id, source_view,
                    category, canonical_field, source_field, ordinal, target_title, external_locator, is_external,
                    resolution_status)
                VALUES (?, ?, ?, ?, 'all', ?, ?, ?, 0, ?, 'https://example.invalid/synthetic', 1, 'external')
                """)
            .params("relation-" + this.relationCount, SNAPSHOT, sourcePageId, ROOT, category, canonicalField,
                    canonicalField, title)
            .update();
    }

}
