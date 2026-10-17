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

/** The guarded store's filter chain: one rule per route from {@link AccessRules}, and everything else refused. */
@Configuration
@Profile("!naive")
class GuardedSecurity {

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
                // a stateless API with credentials in a header and no cookies; cross-site request forgery has its own project
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(registry -> {
                    // the container's own forward to its error page, after a request was already judged; never a client's request
                    registry.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    AccessRules.apply(registry);
                    registry.anyRequest().denyAll();
                });
        return http.build();
    }
}
