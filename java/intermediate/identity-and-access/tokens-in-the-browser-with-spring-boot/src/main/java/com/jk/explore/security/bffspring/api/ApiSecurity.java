package com.jk.explore.security.bffspring.api;

import com.jk.explore.security.bffspring.OffsetClock;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The accounts API checks the key, issuer, audience, dates and scope of every token, and, because DPoP is switched on, also accepts a token that is bound to a key
 * when it comes with a proof signed by that key, and refuses such a token without one.
 */
@Configuration
class ApiSecurity {

    @Bean
    OffsetClock clock() {
        return new OffsetClock();
    }

    @Bean
    JwtDecoder decoder(@Value("${bank.issuer}") String issuer, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuer + "/protocol/openid-connect/certs").build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ofSeconds(30));
        timestamps.setClock(clock);
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains("accounts-api"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtIssuerValidator(issuer), audience, timestamps));
        return decoder;
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)   // tokens in a header, no cookies
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/accounts").hasAuthority("SCOPE_accounts.read")
                        .requestMatchers(HttpMethod.POST, "/transfers").hasAuthority("SCOPE_payments.write")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()).dPoP(Customizer.withDefaults()));
        return http.build();
    }
}
