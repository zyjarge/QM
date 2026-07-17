package com.qm.common;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
public class MeilisearchConfig {

    @Value("${meilisearch.host:http://localhost:7700}")
    private String host;

    @Value("${meilisearch.master-key:}")
    private String apiKey;

    public static final String INDEX_REQUIREMENTS = "requirements";

    @Bean
    public RestTemplate meiliRestTemplate() {
        return new RestTemplate();
    }
}
