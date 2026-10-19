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
            .sql("""
                    SELECT * FROM wallet_account_observations
                    WHERE snapshot_id = :id
                    ORDER BY account_id
                    """)
            .param("id", snapshotId)
            .query((rs, rowNum) -> new WalletAccount(rs.getString("account_id"), rs.getString("name"),
                    rs.getString("labels_json"), rs.getString("active_raw"),
                    SqliteRows.decimal(rs, "available_balance_decimal"),
                    SqliteRows.decimal(rs, "starting_balance_decimal"), SqliteRows.decimal(rs, "value_decimal"),
                    rs.getString("currency_code"), rs.getString("timezone"), SqliteRows.provenance(rs)))
            .list();
    }

}
