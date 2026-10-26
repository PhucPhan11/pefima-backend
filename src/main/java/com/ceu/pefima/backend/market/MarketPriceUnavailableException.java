package com.ceu.pefima.backend.market;

/**
 * Raised when a market price cannot be fetched or read. Messages never contain prices.
 */
public class MarketPriceUnavailableException extends RuntimeException {

    public MarketPriceUnavailableException(String message) {
        super(message);
    }

    public MarketPriceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
