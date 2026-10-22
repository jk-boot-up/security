package com.jk.explore.security.samlkeycloak;

import java.time.Duration;
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * SAML 2.0 login: {@code /saml2/authenticate/keycloak} sends the shopper to Keycloak with an authentication request, and {@code /login/saml2/sso/keycloak} receives the
 * response. Everything else needs a signed-in shopper and answers 401 rather than a redirect, so a program can read the result.
 */
@Configuration
class ShopSecurity {

    @Bean
    SecurityFilterChain chain(HttpSecurity http, Refusals refusals, @Value("${shop.clock-skew-seconds:1}") long skewSeconds) throws Exception {
        OpenSaml5AuthenticationProvider provider = new OpenSaml5AuthenticationProvider();
        // Spring Security's default accepts an assertion up to five minutes past its end; the shop tightens that.
        OpenSaml5AuthenticationProvider.AssertionValidator validator = OpenSaml5AuthenticationProvider.AssertionValidator.withDefaults();
        validator.setValidationContextParameters(parameters -> parameters.put(SAML2AssertionValidationParameters.CLOCK_SKEW, Duration.ofSeconds(skewSeconds)));
        provider.setAssertionValidator(validator);
        http.authorizeHttpRequests(a -> a.requestMatchers("/", "/error").permitAll().anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .saml2Login(l -> l.authenticationManager(new ProviderManager(provider)).failureHandler((request, response, exception) -> {
                    String code = exception instanceof Saml2AuthenticationException s ? s.getSaml2Error().getErrorCode() : exception.getClass().getSimpleName();
                    refusals.add(code);
                    response.sendError(HttpStatus.UNAUTHORIZED.value());
                }));
        return http.build();
    }
}
