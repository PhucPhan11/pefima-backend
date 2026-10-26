package com.ceu.pefima.backend.api;

/**
 * Raised when an endpoint needs a data source that has not been configured.
 */
class SourceNotConfiguredException extends RuntimeException {

    SourceNotConfiguredException() {
        super("No PeFiMa data source is configured. Set pefima.source.sqlite.path to the notion_cleanup "
                + "SQLite database and restart.");
    }

    SourceNotConfiguredException(String message) {
        super(message);
    }

}
