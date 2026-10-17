package com.jk.explore.security.xssspring;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriterFilter;

/** The store: Thymeleaf pages, and Spring Security writing a strict Content-Security-Policy with this request's nonce. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(ReviewController.class)
class Shop {

    @Bean
    SecurityFilterChain chain(HttpSecurity http) {
        http.addFilterBefore(new NonceFilter(), HeaderWriterFilter.class)
                .headers(headers -> headers.addHeaderWriter((request, response) -> {
                    String nonce = NonceFilter.nonceOf(request);
                    if (nonce != null) {
                        response.setHeader("Content-Security-Policy", ContentSecurityPolicy.strict(nonce));
                    }
                }))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        return http.build();
    }
}
