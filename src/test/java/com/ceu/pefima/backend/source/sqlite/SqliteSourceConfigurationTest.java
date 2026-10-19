package com.ceu.pefima.backend.source.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.dao.DataAccessException;

import com.ceu.pefima.backend.source.BudgetEnvelopeSource;
import com.ceu.pefima.backend.source.PefimaSourceException;
import com.ceu.pefima.backend.source.ReviewIssueSource;
import com.ceu.pefima.backend.source.SnapshotSource;
import com.ceu.pefima.backend.source.TransactionSource;
import com.ceu.pefima.backend.source.WalletAccountSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SqliteSourceConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(SqliteSourceConfiguration.class);

    @TempDir
    Path tempDir;

    @Test
    void registersNothingWithoutPath() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SnapshotSource.class);
            assertThat(context).doesNotHaveBean(SqliteCleanupDatabase.class);
        });
    }

    @Test
    void registersAllSourcesWithoutExposingDataSource() {
        Path file = this.tempDir.resolve("notion-clean.sqlite3");
        SyntheticCleanupDatabase.populated(file);
        this.contextRunner.withPropertyValues("pefima.source.sqlite.path=" + file).run((context) -> {
            assertThat(context).hasSingleBean(SnapshotSource.class);
            assertThat(context).hasSingleBean(BudgetEnvelopeSource.class);
            assertThat(context).hasSingleBean(WalletAccountSource.class);
            assertThat(context).hasSingleBean(TransactionSource.class);
            assertThat(context).hasSingleBean(ReviewIssueSource.class);
            assertThat(context).doesNotHaveBean(DataSource.class);
            assertThat(context.getBean(SnapshotSource.class).listSnapshots()).hasSize(2);
        });
    }

    @Test
    void failsWhenFileIsMissing() {
        Path file = this.tempDir.resolve("missing.sqlite3");
        this.contextRunner.withPropertyValues("pefima.source.sqlite.path=" + file).run((context) -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                .isInstanceOf(PefimaSourceException.class)
                .hasMessageContaining("not found");
        });
        assertThat(file).doesNotExist();
    }

    @Test
    void failsOnUnsupportedSchemaVersion() {
        Path file = this.tempDir.resolve("notion-clean.sqlite3");
        SyntheticCleanupDatabase.empty(file).setSchemaVersion("2.0");
        assertThatExceptionOfType(PefimaSourceException.class).isThrownBy(() -> new SqliteCleanupDatabase(file))
            .withMessageContaining("Unsupported notion_cleanup schema version 2.0");
    }

    @Test
    void failsOnFileThatIsNotACleanupDatabase() throws IOException {
        Path file = Files.createFile(this.tempDir.resolve("other.sqlite3"));
        assertThatExceptionOfType(PefimaSourceException.class).isThrownBy(() -> new SqliteCleanupDatabase(file))
            .withMessageContaining("Not a notion_cleanup database");
    }

    @Test
    void opensDatabaseReadOnly() {
        Path file = this.tempDir.resolve("notion-clean.sqlite3");
        SyntheticCleanupDatabase writable = SyntheticCleanupDatabase.populated(file);
        SqliteCleanupDatabase database = new SqliteCleanupDatabase(file);
        assertThatExceptionOfType(DataAccessException.class)
            .isThrownBy(() -> database.jdbc().sql("UPDATE schema_meta SET value = 'changed'").update());
        assertThat(writable.jdbc()
            .sql("SELECT value FROM schema_meta WHERE key = 'schema_version'")
            .query(String.class)
            .single()).isEqualTo("1.0");
    }

}
