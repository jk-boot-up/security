package com.jk.explore.security.csrfspring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Two filter chains. The API chain takes credentials in a header and keeps no session, so a browser's cookie means nothing to it and
 * it has no use for tokens. The web chain uses a session cookie, so it needs them, in one of three modes chosen by profile.
 */
@Configuration
class WebSecurity {

    static final String PASSWORD = "lab-password";

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("ana").password("{noop}" + PASSWORD).roles("CUSTOMER").build(),
                User.withUsername("mallory").password("{noop}" + PASSWORD).roles("CUSTOMER").build());
    }

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a.anyRequest().authenticated());
        return http.build();
    }

    /** What many tutorials do to make a POST work: switch the protection off. */
    @Bean
    @Order(2)
    @Profile("csrf-off")
    SecurityFilterChain webWithoutCsrf(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        return pages(http).build();
    }

    /** The default: a token tied to the session, in a hidden form field or a header, compared with the one in the session. */
    @Bean
    @Order(2)
    @Profile("synchronizer")
    SecurityFilterChain webWithSynchronizerToken(HttpSecurity http, @Value("${csrf.ignore:/webhooks/**}") String[] ignored) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers(ignored));
        return pages(http).build();
    }

    /** For a single-page app: the token lives in a cookie the page's script can read, and comes back in a header. */
    @Bean
    @Order(2)
    @Profile("spa")
    SecurityFilterChain webForSinglePageApp(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                .ignoringRequestMatchers("/webhooks/**"));
        return pages(http).build();
    }

    private static HttpSecurity pages(HttpSecurity http) throws Exception {
        return http
                .formLogin(form -> form.defaultSuccessUrl("/balance", true))
                .logout(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/csrf", "/login", "/webhooks/**", "/error").permitAll()
                        .anyRequest().authenticated());
    }
}
