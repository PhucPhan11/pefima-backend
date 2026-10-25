package com.ceu.pefima.backend.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceu.pefima.backend.market.ExchangeRateService;
import com.ceu.pefima.backend.market.ExchangeRates;
import com.ceu.pefima.backend.market.GoldValuation;
import com.ceu.pefima.backend.market.GoldValuationService;

/**
 * Live market values. Unlike the data endpoints these are not tied to a snapshot:
 * they use today's price.
 */
@RestController
@RequestMapping("/api/market")
@Tag(name = "Market", description = "Live prices for holdings the Notion data does not value in VND")
class MarketController {

    private final ObjectProvider<GoldValuationService> gold;

    private final ObjectProvider<ExchangeRateService> exchangeRates;

    MarketController(ObjectProvider<GoldValuationService> gold, ObjectProvider<ExchangeRateService> exchangeRates) {
        this.gold = gold;
        this.exchangeRates = exchangeRates;
    }

    @GetMapping("/gold")
    @Operation(summary = "Value the gold holding at today's price",
            description = "Configured quantity in chỉ times PNJ's current buy price for ring gold 999.9. "
                    + "The quote is cached for a few minutes.")
    GoldValuation gold() {
        GoldValuationService service = this.gold.getIfAvailable();
        if (service == null) {
            throw new SourceNotConfiguredException("Gold valuation is not configured. Set pefima.gold.quantity-chi "
                    + "(for example in pefima-local.properties) and restart.");
        }
        return service.current();
    }

    @GetMapping("/fx-rates")
    @Operation(summary = "Today's exchange rates for the configured currencies",
            description = "Vietcombank's cash buy rate in VND per unit, or its transfer buy rate when it does not "
                    + "buy that currency in cash. The rate list is cached for at least 5 minutes.")
    ExchangeRates exchangeRates() {
        ExchangeRateService service = this.exchangeRates.getIfAvailable();
        if (service == null) {
            throw new SourceNotConfiguredException("Exchange rates are not configured. Set pefima.fx.currencies "
                    + "(for example in pefima-local.properties) and restart.");
        }
        return service.current();
    }

}
