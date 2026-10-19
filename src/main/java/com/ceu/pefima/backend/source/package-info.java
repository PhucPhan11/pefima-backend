/**
 * Read-only ports for collecting PeFiMa data from {@code pefima-data}.
 * <p>
 * The interfaces here do not depend on a storage format. Every data query takes
 * an explicit snapshot ID: callers choose which export they trust, and adapters
 * never pick a "latest" snapshot on their own.
 */
@NullMarked
package com.ceu.pefima.backend.source;

import org.jspecify.annotations.NullMarked;
