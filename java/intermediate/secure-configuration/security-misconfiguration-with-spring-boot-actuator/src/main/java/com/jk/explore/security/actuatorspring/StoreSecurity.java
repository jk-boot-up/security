package com.jk.explore.security.actuatorspring;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** Two filter chains: one that lets everything through, as in a store where nobody wrote rules for the actuator, and one with rules. */
@Configuration
class StoreSecurity {

    static final String PASSWORD = "lab-password";

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("ops").password("{noop}" + PASSWORD).roles("OPS").build(),
                User.withUsername("ana").password("{noop}" + PASSWORD).roles("CUSTOMER").build());
    }

    @Bean
    @Profile("!hardened")
    SecurityFilterChain open(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a.anyRequest().permitAll()).csrf(c -> c.disable()).build();
    }

    @Bean
    @Profile("hardened")
    SecurityFilterChain rules(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("OPS")
                        .requestMatchers("/products", "/orders/**").permitAll()
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                // Operators call the actuator with a header, never from a browser page, so it has no use for form tokens.
                .csrf(c -> c.ignoringRequestMatchers(EndpointRequest.toAnyEndpoint()))
                .build();
    }
}
