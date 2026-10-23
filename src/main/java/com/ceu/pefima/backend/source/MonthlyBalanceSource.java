package com.ceu.pefima.backend.source;

import java.util.List;

import com.ceu.pefima.backend.source.model.MonthlyBalance;

/**
 * Monthly cash-flow summaries (the Notion Balance database) observed in one snapshot.
 */
public interface MonthlyBalanceSource {

    /**
     * Months in calendar order, then the {@code Year} row.
     * @throws UnknownSnapshotException if the snapshot does not exist
     */
    List<MonthlyBalance> findBySnapshot(String snapshotId);

}
