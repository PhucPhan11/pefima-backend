package com.ceu.pefima.backend.source;

/**
 * Raised when a query names a snapshot that the source does not contain.
 */
public class UnknownSnapshotException extends PefimaSourceException {

    private final String snapshotId;

    public UnknownSnapshotException(String snapshotId) {
        super("Unknown export snapshot: " + snapshotId);
        this.snapshotId = snapshotId;
    }

    public String getSnapshotId() {
        return this.snapshotId;
    }

}
