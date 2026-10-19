package com.ceu.pefima.backend.source;

import java.util.List;

import com.ceu.pefima.backend.source.model.Transaction;

/**
 * Expenses, income, and internal transfers observed in one snapshot.
 */
public interface TransactionSource {

    /**
     * Returns transactions ordered by date (undated first), then transaction ID.
     * @throws UnknownSnapshotException if the snapshot does not exist
     */
    List<Transaction> findBySnapshot(String snapshotId);

}
