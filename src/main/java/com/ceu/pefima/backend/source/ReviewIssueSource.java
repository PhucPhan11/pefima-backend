package com.ceu.pefima.backend.source;

import java.util.List;

import com.ceu.pefima.backend.source.model.ReviewIssue;

/**
 * Conflicts and unknown fields recorded for one snapshot. Data affected by an
 * open issue should not be treated as clean.
 */
public interface ReviewIssueSource {

    /**
     * @throws UnknownSnapshotException if the snapshot does not exist
     */
    List<ReviewIssue> findBySnapshot(String snapshotId);

}
