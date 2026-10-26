PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS schema_meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS export_snapshots (
    snapshot_id TEXT PRIMARY KEY,
    export_date TEXT NOT NULL,
    year INTEGER NOT NULL,
    source_manifest_hash TEXT NOT NULL,
    view TEXT NOT NULL,
    raw_archive_path TEXT,
    source_format TEXT,
    source_root_count INTEGER NOT NULL DEFAULT 0,
    created_run_id TEXT,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS ingest_runs (
    run_id TEXT PRIMARY KEY,
    snapshot_id TEXT NOT NULL REFERENCES export_snapshots(snapshot_id),
    started_at TEXT NOT NULL,
    finished_at TEXT,
    status TEXT NOT NULL,
    input_path TEXT,
    raw_archive_path TEXT,
    files_processed INTEGER NOT NULL DEFAULT 0,
    rows_seen INTEGER NOT NULL DEFAULT 0,
    rows_added INTEGER NOT NULL DEFAULT 0,
    rows_updated INTEGER NOT NULL DEFAULT 0,
    rows_flagged INTEGER NOT NULL DEFAULT 0,
    rows_skipped INTEGER NOT NULL DEFAULT 0,
    conflicts INTEGER NOT NULL DEFAULT 0,
    unknown_fields INTEGER NOT NULL DEFAULT 0,
    unresolved_relations INTEGER NOT NULL DEFAULT 0,
    error_text TEXT
);

CREATE TABLE IF NOT EXISTS run_files (
    run_id TEXT NOT NULL REFERENCES ingest_runs(run_id),
    source_root_id TEXT NOT NULL,
    relative_path TEXT NOT NULL,
    file_role TEXT NOT NULL,
    category TEXT,
    view TEXT,
    source_format TEXT,
    byte_count INTEGER NOT NULL,
    sha256 TEXT NOT NULL,
    row_count INTEGER,
    PRIMARY KEY (run_id, source_root_id, relative_path)
);

CREATE TABLE IF NOT EXISTS pages (
    page_id TEXT PRIMARY KEY,
    category TEXT NOT NULL,
    current_title TEXT,
    first_snapshot_id TEXT REFERENCES export_snapshots(snapshot_id),
    last_snapshot_id TEXT REFERENCES export_snapshots(snapshot_id),
    first_seen_at TEXT,
    last_seen_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS page_observations (
    snapshot_id TEXT NOT NULL REFERENCES export_snapshots(snapshot_id),
    page_id TEXT NOT NULL REFERENCES pages(page_id),
    source_root_id TEXT NOT NULL,
    source_view TEXT NOT NULL,
    source_format TEXT NOT NULL,
    category TEXT NOT NULL,
    title TEXT,
    icon TEXT,
    source_relative_path TEXT,
    body_markdown TEXT,
    raw_properties_json TEXT,
    property_type_metadata_json TEXT,
    record_hash TEXT,
    scope_status TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    PRIMARY KEY (snapshot_id, page_id, source_root_id, source_view)
);

CREATE TABLE IF NOT EXISTS field_observations (
    snapshot_id TEXT NOT NULL REFERENCES export_snapshots(snapshot_id),
    page_id TEXT NOT NULL REFERENCES pages(page_id),
    canonical_field TEXT NOT NULL,
    source_root_id TEXT NOT NULL,
    source_view TEXT NOT NULL,
    category TEXT NOT NULL,
    source_field TEXT NOT NULL,
    raw_text TEXT,
    normalized_text TEXT,
    normalized_decimal TEXT,
    normalized_date TEXT,
    notion_type TEXT,
    mapping_status TEXT NOT NULL,
    resolution_status TEXT NOT NULL,
    source_file TEXT,
    row_number INTEGER,
    review_key TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    PRIMARY KEY (
        snapshot_id, page_id, canonical_field, source_root_id, source_view
    )
);

CREATE TABLE IF NOT EXISTS relations (
    relation_id TEXT PRIMARY KEY,
    snapshot_id TEXT NOT NULL REFERENCES export_snapshots(snapshot_id),
    source_page_id TEXT NOT NULL REFERENCES pages(page_id),
    source_root_id TEXT NOT NULL,
    source_view TEXT NOT NULL,
    category TEXT NOT NULL,
    canonical_field TEXT NOT NULL,
    source_field TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    raw_display TEXT,
    target_page_id TEXT REFERENCES pages(page_id),
    target_category TEXT,
    target_title TEXT,
    target_path TEXT,
    external_locator TEXT,
    is_external INTEGER NOT NULL DEFAULT 0,
    resolution_status TEXT NOT NULL,
    source_file TEXT,
    row_number INTEGER
);

CREATE TABLE IF NOT EXISTS computed_property_observations (
    observation_id TEXT PRIMARY KEY,
    snapshot_id TEXT NOT NULL REFERENCES export_snapshots(snapshot_id),
    page_id TEXT NOT NULL REFERENCES pages(page_id),
    source_root_id TEXT NOT NULL,
    source_view TEXT NOT NULL,
    category TEXT NOT NULL,
    canonical_field TEXT NOT NULL,
    source_field TEXT NOT NULL,
    notion_type TEXT NOT NULL,
    value_text TEXT,
    value_decimal TEXT,
    source_file TEXT,
    row_number INTEGER
);

CREATE TABLE IF NOT EXISTS unknown_fields (
    issue_key TEXT PRIMARY KEY,
    snapshot_id TEXT NOT NULL REFERENCES export_snapshots(snapshot_id),
    page_id TEXT,
    category TEXT NOT NULL,
    source_field TEXT NOT NULL,
    source_root_id TEXT NOT NULL,
    source_view TEXT NOT NULL,
    source_file TEXT NOT NULL,
    first_seen TEXT NOT NULL,
    last_seen TEXT NOT NULL,
    status TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS conflicts (
    issue_key TEXT PRIMARY KEY,
    snapshot_id TEXT NOT NULL REFERENCES export_snapshots(snapshot_id),
    page_id TEXT,
    category TEXT NOT NULL,
    canonical_field TEXT,
    conflict_kind TEXT NOT NULL,
    source_a_file TEXT,
    source_b_file TEXT,
    value_a TEXT,
    value_b TEXT,
    source_a_type TEXT,
    source_b_type TEXT,
    message TEXT NOT NULL,
    status TEXT NOT NULL,
    first_seen TEXT,
    last_seen TEXT,
    resolution_note TEXT
);

CREATE INDEX IF NOT EXISTS idx_page_observations_snapshot
    ON page_observations(snapshot_id, page_id);
CREATE INDEX IF NOT EXISTS idx_field_observations_snapshot
    ON field_observations(snapshot_id, page_id, canonical_field);
CREATE INDEX IF NOT EXISTS idx_relations_source
    ON relations(snapshot_id, source_page_id, canonical_field);
CREATE INDEX IF NOT EXISTS idx_conflicts_snapshot
    ON conflicts(snapshot_id, page_id, canonical_field);
CREATE INDEX IF NOT EXISTS idx_unknown_fields_snapshot
    ON unknown_fields(snapshot_id, page_id, category);

INSERT OR IGNORE INTO schema_meta(key, value) VALUES ('schema_version', '1.2');

-- 1.1 added monthly_balance_observations; 1.2 added etf_company to
-- wallet_account_observations. Older 1.0 and 1.1 databases are upgraded in
-- place by notion_cleanup.storage.initialize_database (views only).
DROP VIEW IF EXISTS monthly_balance_observations;
CREATE VIEW IF NOT EXISTS monthly_balance_observations AS
WITH fields AS (
    SELECT
        snapshot_id,
        page_id,
        MAX(source_root_id) AS source_root_id,
        MAX(CASE WHEN canonical_field = 'Time'
            THEN COALESCE(normalized_text, raw_text) END) AS period,
        MAX(CASE WHEN canonical_field = 'Monthly Income'
            THEN normalized_decimal END) AS income_decimal,
        MAX(CASE WHEN canonical_field = 'Monthly Expenses'
            THEN normalized_decimal END) AS expenses_decimal,
        MAX(CASE WHEN canonical_field = 'Monthly End Balance'
            THEN normalized_decimal END) AS net_decimal,
        MAX(CASE WHEN canonical_field = 'Year End Balance'
            THEN normalized_decimal END) AS year_end_decimal
    FROM field_observations
    WHERE resolution_status = 'resolved'
    GROUP BY snapshot_id, page_id
)
SELECT
    f.snapshot_id,
    s.export_date,
    s.year,
    f.page_id AS balance_id,
    f.source_root_id,
    f.period,
    CASE f.period
        WHEN 'January' THEN 1 WHEN 'February' THEN 2 WHEN 'March' THEN 3
        WHEN 'April' THEN 4 WHEN 'May' THEN 5 WHEN 'June' THEN 6
        WHEN 'July' THEN 7 WHEN 'August' THEN 8 WHEN 'September' THEN 9
        WHEN 'October' THEN 10 WHEN 'November' THEN 11 WHEN 'December' THEN 12
    END AS month_number,
    f.income_decimal,
    f.expenses_decimal,
    f.net_decimal,
    f.year_end_decimal
FROM fields f
JOIN pages p ON p.page_id = f.page_id AND p.category = 'Balance'
JOIN export_snapshots s ON s.snapshot_id = f.snapshot_id;

DROP VIEW IF EXISTS budget_envelope_observations;
CREATE VIEW IF NOT EXISTS budget_envelope_observations AS
WITH fields AS (
    SELECT
        snapshot_id,
        page_id,
        MAX(source_root_id) AS source_root_id,
        MAX(CASE WHEN canonical_field = 'Wallet'
            THEN COALESCE(normalized_text, raw_text) END) AS name,
        MAX(CASE WHEN canonical_field = 'Available Balance'
            THEN normalized_decimal END) AS available_balance_decimal,
        MAX(CASE WHEN canonical_field = 'Starting Balance'
            THEN normalized_decimal END) AS starting_balance_decimal
    FROM field_observations
    WHERE resolution_status = 'resolved'
    GROUP BY snapshot_id, page_id
),
emergency_relations AS (
    SELECT snapshot_id, source_page_id AS page_id,
           MAX(target_page_id) AS emergency_relation_id
    FROM relations
    WHERE canonical_field IN ('emergency_fund_relation', 'EmerFund')
      AND resolution_status IN ('resolved', 'resolved_configured')
    GROUP BY snapshot_id, source_page_id
)
SELECT
    f.snapshot_id,
    s.export_date,
    s.year,
    f.page_id AS budget_id,
    f.source_root_id,
    f.name,
    f.available_balance_decimal,
    f.starting_balance_decimal,
    r.emergency_relation_id,
    CAST(NULL AS TEXT) AS role,
    CAST(NULL AS TEXT) AS account_id
FROM fields f
JOIN pages p ON p.page_id = f.page_id AND p.category = 'Budget'
JOIN export_snapshots s ON s.snapshot_id = f.snapshot_id
LEFT JOIN emergency_relations r
    ON r.snapshot_id = f.snapshot_id AND r.page_id = f.page_id;

DROP VIEW IF EXISTS wallet_account_observations;
CREATE VIEW IF NOT EXISTS wallet_account_observations AS
WITH fields AS (
    SELECT
        snapshot_id,
        page_id,
        MAX(source_root_id) AS source_root_id,
        MAX(CASE WHEN canonical_field = 'Wallet'
            THEN COALESCE(normalized_text, raw_text) END) AS name,
        MAX(CASE WHEN canonical_field = 'wallet_labels'
            THEN COALESCE(normalized_text, raw_text) END) AS labels_json,
        MAX(CASE WHEN canonical_field = 'wallet_active'
            THEN COALESCE(normalized_text, raw_text) END) AS active_raw,
        MAX(CASE WHEN canonical_field = 'Available Balance'
            THEN normalized_decimal END) AS available_balance_decimal,
        MAX(CASE WHEN canonical_field = 'Starting Balance'
            THEN normalized_decimal END) AS starting_balance_decimal,
        MAX(CASE WHEN canonical_field = 'wallet_value'
            THEN normalized_decimal END) AS value_decimal
    FROM field_observations
    WHERE resolution_status = 'resolved'
    GROUP BY snapshot_id, page_id
),
-- The fund company of a Fund wallet: the title of its "ETF Company" relation
-- (a page outside the export), never the link itself. One per wallet so far.
etf_companies AS (
    SELECT snapshot_id, source_page_id AS page_id,
           MAX(target_title) AS etf_company
    FROM relations
    WHERE canonical_field = 'etf_company'
      AND target_title IS NOT NULL
    GROUP BY snapshot_id, source_page_id
)
SELECT
    f.snapshot_id,
    s.export_date,
    s.year,
    f.page_id AS account_id,
    f.source_root_id,
    f.name,
    f.labels_json,
    f.active_raw,
    f.available_balance_decimal,
    f.starting_balance_decimal,
    f.value_decimal,
    CAST(NULL AS TEXT) AS currency_code,
    CAST(NULL AS TEXT) AS timezone,
    c.etf_company
FROM fields f
JOIN pages p ON p.page_id = f.page_id AND p.category = 'Wallet Balance'
JOIN export_snapshots s ON s.snapshot_id = f.snapshot_id
LEFT JOIN etf_companies c
    ON c.snapshot_id = f.snapshot_id AND c.page_id = f.page_id;

DROP VIEW IF EXISTS transaction_observations;
CREATE VIEW IF NOT EXISTS transaction_observations AS
WITH fields AS (
    SELECT
        snapshot_id,
        page_id,
        category,
        MAX(source_root_id) AS source_root_id,
        MAX(CASE WHEN canonical_field = 'Name'
            THEN COALESCE(normalized_text, raw_text) END) AS name,
        MAX(CASE WHEN canonical_field = 'Amount'
            THEN normalized_decimal END) AS amount_decimal,
        MAX(CASE WHEN canonical_field = 'Date'
            THEN normalized_date END) AS occurred_at,
        MAX(CASE WHEN canonical_field = 'Date'
            THEN raw_text END) AS raw_date_text,
        MAX(CASE WHEN canonical_field = 'Month'
            THEN COALESCE(normalized_text, raw_text) END) AS month,
        MAX(CASE WHEN canonical_field = 'transaction_description'
            THEN COALESCE(normalized_text, raw_text) END) AS description,
        MAX(CASE WHEN canonical_field IN ('Notes', 'expense_notes_legacy')
            THEN COALESCE(normalized_text, raw_text) END) AS notes,
        MAX(CASE WHEN canonical_field = 'Condition'
            THEN COALESCE(normalized_text, raw_text) END) AS condition_raw,
        MAX(CASE WHEN canonical_field = 'Paid'
            THEN COALESCE(normalized_text, raw_text) END) AS paid_raw,
        MAX(CASE WHEN canonical_field = 'expense_spent_legacy'
            THEN COALESCE(normalized_text, raw_text) END) AS spent_raw,
        MAX(CASE WHEN canonical_field IN (
                'Differ', 'income_difference_legacy', 'expense_difference_legacy'
            )
            THEN normalized_decimal END) AS differ_decimal,
        MAX(CASE WHEN canonical_field = 'monitor_formula'
            THEN normalized_decimal END) AS monitor_decimal,
        MAX(CASE WHEN canonical_field = 'credit_hold_marker'
            THEN COALESCE(normalized_text, raw_text) END) AS credit_hold_raw,
        MAX(CASE WHEN canonical_field = 'overpay_marker'
            THEN COALESCE(normalized_text, raw_text) END) AS overpay_raw,
        MAX(CASE WHEN canonical_field = 'expense_tags'
            THEN COALESCE(normalized_text, raw_text) END) AS tags_json
    FROM field_observations
    WHERE resolution_status = 'resolved'
    GROUP BY snapshot_id, page_id, category
),
relation_values AS (
    SELECT
        snapshot_id,
        source_page_id AS page_id,
        MAX(CASE WHEN canonical_field IN ('From', 'from_relation')
            THEN target_page_id END) AS from_relation_id,
        MAX(CASE WHEN canonical_field IN ('To', 'to_relation')
            THEN target_page_id END) AS to_relation_id,
        MAX(CASE WHEN canonical_field IN (
                'To Budget', 'to_budget_relation', 'Type'
            )
            THEN target_page_id END) AS budget_relation_id,
        MAX(CASE WHEN canonical_field = 'income_default_budget_allocation'
            THEN target_page_id END) AS budget_id,
        MAX(CASE WHEN canonical_field = 'income_insert_budget_allocation'
            THEN target_page_id END) AS insert_budget_id
    FROM relations
    WHERE resolution_status IN ('resolved', 'resolved_configured')
    GROUP BY snapshot_id, source_page_id
)
SELECT
    f.snapshot_id,
    s.export_date,
    s.year,
    f.page_id AS transaction_id,
    CASE f.category
        WHEN 'Expenses' THEN 'expense'
        WHEN 'Income' THEN 'income'
        WHEN 'Internal Transactions' THEN 'internal_transfer'
        ELSE 'unknown'
    END AS kind,
    f.source_root_id,
    f.name,
    f.amount_decimal,
    f.occurred_at,
    f.raw_date_text,
    f.month,
    r.from_relation_id AS from_account_id,
    r.to_relation_id AS to_account_id,
    CAST(NULL AS TEXT) AS from_budget_id,
    CASE
        WHEN r.budget_id IS NOT NULL AND r.insert_budget_id IS NOT NULL
            THEN NULL
        ELSE COALESCE(r.budget_id, r.insert_budget_id, r.budget_relation_id)
    END AS to_budget_id,
    CASE
        WHEN r.budget_id IS NOT NULL AND r.insert_budget_id IS NOT NULL
            THEN NULL
        ELSE COALESCE(r.budget_id, r.insert_budget_id, r.budget_relation_id)
    END AS budget_id,
    r.budget_id AS default_budget_id,
    r.insert_budget_id,
    f.tags_json,
    f.description,
    f.notes,
    f.condition_raw,
    f.paid_raw,
    f.spent_raw,
    f.differ_decimal,
    f.monitor_decimal,
    f.credit_hold_raw,
    f.overpay_raw
FROM fields f
JOIN pages p ON p.page_id = f.page_id
JOIN export_snapshots s ON s.snapshot_id = f.snapshot_id
LEFT JOIN relation_values r
    ON r.snapshot_id = f.snapshot_id AND r.page_id = f.page_id
WHERE f.category IN ('Expenses', 'Income', 'Internal Transactions');
