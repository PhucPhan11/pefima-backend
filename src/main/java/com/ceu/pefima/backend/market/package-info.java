/**
 * Live market prices used to value holdings that the Notion data records only
 * at cost. This is the backend's only outbound HTTP call; it is enabled only
 * when {@code pefima.gold.quantity-chi} is set.
 */
@NullMarked
package com.ceu.pefima.backend.market;

import org.jspecify.annotations.NullMarked;
