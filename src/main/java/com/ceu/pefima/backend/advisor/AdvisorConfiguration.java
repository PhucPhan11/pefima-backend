package com.ceu.pefima.backend.advisor;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Registers the advisor only when {@code pefima.advisor.enabled} is true. The LLM
 * must listen on a loopback address, so profile figures never leave the machine;
 * any other host fails the startup.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "pefima.advisor", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AdvisorProperties.class)
public class AdvisorConfiguration {

    @Bean
    LmStudioClient lmStudioClient(AdvisorProperties properties) {
        requireLoopback(properties.baseUrl());
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        return new LmStudioClient(restClient, properties.baseUrl(), properties.model(), properties.temperature(),
                properties.maxTokens());
    }

    @Bean
    AdvisorService advisorService(LmStudioClient client) {
        return new AdvisorService(client);
    }

    static void requireLoopback(URI baseUrl) {
        String host = baseUrl.getHost();
        boolean loopback;
        try {
            loopback = host != null && InetAddress.getByName(host).isLoopbackAddress();
        }
        catch (UnknownHostException ex) {
            loopback = false;
        }
        if (!loopback) {
            throw new IllegalStateException("pefima.advisor.base-url must point at this machine (localhost or "
                    + "127.0.0.1) so that profile figures stay local");
        }
    }

}
