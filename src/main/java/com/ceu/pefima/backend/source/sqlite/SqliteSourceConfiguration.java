package com.ceu.pefima.backend.source.sqlite;

import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ceu.pefima.backend.source.BudgetEnvelopeSource;
import com.ceu.pefima.backend.source.ReviewIssueSource;
import com.ceu.pefima.backend.source.SnapshotSource;
import com.ceu.pefima.backend.source.TransactionSource;
import com.ceu.pefima.backend.source.WalletAccountSource;

/**
 * Registers the SQLite-backed source ports. The SQLite connection stays private
 * to these adapters instead of being exposed as a {@code DataSource} bean, so it
 * can never become the application's primary database.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "pefima.source.sqlite", name = "path")
@EnableConfigurationProperties(SqliteSourceProperties.class)
public class SqliteSourceConfiguration {

    @Bean
    SqliteCleanupDatabase pefimaSqliteCleanupDatabase(SqliteSourceProperties properties) {
        return new SqliteCleanupDatabase(Path.of(properties.path()));
    }

    @Bean
    SnapshotSource snapshotSource(SqliteCleanupDatabase database) {
        return new SqliteSnapshotSource(database);
    }

    @Bean
    BudgetEnvelopeSource budgetEnvelopeSource(SqliteCleanupDatabase database) {
        return new SqliteBudgetEnvelopeSource(database);
    }

    @Bean
    WalletAccountSource walletAccountSource(SqliteCleanupDatabase database) {
        return new SqliteWalletAccountSource(database);
    }

    @Bean
    TransactionSource transactionSource(SqliteCleanupDatabase database) {
        return new SqliteTransactionSource(database);
    }

    @Bean
    ReviewIssueSource reviewIssueSource(SqliteCleanupDatabase database) {
        return new SqliteReviewIssueSource(database);
    }

}
