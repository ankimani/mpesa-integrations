package com.mpesa.integration;

import com.mpesa.integration.config.MpesaDarajaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({MpesaDarajaProperties.class})
public class MpesaIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpesaIntegrationApplication.class, args);
    }
}
