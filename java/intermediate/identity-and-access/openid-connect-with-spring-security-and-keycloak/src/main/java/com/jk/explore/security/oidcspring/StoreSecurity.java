package com.jk.explore.security.oidcspring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.session.InMemoryOidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * The store signs shoppers in with OpenID Connect: the authorization code flow with PKCE, a state and a nonce that Spring Security keeps in the shopper's session,
 * an ID token that Spring Security checks, and sign-out that reaches the provider (RP-initiated logout) and that the provider can start (back-channel logout).
 * Back-channel logout makes the store post to its own logout address to end the shopper's session, and by default it builds that address from the request, which here
 * names {@code host.docker.internal}, a name the store cannot reach; so the store is told its own address.
 */
@Configuration
class StoreSecurity {

    @Bean
    OidcSessionRegistry oidcSessionRegistry() {
        return new InMemoryOidcSessionRegistry();
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, ClientRegistrationRepository registrations, @Value("${server.port}") int port) throws Exception {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());   // PKCE for confidential clients too, as OAuth 2.1 asks
        OidcClientInitiatedLogoutSuccessHandler logout = new OidcClientInitiatedLogoutSuccessHandler(registrations);
        logout.setPostLogoutRedirectUri("{baseUrl}/");
        http.authorizeHttpRequests(a -> a.requestMatchers("/", "/error").permitAll().anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(o -> o.authorizationEndpoint(a -> a.authorizationRequestResolver(resolver)))
                .logout(l -> l.logoutSuccessHandler(logout))
                .oidcLogout(l -> l.backChannel(b -> b.logoutUri("http://127.0.0.1:" + port + "/logout")));   // see the note on the class
        return http.build();
    }
}
