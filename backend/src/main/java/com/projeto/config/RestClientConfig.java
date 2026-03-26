package com.projeto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${anthropic.api.url}")
    private String anthropicUrl;

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    @Value("${anthropic.api.version}")
    private String anthropicVersion;


    @Bean("anthropicRestClient")
    public RestClient anthropicRestClient() {
        return RestClient.builder()
                .baseUrl(anthropicUrl)
                .defaultHeader("x-api-key", anthropicApiKey)
                .defaultHeader("anthropic-version", anthropicVersion)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
