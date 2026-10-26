package com.ceu.pefima.backend.source.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.ceu.pefima.backend.source.PefimaSourceException;
import com.ceu.pefima.backend.source.UnknownSnapshotException;

/**
 * Read-only connection to a {@code notion_cleanup} database. Opening it fails
 * fast when the file is missing or its schema version is not supported.
 */
final class SqliteCleanupDatabase {

    static final String SUPPORTED_SCHEMA_VERSION = "1.2";

    private final Path path;

    private final SQLiteDataSource dataSource;

    private final JdbcClient jdbc;

    SqliteCleanupDatabase(Path path) {
        this.path = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(this.path)) {
            throw new PefimaSourceException("PeFiMa SQLite database not found: " + this.path);
        }
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        this.dataSource = new SQLiteDataSource(config);
        this.dataSource.setUrl("jdbc:sqlite:" + this.path);
        this.jdbc = JdbcClient.create(this.dataSource);
        verifySchemaVersion();
    }

    private void verifySchemaVersion() {
        @Nullable String version;
        try {
            version = this.jdbc.sql("SELECT value FROM schema_meta WHERE key = 'schema_version'")
                .query(String.class)
                .optional()
                .orElse(null);
        }
        catch (DataAccessException ex) {
            throw new PefimaSourceException("Not a notion_cleanup database: " + this.path, ex);
        }
        if (!SUPPORTED_SCHEMA_VERSION.equals(version)) {
            throw new PefimaSourceException("Unsupported notion_cleanup schema version " + version + " in "
                    + this.path + " (expected " + SUPPORTED_SCHEMA_VERSION + ")");
        }
    }

    Path path() {
        return this.path;
    }

    SQLiteDataSource dataSource() {
        return this.dataSource;
    }

    JdbcClient jdbc() {
        return this.jdbc;
    }

    void requireSnapshot(String snapshotId) {
        Integer count = this.jdbc.sql("SELECT COUNT(*) FROM export_snapshots WHERE snapshot_id = :id")
            .param("id", snapshotId)
            .query(Integer.class)
            .single();
        if (count == 0) {
            throw new UnknownSnapshotException(snapshotId);
        }
    }

}
