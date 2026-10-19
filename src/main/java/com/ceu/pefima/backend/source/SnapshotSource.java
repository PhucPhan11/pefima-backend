package com.ceu.pefima.backend.source;

import java.util.List;
import java.util.Optional;

import com.ceu.pefima.backend.source.model.ExportSnapshot;
import com.ceu.pefima.backend.source.model.SourceContract;

/**
 * Lists the export snapshots a source holds, so callers can choose one explicitly.
 */
public interface SnapshotSource {

    /** Describes the data contract the source was produced with. */
    SourceContract describeContract();

    /** Returns every snapshot, ordered by export date then snapshot ID. */
    List<ExportSnapshot> listSnapshots();

    Optional<ExportSnapshot> findSnapshot(String snapshotId);

}
