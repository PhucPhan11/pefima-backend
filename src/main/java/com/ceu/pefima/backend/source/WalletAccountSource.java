package com.ceu.pefima.backend.source;

import java.util.List;

import com.ceu.pefima.backend.source.model.WalletAccount;

/**
 * Wallet accounts observed in one snapshot.
 */
public interface WalletAccountSource {

    /**
     * @throws UnknownSnapshotException if the snapshot does not exist
     */
    List<WalletAccount> findBySnapshot(String snapshotId);

}
