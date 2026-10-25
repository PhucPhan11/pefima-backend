package com.ceu.pefima.backend.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceu.pefima.backend.market.GoldValuation;
import com.ceu.pefima.backend.market.GoldValuationService;

/**
 * Live market values. Unlike the data endpoints these are not tied to a snapshot:
 * they use today's price.
 */
@RestController
@RequestMapping("/api/market")
@Tag(name = "Market", description = "Live prices for holdings the Notion data records only at cost")
class MarketController {

    private final ObjectProvider<GoldValuationService> gold;

    MarketController(ObjectProvider<GoldValuationService> gold) {
        this.gold = gold;
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

}
