package com.ceu.pefima.backend.source.sqlite;

import java.util.List;

import com.ceu.pefima.backend.source.MonthlyBalanceSource;
import com.ceu.pefima.backend.source.model.MonthlyBalance;

final class SqliteMonthlyBalanceSource implements MonthlyBalanceSource {

    private final SqliteCleanupDatabase database;

    SqliteMonthlyBalanceSource(SqliteCleanupDatabase database) {
        this.database = database;
    }

    @Override
    public List<MonthlyBalance> findBySnapshot(String snapshotId) {
        this.database.requireSnapshot(snapshotId);
        return this.database.jdbc()
            .sql("""
                    SELECT * FROM monthly_balance_observations
                    WHERE snapshot_id = :id
                    ORDER BY month_number IS NULL, month_number, period, balance_id
                    """)
            .param("id", snapshotId)
            .query((rs, rowNum) -> {
                int monthNumber = rs.getInt("month_number");
                Integer month = rs.wasNull() ? null : monthNumber;
                return new MonthlyBalance(rs.getString("balance_id"), rs.getString("period"), month,
                        SqliteRows.decimal(rs, "income_decimal"),
                        SqliteRows.decimal(rs, "expenses_decimal"), SqliteRows.decimal(rs, "net_decimal"),
                        SqliteRows.decimal(rs, "year_end_decimal"), SqliteRows.provenance(rs));
            })
            .list();
    }

}
