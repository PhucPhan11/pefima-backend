package com.ceu.pefima.backend.market;

/**
 * Fetches the current exchange-rate list.
 */
public interface ExchangeRateClient {

    /**
     * @throws MarketPriceUnavailableException when the feed cannot be read
     */
    FxQuote fetch();

}
