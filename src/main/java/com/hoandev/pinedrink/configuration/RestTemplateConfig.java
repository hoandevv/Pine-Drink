package com.hoandev.pinedrink.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate with timeout settings.
 * Used for external API calls (e.g., Nominatim geocoding).
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a RestTemplate bean with configured timeouts.
     * 
     * @param builder the RestTemplateBuilder
     * @return configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))  // Connection timeout: 3 seconds
                .readTimeout(Duration.ofSeconds(5))     // Read timeout: 5 seconds
                .build();
    }
}
