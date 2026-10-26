package com.ceu.pefima.backend.source.sqlite;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.ceu.pefima.backend.source.UnknownSnapshotException;
import com.ceu.pefima.backend.source.model.BudgetEnvelope;
import com.ceu.pefima.backend.source.model.ExportSnapshot;
import com.ceu.pefima.backend.source.model.MonthlyBalance;
import com.ceu.pefima.backend.source.model.Provenance;
import com.ceu.pefima.backend.source.model.ReviewIssue;
import com.ceu.pefima.backend.source.model.Transaction;
import com.ceu.pefima.backend.source.model.TransactionKind;
import com.ceu.pefima.backend.source.model.WalletAccount;

import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.BUDGET;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.EMERGENCY_TARGET;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.EMERGENCY_WALLET;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.EMPTY_SNAPSHOT;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.EXPENSE;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.FUND_COMPANY;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.INCOME;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.MARCH_BALANCE;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.ROOT;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.SNAPSHOT;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.TRANSFER;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.WALLET;
import static com.ceu.pefima.backend.source.sqlite.SyntheticCleanupDatabase.YEAR_BALANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SqliteSourcesTest {

    private static final Provenance PROVENANCE = new Provenance(SNAPSHOT, LocalDate.of(2026, 1, 4), 2026, ROOT);

    @TempDir
    Path tempDir;

    private SqliteCleanupDatabase database;

    @BeforeEach
    void openDatabase() {
        Path file = this.tempDir.resolve("notion-clean.sqlite3");
        SyntheticCleanupDatabase.populated(file);
        this.database = new SqliteCleanupDatabase(file);
    }

    @Test
    void describesContract() {
        assertThat(new SqliteSnapshotSource(this.database).describeContract())
            .satisfies((contract) -> {
                assertThat(contract.schemaVersion()).isEqualTo("1.2");
                assertThat(contract.mappingFileHash()).isEqualTo("mapping-hash-0001");
            });
    }

    @Test
    void listsSnapshotsByExportDate() {
        SqliteSnapshotSource source = new SqliteSnapshotSource(this.database);
        assertThat(source.listSnapshots()).extracting(ExportSnapshot::snapshotId)
            .containsExactly(SNAPSHOT, EMPTY_SNAPSHOT);
        assertThat(source.findSnapshot(SNAPSHOT))
            .contains(new ExportSnapshot(SNAPSHOT, LocalDate.of(2026, 1, 4), 2026, "all", "notion", 1));
        assertThat(source.findSnapshot("missing")).isEmpty();
    }

    @Test
    void readsBudgetEnvelopes() {
        assertThat(new SqliteBudgetEnvelopeSource(this.database).findBySnapshot(SNAPSHOT)).containsExactly(
                new BudgetEnvelope(BUDGET, "Synthetic Envelope", new BigDecimal("100.50"), new BigDecimal("10"),
                        EMERGENCY_TARGET, PROVENANCE));
    }

    @Test
    void readsWalletAccountsWithTheirFundCompanyTitleAndEmergencyFlagOnly() {
        assertThat(new SqliteWalletAccountSource(this.database).findBySnapshot(SNAPSHOT)).containsExactly(
                new WalletAccount(WALLET, "Synthetic Wallet", "[\"Cash\"]", "Yes", new BigDecimal("42.00"), null,
                        new BigDecimal("42.00"), null, null, FUND_COMPANY, false, PROVENANCE),
                new WalletAccount(EMERGENCY_WALLET, "Synthetic Bank", null, null, new BigDecimal("7.00"), null, null,
                        null, null, null, true, PROVENANCE));
    }

    @Test
    void readsMonthlyBalancesWithMonthsBeforeTheYearRow() {
        assertThat(new SqliteMonthlyBalanceSource(this.database).findBySnapshot(SNAPSHOT)).containsExactly(
                new MonthlyBalance(MARCH_BALANCE, "March", 3, new BigDecimal("1000"), new BigDecimal("12.34"),
                        new BigDecimal("987.66"), null, PROVENANCE),
                new MonthlyBalance(YEAR_BALANCE, "Year", null, new BigDecimal("1000"), new BigDecimal("12.34"),
                        new BigDecimal("987.66"), new BigDecimal("987.66"), PROVENANCE));
        assertThat(new SqliteMonthlyBalanceSource(this.database).findBySnapshot(EMPTY_SNAPSHOT)).isEmpty();
    }

    @Test
    void readsTransactionsOrderedByDateWithUndatedFirst() {
        List<Transaction> transactions = new SqliteTransactionSource(this.database).findBySnapshot(SNAPSHOT);
        assertThat(transactions).extracting(Transaction::transactionId).containsExactly(TRANSFER, INCOME, EXPENSE);
        assertThat(transactions).extracting(Transaction::kind)
            .containsExactly(TransactionKind.INTERNAL_TRANSFER, TransactionKind.INCOME, TransactionKind.EXPENSE);
        assertThat(transactions).allSatisfy((transaction) -> assertThat(transaction.provenance()).isEqualTo(PROVENANCE));
    }

    @Test
    void mapsDateOnlyTransaction() {
        Transaction expense = transaction(EXPENSE);
        assertThat(expense.name()).isEqualTo("Synthetic lunch");
        assertThat(expense.amount()).isEqualByComparingTo("12.34");
        assertThat(expense.occurredOn()).isEqualTo(LocalDate.of(2026, 1, 3));
        assertThat(expense.occurredAt()).isNull();
        assertThat(expense.rawDateText()).isEqualTo("January 3, 2026");
        assertThat(expense.month()).isEqualTo("January");
        assertThat(expense.fromAccountId()).isEqualTo(WALLET);
        assertThat(expense.toAccountId()).isNull();
    }

    @Test
    void mapsDateTimeTransactionAndBudgetAllocation() {
        Transaction income = transaction(INCOME);
        assertThat(income.amount()).isEqualByComparingTo("1000");
        assertThat(income.occurredOn()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(income.occurredAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 9, 30));
        assertThat(income.toAccountId()).isEqualTo(WALLET);
        assertThat(income.budgetId()).isEqualTo(BUDGET);
        assertThat(income.defaultBudgetId()).isEqualTo(BUDGET);
        assertThat(income.insertBudgetId()).isNull();
    }

    @Test
    void mapsUndatedTransfer() {
        Transaction transfer = transaction(TRANSFER);
        assertThat(transfer.occurredOn()).isNull();
        assertThat(transfer.occurredAt()).isNull();
        assertThat(transfer.fromAccountId()).isEqualTo(WALLET);
        assertThat(transfer.toAccountId()).isEqualTo(BUDGET);
    }

    @Test
    void readsReviewIssuesWithoutSourceValues() {
        List<ReviewIssue> issues = new SqliteReviewIssueSource(this.database).findBySnapshot(SNAPSHOT);
        assertThat(issues).containsExactly(
                new ReviewIssue("issue-conflict-1", SNAPSHOT, EXPENSE, "Expenses", "Amount", "source_root_conflict",
                        "open", "populated source observations disagree"),
                new ReviewIssue("issue-unknown-1", SNAPSHOT, null, "Expenses", "Synthetic Header",
                        ReviewIssue.UNKNOWN_FIELD, "open", null));
        assertThat(issues).allMatch(ReviewIssue::isOpen);
        assertThat(issues.toString()).doesNotContain("synthetic-a", "synthetic-b");
    }

    @Test
    void returnsEmptyListsForSnapshotWithoutData() {
        assertThat(new SqliteBudgetEnvelopeSource(this.database).findBySnapshot(EMPTY_SNAPSHOT)).isEmpty();
        assertThat(new SqliteWalletAccountSource(this.database).findBySnapshot(EMPTY_SNAPSHOT)).isEmpty();
        assertThat(new SqliteTransactionSource(this.database).findBySnapshot(EMPTY_SNAPSHOT)).isEmpty();
        assertThat(new SqliteReviewIssueSource(this.database).findBySnapshot(EMPTY_SNAPSHOT)).isEmpty();
    }

    @Test
    void rejectsUnknownSnapshot() {
        assertThatExceptionOfType(UnknownSnapshotException.class)
            .isThrownBy(() -> new SqliteBudgetEnvelopeSource(this.database).findBySnapshot("missing"))
            .satisfies((ex) -> assertThat(ex.getSnapshotId()).isEqualTo("missing"));
        assertThatExceptionOfType(UnknownSnapshotException.class)
            .isThrownBy(() -> new SqliteWalletAccountSource(this.database).findBySnapshot("missing"));
        assertThatExceptionOfType(UnknownSnapshotException.class)
            .isThrownBy(() -> new SqliteTransactionSource(this.database).findBySnapshot("missing"));
        assertThatExceptionOfType(UnknownSnapshotException.class)
            .isThrownBy(() -> new SqliteReviewIssueSource(this.database).findBySnapshot("missing"));
    }

    private Transaction transaction(String transactionId) {
        return new SqliteTransactionSource(this.database).findBySnapshot(SNAPSHOT)
            .stream()
            .filter((transaction) -> transaction.transactionId().equals(transactionId))
            .findFirst()
            .orElseThrow();
    }

}
