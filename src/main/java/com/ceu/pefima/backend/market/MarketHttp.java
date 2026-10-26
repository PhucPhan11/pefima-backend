package com.ceu.pefima.backend.market;

import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The HTTP client shared by the market price feeds.
 */
final class MarketHttp {

    private MarketHttp() {
    }

    static RestClient restClient(Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
            .requestFactory(requestFactory)
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; pefima-backend)")
            .build();
    }

}
