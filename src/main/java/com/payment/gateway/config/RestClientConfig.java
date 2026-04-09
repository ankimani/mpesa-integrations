package com.mpesa.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient mpesaRestClient(MpesaDarajaProperties mpesaDarajaSettings) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(mpesaDarajaSettings.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(mpesaDarajaSettings.getReadTimeoutMs()));
        return RestClient.builder()
                .baseUrl(mpesaDarajaSettings.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
