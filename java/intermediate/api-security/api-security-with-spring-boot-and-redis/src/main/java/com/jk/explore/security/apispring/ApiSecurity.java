package com.jk.explore.security.apispring;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/** Callers sign in with a header, so there are no cookies to forge and no session; the budget is counted per signed-in caller. */
@Configuration
class ApiSecurity {

    static final String PASSWORD = "lab-password";

    /**
     * Basic authentication checks the password on every request. Spring Security's default encoder is bcrypt, about 70 ms a request, which would make the
     * demo's thousand-request loops take minutes. PBKDF2 with 1,000 iterations is fast enough for a lab; a real API checks credentials once and issues a token.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new Pbkdf2PasswordEncoder("lab-secret", 16, 1000, Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("ana").password(encoder.encode(PASSWORD)).roles("CUSTOMER").build(),
                User.withUsername("ben").password(encoder.encode(PASSWORD)).roles("CUSTOMER").build());
    }

    @Bean
    ManualClock clock() {
        return new ManualClock();
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, ObjectProvider<RateLimiter> limiter, Environment environment) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)   // credentials travel in a header, not a cookie
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a.anyRequest().authenticated());
        if (environment.acceptsProfiles(Profiles.of("safe"))) {
            http.addFilterAfter(new RateLimitFilter(limiter.getObject()), BasicAuthenticationFilter.class);
        }
        return http.build();
    }
}
