package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads PNJ's public gold price list. Prices there are in thousand VND per chỉ;
 * a price the shop does not quote comes as an empty string.
 */
class PnjGoldPriceClient implements GoldPriceClient {

    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private static final DateTimeFormatter UPDATE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final RestClient restClient;

    private final URI priceUrl;

    private final String productCode;

    private final Clock clock;

    PnjGoldPriceClient(RestClient restClient, URI priceUrl, String productCode, Clock clock) {
        this.restClient = restClient;
        this.priceUrl = priceUrl;
        this.productCode = productCode;
        this.clock = clock;
    }

    @Override
    public GoldQuote fetch() {
        JsonNode body;
        try {
            body = this.restClient.get().uri(this.priceUrl).retrieve().body(JsonNode.class);
        }
        catch (RestClientException ex) {
            throw new MarketPriceUnavailableException("Could not read the PNJ gold price list", ex);
        }
        if (body == null) {
            throw new MarketPriceUnavailableException("The PNJ gold price list was empty");
        }
        for (JsonNode item : body.path("data")) {
            if (this.productCode.equals(item.path("masp").asString(""))) {
                BigDecimal buy = price(item.path("giamua"));
                if (buy == null) {
                    throw new MarketPriceUnavailableException(
                            "PNJ has no buy price for gold product " + this.productCode);
                }
                return new GoldQuote(this.productCode, item.path("tensp").asString(this.productCode), buy,
                        price(item.path("giaban")), updateDate(body.path("updateDate")), this.clock.instant());
            }
        }
        throw new MarketPriceUnavailableException("PNJ lists no gold product " + this.productCode);
    }

    private static @Nullable BigDecimal price(JsonNode node) {
        BigDecimal thousands;
        if (node.isNumber()) {
            thousands = node.decimalValue();
        }
        else if (node.isString() && !node.asString().isBlank()) {
            try {
                thousands = new BigDecimal(node.asString().trim().replace(",", ""));
            }
            catch (NumberFormatException ex) {
                return null;
            }
        }
        else {
            return null;
        }
        return thousands.signum() > 0 ? thousands.multiply(THOUSAND) : null;
    }

    private static @Nullable LocalDateTime updateDate(JsonNode node) {
        if (!node.isString()) {
            return null;
        }
        try {
            return LocalDateTime.parse(node.asString().trim(), UPDATE_DATE);
        }
        catch (DateTimeParseException ex) {
            return null;
        }
    }

}
