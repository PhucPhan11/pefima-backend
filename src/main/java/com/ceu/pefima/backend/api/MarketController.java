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
    @Operation(summary = "Today's gold price",
            description = "PNJ's current buy and sell price per chỉ for ring gold 999.9, plus the value of "
                    + "pefima.gold.quantity-chi when it is set. The quote is cached for a few minutes.")
    GoldValuation gold() {
        GoldValuationService service = this.gold.getIfAvailable();
        if (service == null) {
            throw new SourceNotConfiguredException("The gold price is not configured. Set pefima.gold.enabled=true "
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
