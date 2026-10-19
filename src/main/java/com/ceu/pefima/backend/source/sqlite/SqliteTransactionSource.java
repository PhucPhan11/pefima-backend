package com.ceu.pefima.backend.source.sqlite;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.ceu.pefima.backend.source.TransactionSource;
import com.ceu.pefima.backend.source.model.Transaction;
import com.ceu.pefima.backend.source.model.TransactionKind;

final class SqliteTransactionSource implements TransactionSource {

    private final SqliteCleanupDatabase database;

    SqliteTransactionSource(SqliteCleanupDatabase database) {
        this.database = database;
    }

    @Override
    public List<Transaction> findBySnapshot(String snapshotId) {
        this.database.requireSnapshot(snapshotId);
        return this.database.jdbc()
            .sql("""
                    SELECT * FROM transaction_observations
                    WHERE snapshot_id = :id
                    ORDER BY occurred_at, transaction_id
                    """)
            .param("id", snapshotId)
            .query((rs, rowNum) -> transaction(rs))
            .list();
    }

    private static Transaction transaction(ResultSet rs) throws SQLException {
        return new Transaction(rs.getString("transaction_id"), TransactionKind.fromSourceValue(rs.getString("kind")),
                rs.getString("name"), SqliteRows.decimal(rs, "amount_decimal"),
                SqliteRows.datePartOrNull(rs, "occurred_at"), SqliteRows.dateTimeOrNull(rs, "occurred_at"),
                rs.getString("raw_date_text"), rs.getString("month"), rs.getString("from_account_id"),
                rs.getString("to_account_id"), rs.getString("budget_id"), rs.getString("default_budget_id"),
                rs.getString("insert_budget_id"), rs.getString("tags_json"), rs.getString("description"),
                rs.getString("notes"), rs.getString("condition_raw"), rs.getString("paid_raw"),
                rs.getString("spent_raw"), SqliteRows.decimal(rs, "differ_decimal"),
                SqliteRows.decimal(rs, "monitor_decimal"), rs.getString("credit_hold_raw"),
                rs.getString("overpay_raw"), SqliteRows.provenance(rs));
    }

}
