package org.example.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PetApiConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            log.info("Calling Pets API {}", requestTemplate.url());
        };
    }
}
