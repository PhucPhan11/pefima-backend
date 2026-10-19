package com.ceu.pefima.backend.source.sqlite;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ceu.pefima.backend.source.SnapshotSource;
import com.ceu.pefima.backend.source.model.ExportSnapshot;
import com.ceu.pefima.backend.source.model.SourceContract;

final class SqliteSnapshotSource implements SnapshotSource {

    private static final String SELECT_SNAPSHOTS = """
            SELECT snapshot_id, export_date, year, view, source_format, source_root_count
            FROM export_snapshots
            """;

    private final SqliteCleanupDatabase database;

    SqliteSnapshotSource(SqliteCleanupDatabase database) {
        this.database = database;
    }

    @Override
    public SourceContract describeContract() {
        Map<String, String> meta = this.database.jdbc()
            .sql("SELECT key, value FROM schema_meta")
            .query((rs, rowNum) -> Map.entry(rs.getString("key"), rs.getString("value")))
            .list()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new SourceContract(meta.get("schema_version"), meta.get("mapping_file_hash"));
    }

    @Override
    public List<ExportSnapshot> listSnapshots() {
        return this.database.jdbc()
            .sql(SELECT_SNAPSHOTS + "ORDER BY export_date, snapshot_id")
            .query((rs, rowNum) -> snapshot(rs))
            .list();
    }

    @Override
    public Optional<ExportSnapshot> findSnapshot(String snapshotId) {
        return this.database.jdbc()
            .sql(SELECT_SNAPSHOTS + "WHERE snapshot_id = :id")
            .param("id", snapshotId)
            .query((rs, rowNum) -> snapshot(rs))
            .optional();
    }

    private static ExportSnapshot snapshot(ResultSet rs) throws SQLException {
        return new ExportSnapshot(rs.getString("snapshot_id"), SqliteRows.date(rs, "export_date"), rs.getInt("year"),
                rs.getString("view"), rs.getString("source_format"), rs.getInt("source_root_count"));
    }

}
