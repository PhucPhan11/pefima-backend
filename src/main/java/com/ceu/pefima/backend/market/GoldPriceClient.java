package com.ceu.pefima.backend.market;

/**
 * Fetches the current price of the configured gold product.
 */
public interface GoldPriceClient {

    /**
     * @throws MarketPriceUnavailableException when the feed cannot be read or has no buy price
     */
    GoldQuote fetch();

}
