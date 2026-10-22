package com.jk.explore.security.passkeysspring;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import org.springframework.security.web.webauthn.management.MapPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.MapUserCredentialRepository;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

/**
 * The bank signs customers in with a password or a passkey. Spring Security's WebAuthn support provides the endpoints that create and check the challenges and keep
 * the public keys; the bank states its relying party id, its name and the one origin its pages come from.
 */
@Configuration
class PasskeySecurity {

    static final String RP_ID = "bank.example";
    static final String ORIGIN = "https://bank.example";
    static final String PASSWORD = "lab-password";

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("ana").password("{noop}" + PASSWORD).roles("CUSTOMER").build(),
                User.withUsername("ben").password("{noop}" + PASSWORD).roles("CUSTOMER").build());
    }

    @Bean
    MapPublicKeyCredentialUserEntityRepository userEntities() {
        return new MapPublicKeyCredentialUserEntityRepository();
    }

    @Bean
    MapUserCredentialRepository credentials() {
        return new MapUserCredentialRepository();
    }

    /** Spring Security's own relying party, and, in the hardened mode, the same one with user verification required and the counter compared. */
    @Bean
    WebAuthnRelyingPartyOperations relyingParty(MapPublicKeyCredentialUserEntityRepository userEntities, MapUserCredentialRepository credentials, @Value("${bank.mode:default}") String mode) {
        Webauthn4JRelyingPartyOperations spring = new Webauthn4JRelyingPartyOperations(userEntities, credentials,
                PublicKeyCredentialRpEntity.builder().id(RP_ID).name("The Bank").build(), Set.of(ORIGIN));
        if (!mode.equals("hardened")) {
            return spring;
        }
        spring.setCustomizeRequestOptions(options -> options.userVerification(UserVerificationRequirement.REQUIRED));
        return new HardenedRelyingParty(spring, credentials);
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, @Value("${bank.password-sign-in:true}") boolean passwordSignIn, MapPublicKeyCredentialUserEntityRepository userEntities,
            MapUserCredentialRepository credentials) throws Exception {
        http.authorizeHttpRequests(a -> a.requestMatchers("/csrf").permitAll().anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .webAuthn(w -> w.rpName("The Bank").rpId(RP_ID).allowedOrigins(ORIGIN));
        if (passwordSignIn) {
            http.formLogin(f -> f.successHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value())).failureHandler((req, res, e) -> res.setStatus(HttpStatus.UNAUTHORIZED.value())));
            http.addFilterBefore(new PasswordSwitchOffFilter(userEntities, credentials), UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }
}
