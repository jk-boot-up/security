package com.jk.explore.security.accesscontrolspring;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The naive store's filter chain, with three common mistakes: a sign-in rule where an ownership rule is needed, a pattern that covers
 * {@code /admin/users} but not {@code /admin/users/ben}, and {@code anyRequest().permitAll()} at the end "until it is locked down".
 */
@Configuration
@Profile("naive")
class NaiveSecurity {

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(registry -> registry
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/account", "/orders/**").authenticated()
                        .requestMatchers("/admin/users").hasRole("ADMIN")
                        .anyRequest().permitAll());
        return http.build();
    }
}
