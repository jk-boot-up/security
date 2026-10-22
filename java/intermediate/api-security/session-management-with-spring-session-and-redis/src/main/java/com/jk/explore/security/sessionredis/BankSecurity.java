package com.jk.explore.security.sessionredis;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Form login with the session in Redis, answers a program can read (200 or 401, never a redirect), and the session rules of the lesson: a new id at sign-in,
 * a limit on concurrent sessions, an absolute timeout, and a cookie with every protection.
 */
@Configuration
class BankSecurity {

    static final String PASSWORD = "lab-password";

    @Bean
    InMemoryUserDetailsManager users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("ana").password("{noop}" + PASSWORD).roles("CUSTOMER").build(),
                User.withUsername("ben").password("{noop}" + PASSWORD).roles("CUSTOMER").build());
    }

    @Bean
    OffsetClock clock() {
        return new OffsetClock();
    }

    @Bean
    DefaultCookieSerializer cookieSerializer() {
        DefaultCookieSerializer cookie = new DefaultCookieSerializer();
        cookie.setCookieName("__Host-session");
        cookie.setCookiePath("/");
        cookie.setUseSecureCookie(true);
        cookie.setUseHttpOnlyCookie(true);
        cookie.setSameSite("Strict");
        return cookie;
    }

    @Bean
    SpringSessionBackedSessionRegistry<? extends Session> sessionRegistry(FindByIndexNameSessionRepository<? extends Session> sessions) {
        return new SpringSessionBackedSessionRegistry<>(sessions);
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, SpringSessionBackedSessionRegistry<? extends Session> registry, Clock clock,
            @Value("${bank.absolute-timeout}") Duration absolute, @Value("${bank.fixation}") String fixation, @Value("${bank.max-sessions}") int maxSessions,
            @Value("${bank.save-requests}") boolean saveRequests)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)   // forged requests are the subject of the CSRF projects
                // Spring Security's default remembers the page a stranger asked for in a new session, which is a record in Redis for every unauthenticated request.
                .requestCache(c -> {
                    if (!saveRequests) {
                        c.disable();
                    }
                })
                .authorizeHttpRequests(a -> a.requestMatchers("/login", "/welcome").permitAll().anyRequest().authenticated())
                .formLogin(f -> f.successHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value()))
                        .failureHandler((req, res, e) -> res.setStatus(HttpStatus.UNAUTHORIZED.value())))
                .logout(l -> l.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .sessionManagement(s -> {
                    if (fixation.equals("none")) {
                        s.sessionFixation(f -> f.none());
                    } else {
                        s.sessionFixation(f -> f.changeSessionId());
                    }
                    if (maxSessions > 0) {
                        s.sessionConcurrency(c -> c.maximumSessions(maxSessions).sessionRegistry(registry)
                                .expiredSessionStrategy(event -> event.getResponse().sendError(HttpStatus.UNAUTHORIZED.value(), "session ended by a newer sign-in")));
                    }
                })
                .addFilterAfter(new AbsoluteTimeoutFilter(clock, absolute), BasicAuthenticationFilter.class);
        return http.build();
    }
}
