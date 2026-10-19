package com.ceu.pefima.backend.source;

import java.util.List;

import com.ceu.pefima.backend.source.model.BudgetEnvelope;

/**
 * Budget envelopes (for example the Emergency Fund) observed in one snapshot.
 */
public interface BudgetEnvelopeSource {

    /**
     * @throws UnknownSnapshotException if the snapshot does not exist
     */
    List<BudgetEnvelope> findBySnapshot(String snapshotId);

}
