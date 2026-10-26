/**
 * Live market prices: the gold price per chỉ for the Gold wallet, and exchange
 * rates for foreign-currency wallets. These are the backend's only outbound HTTP
 * calls; each feed is enabled only by its setting ({@code pefima.gold.enabled=true},
 * {@code pefima.fx.currencies}).
 */
@NullMarked
package com.ceu.pefima.backend.market;

import org.jspecify.annotations.NullMarked;
