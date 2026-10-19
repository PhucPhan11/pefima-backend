package com.ceu.pefima.backend.source.sqlite;

import java.util.List;

import com.ceu.pefima.backend.source.ReviewIssueSource;
import com.ceu.pefima.backend.source.model.ReviewIssue;

/**
 * Reads {@code conflicts} and {@code unknown_fields}. The conflicting values
 * ({@code value_a}/{@code value_b}) are raw financial data and are never selected.
 */
final class SqliteReviewIssueSource implements ReviewIssueSource {

    private final SqliteCleanupDatabase database;

    SqliteReviewIssueSource(SqliteCleanupDatabase database) {
        this.database = database;
    }

    @Override
    public List<ReviewIssue> findBySnapshot(String snapshotId) {
        this.database.requireSnapshot(snapshotId);
        return this.database.jdbc()
            .sql("""
                    SELECT issue_key, snapshot_id, page_id, category, canonical_field AS field,
                           conflict_kind AS kind, status, message
                    FROM conflicts
                    WHERE snapshot_id = :id
                    UNION ALL
                    SELECT issue_key, snapshot_id, page_id, category, source_field AS field,
                           :unknownField AS kind, status, NULL AS message
                    FROM unknown_fields
                    WHERE snapshot_id = :id
                    ORDER BY kind, issue_key
                    """)
            .param("id", snapshotId)
            .param("unknownField", ReviewIssue.UNKNOWN_FIELD)
            .query((rs, rowNum) -> new ReviewIssue(rs.getString("issue_key"), rs.getString("snapshot_id"),
                    rs.getString("page_id"), rs.getString("category"), rs.getString("field"),
                    rs.getString("kind"), rs.getString("status"), rs.getString("message")))
            .list();
    }

}
