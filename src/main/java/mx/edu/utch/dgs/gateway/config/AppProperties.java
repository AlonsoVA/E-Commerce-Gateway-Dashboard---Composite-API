package mx.edu.utch.dgs.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String internalBaseUrl, Currency currency, Http http) {

    public record Currency(String baseUrl, Duration cacheTtl) {
    }

    public record Http(Duration connectTimeout, Duration readTimeout) {
    }
}
