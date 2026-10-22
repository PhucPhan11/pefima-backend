package com.ceu.pefima.backend.source.sqlite;

import java.util.List;

import com.ceu.pefima.backend.source.WalletAccountSource;
import com.ceu.pefima.backend.source.model.WalletAccount;

final class SqliteWalletAccountSource implements WalletAccountSource {

    private final SqliteCleanupDatabase database;

    SqliteWalletAccountSource(SqliteCleanupDatabase database) {
        this.database = database;
    }

    @Override
    public List<WalletAccount> findBySnapshot(String snapshotId) {
        this.database.requireSnapshot(snapshotId);
        return this.database.jdbc()
            // The view has no column for the "🏹 EmerFund Allocation" relation, so read
            // whether it exists from the relations table; its target is never selected.
            .sql("""
                    SELECT w.*, EXISTS (
                        SELECT 1 FROM relations r
                        WHERE r.snapshot_id = w.snapshot_id
                          AND r.source_page_id = w.account_id
                          AND r.canonical_field = 'emergency_fund_allocation_relation'
                    ) AS emergency_fund_allocation
                    FROM wallet_account_observations w
                    WHERE w.snapshot_id = :id
                    ORDER BY w.account_id
                    """)
            .param("id", snapshotId)
            .query((rs, rowNum) -> new WalletAccount(rs.getString("account_id"), rs.getString("name"),
                    rs.getString("labels_json"), rs.getString("active_raw"),
                    SqliteRows.decimal(rs, "available_balance_decimal"),
                    SqliteRows.decimal(rs, "starting_balance_decimal"), SqliteRows.decimal(rs, "value_decimal"),
                    rs.getString("currency_code"), rs.getString("timezone"), rs.getString("etf_company"),
                    rs.getBoolean("emergency_fund_allocation"), SqliteRows.provenance(rs)))
            .list();
    }

}
