package com.jk.explore.security.bffspring.bff;

import com.jk.explore.security.bffspring.OffsetClock;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * The browser gets one session cookie and, for changes, one more cookie its script copies into a header. Everything else, the tokens included, stays here.
 * The tokens are kept in a service the demo can look into, standing for the session store of a real BFF.
 */
@Configuration
class BffSecurity {

    @Bean
    OffsetClock clock() {
        return new OffsetClock();
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClients(ClientRegistrationRepository registrations) {
        return new org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    OAuth2AuthorizedClientManager clientManager(ClientRegistrationRepository registrations, OAuth2AuthorizedClientService service, Clock clock) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().refreshToken(r -> r.clock(clock)).build();
        DefaultOAuth2AuthorizedClientManager manager = new DefaultOAuth2AuthorizedClientManager(registrations, new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(service));
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, ClientRegistrationRepository registrations) throws Exception {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        http.authorizeHttpRequests(a -> a.requestMatchers("/", "/error").permitAll().anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(o -> o.authorizationEndpoint(a -> a.authorizationRequestResolver(resolver)))
                .csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }
}
