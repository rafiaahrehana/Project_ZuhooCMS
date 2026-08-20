package com.businessos.modules.ai.config;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate(RestTemplateBuilder builder, AiProperties props) {
        return builder
            .connectTimeout(Duration.ofMillis(props.getGlobalTimeoutMs()))
            .readTimeout(Duration.ofMillis(props.getGlobalTimeoutMs()))
            .build();
    }
}
