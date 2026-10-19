package com.ceu.pefima.backend.api;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceu.pefima.backend.source.BudgetEnvelopeSource;
import com.ceu.pefima.backend.source.ReviewIssueSource;
import com.ceu.pefima.backend.source.SnapshotSource;
import com.ceu.pefima.backend.source.TransactionSource;
import com.ceu.pefima.backend.source.UnknownSnapshotException;
import com.ceu.pefima.backend.source.WalletAccountSource;
import com.ceu.pefima.backend.source.model.BudgetEnvelope;
import com.ceu.pefima.backend.source.model.ExportSnapshot;
import com.ceu.pefima.backend.source.model.ReviewIssue;
import com.ceu.pefima.backend.source.model.SourceContract;
import com.ceu.pefima.backend.source.model.Transaction;
import com.ceu.pefima.backend.source.model.WalletAccount;

/**
 * Read-only endpoints over the PeFiMa data source. Sources are looked up lazily
 * so the API (and Swagger UI) still starts when no source is configured; such
 * calls answer 503 instead.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "PeFiMa data", description = "Read-only data collected from pefima-data")
class PefimaDataController {

    private final ObjectProvider<SnapshotSource> snapshots;

    private final ObjectProvider<BudgetEnvelopeSource> budgets;

    private final ObjectProvider<WalletAccountSource> wallets;

    private final ObjectProvider<TransactionSource> transactions;

    private final ObjectProvider<ReviewIssueSource> reviewIssues;

    PefimaDataController(ObjectProvider<SnapshotSource> snapshots, ObjectProvider<BudgetEnvelopeSource> budgets,
            ObjectProvider<WalletAccountSource> wallets, ObjectProvider<TransactionSource> transactions,
            ObjectProvider<ReviewIssueSource> reviewIssues) {
        this.snapshots = snapshots;
        this.budgets = budgets;
        this.wallets = wallets;
        this.transactions = transactions;
        this.reviewIssues = reviewIssues;
    }

    @GetMapping("/source/contract")
    @Operation(summary = "Describe the source data contract", description = "Schema version and mapping-file hash.")
    SourceContract contract() {
        return require(this.snapshots).describeContract();
    }

    @GetMapping("/snapshots")
    @Operation(summary = "List export snapshots",
            description = "Ordered by export date. Pick a snapshot ID here for the other endpoints.")
    List<ExportSnapshot> listSnapshots() {
        return require(this.snapshots).listSnapshots();
    }

    @GetMapping("/snapshots/{snapshotId}")
    @Operation(summary = "Get one export snapshot")
    ExportSnapshot getSnapshot(@PathVariable @Parameter(description = "Snapshot ID") String snapshotId) {
        return require(this.snapshots).findSnapshot(snapshotId)
            .orElseThrow(() -> new UnknownSnapshotException(snapshotId));
    }

    @GetMapping("/snapshots/{snapshotId}/budgets")
    @Operation(summary = "List budget envelopes in a snapshot")
    List<BudgetEnvelope> listBudgets(@PathVariable @Parameter(description = "Snapshot ID") String snapshotId) {
        return require(this.budgets).findBySnapshot(snapshotId);
    }

    @GetMapping("/snapshots/{snapshotId}/wallets")
    @Operation(summary = "List wallet accounts in a snapshot")
    List<WalletAccount> listWallets(@PathVariable @Parameter(description = "Snapshot ID") String snapshotId) {
        return require(this.wallets).findBySnapshot(snapshotId);
    }

    @GetMapping("/snapshots/{snapshotId}/transactions")
    @Operation(summary = "List transactions in a snapshot",
            description = "Expenses, income, and internal transfers, ordered by date (undated first).")
    List<Transaction> listTransactions(@PathVariable @Parameter(description = "Snapshot ID") String snapshotId) {
        return require(this.transactions).findBySnapshot(snapshotId);
    }

    @GetMapping("/snapshots/{snapshotId}/review-issues")
    @Operation(summary = "List review issues in a snapshot",
            description = "Conflicts and unknown fields. Data affected by an open issue is not clean.")
    List<ReviewIssue> listReviewIssues(@PathVariable @Parameter(description = "Snapshot ID") String snapshotId) {
        return require(this.reviewIssues).findBySnapshot(snapshotId);
    }

    private static <T> T require(ObjectProvider<T> source) {
        T available = source.getIfAvailable();
        if (available == null) {
            throw new SourceNotConfiguredException();
        }
        return available;
    }

}
