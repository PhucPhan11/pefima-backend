/**
 * Live market prices: gold, valued from a configured quantity, and exchange
 * rates for foreign-currency wallets. These are the backend's only outbound HTTP
 * calls; each feed is enabled only when its setting ({@code pefima.gold.quantity-chi},
 * {@code pefima.fx.currencies}) is present.
 */
@NullMarked
package com.ceu.pefima.backend.market;

import org.jspecify.annotations.NullMarked;
