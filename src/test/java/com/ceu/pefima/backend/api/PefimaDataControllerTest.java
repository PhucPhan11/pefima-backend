package com.ceu.pefima.backend.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.ceu.pefima.backend.source.BudgetEnvelopeSource;
import com.ceu.pefima.backend.source.ReviewIssueSource;
import com.ceu.pefima.backend.source.SnapshotSource;
import com.ceu.pefima.backend.source.TransactionSource;
import com.ceu.pefima.backend.source.UnknownSnapshotException;
import com.ceu.pefima.backend.source.WalletAccountSource;
import com.ceu.pefima.backend.source.model.ExportSnapshot;
import com.ceu.pefima.backend.source.model.Provenance;
import com.ceu.pefima.backend.source.model.ReviewIssue;
import com.ceu.pefima.backend.source.model.SourceContract;
import com.ceu.pefima.backend.source.model.Transaction;
import com.ceu.pefima.backend.source.model.TransactionKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(PefimaDataController.class)
class PefimaDataControllerTest {

    private static final String SNAPSHOT = "snapshot-0001";

    private static final Provenance PROVENANCE = new Provenance(SNAPSHOT, LocalDate.of(2026, 1, 4), 2026,
            "root-0001");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SnapshotSource snapshots;

    @MockitoBean
    private BudgetEnvelopeSource budgets;

    @MockitoBean
    private WalletAccountSource wallets;

    @MockitoBean
    private TransactionSource transactions;

    @MockitoBean
    private ReviewIssueSource reviewIssues;

    @Test
    void describesContract() {
        given(this.snapshots.describeContract()).willReturn(new SourceContract("1.0", "mapping-hash"));
        assertThat(this.mvc.get().uri("/api/source/contract")).hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                    {"schemaVersion": "1.0", "mappingFileHash": "mapping-hash"}
                    """);
    }

    @Test
    void listsSnapshotsWithIsoDates() {
        given(this.snapshots.listSnapshots())
            .willReturn(List.of(new ExportSnapshot(SNAPSHOT, LocalDate.of(2026, 1, 4), 2026, "all", "notion", 1)));
        assertThat(this.mvc.get().uri("/api/snapshots")).hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                    [{"snapshotId": "snapshot-0001", "exportDate": "2026-01-04", "year": 2026, "view": "all",
                      "sourceFormat": "notion", "sourceRootCount": 1}]
                    """);
    }

    @Test
    void getsSnapshotOrAnswers404() {
        given(this.snapshots.findSnapshot("missing")).willReturn(Optional.empty());
        assertThat(this.mvc.get().uri("/api/snapshots/missing")).hasStatus(HttpStatus.NOT_FOUND)
            .bodyJson()
            .isLenientlyEqualTo("""
                    {"status": 404, "title": "Unknown snapshot", "detail": "Unknown export snapshot: missing"}
                    """);
    }

    @Test
    void listsTransactions() {
        Transaction income = new Transaction("txn-1", TransactionKind.INCOME, "Synthetic salary",
                new BigDecimal("1000.50"), LocalDate.of(2026, 1, 2), LocalDateTime.of(2026, 1, 2, 9, 30),
                "January 2, 2026 9:30 AM", null, null, "wallet-1", "budget-1", "budget-1", null, null, null, null,
                null, null, null, null, null, null, null, PROVENANCE);
        given(this.transactions.findBySnapshot(SNAPSHOT)).willReturn(List.of(income));
        assertThat(this.mvc.get().uri("/api/snapshots/{id}/transactions", SNAPSHOT)).hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                    [{"transactionId": "txn-1", "kind": "INCOME", "amount": 1000.50,
                      "occurredOn": "2026-01-02", "occurredAt": "2026-01-02T09:30:00",
                      "toAccountId": "wallet-1", "budgetId": "budget-1",
                      "provenance": {"snapshotId": "snapshot-0001", "exportDate": "2026-01-04"}}]
                    """);
    }

    @Test
    void answers404ForUnknownSnapshotOnCollections() {
        given(this.budgets.findBySnapshot("missing")).willThrow(new UnknownSnapshotException("missing"));
        given(this.wallets.findBySnapshot("missing")).willThrow(new UnknownSnapshotException("missing"));
        given(this.reviewIssues.findBySnapshot("missing")).willThrow(new UnknownSnapshotException("missing"));
        assertThat(this.mvc.get().uri("/api/snapshots/missing/budgets")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(this.mvc.get().uri("/api/snapshots/missing/wallets")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(this.mvc.get().uri("/api/snapshots/missing/review-issues")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void listsReviewIssues() {
        given(this.reviewIssues.findBySnapshot(SNAPSHOT)).willReturn(List.of(new ReviewIssue("issue-1", SNAPSHOT,
                null, "Expenses", "Synthetic Header", ReviewIssue.UNKNOWN_FIELD, "open", null)));
        assertThat(this.mvc.get().uri("/api/snapshots/{id}/review-issues", SNAPSHOT)).hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                    [{"issueKey": "issue-1", "kind": "unknown_field", "status": "open"}]
                    """);
    }

}
