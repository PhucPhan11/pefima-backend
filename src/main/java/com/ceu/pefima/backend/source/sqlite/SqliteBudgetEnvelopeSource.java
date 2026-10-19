package com.ceu.pefima.backend.source.sqlite;

import java.util.List;

import com.ceu.pefima.backend.source.BudgetEnvelopeSource;
import com.ceu.pefima.backend.source.model.BudgetEnvelope;

final class SqliteBudgetEnvelopeSource implements BudgetEnvelopeSource {

    private final SqliteCleanupDatabase database;

    SqliteBudgetEnvelopeSource(SqliteCleanupDatabase database) {
        this.database = database;
    }

    @Override
    public List<BudgetEnvelope> findBySnapshot(String snapshotId) {
        this.database.requireSnapshot(snapshotId);
        return this.database.jdbc()
            .sql("""
                    SELECT * FROM budget_envelope_observations
                    WHERE snapshot_id = :id
                    ORDER BY budget_id
                    """)
            .param("id", snapshotId)
            .query((rs, rowNum) -> new BudgetEnvelope(rs.getString("budget_id"), rs.getString("name"),
                    SqliteRows.decimal(rs, "available_balance_decimal"),
                    SqliteRows.decimal(rs, "starting_balance_decimal"), rs.getString("emergency_relation_id"),
                    SqliteRows.provenance(rs)))
            .list();
    }

}
