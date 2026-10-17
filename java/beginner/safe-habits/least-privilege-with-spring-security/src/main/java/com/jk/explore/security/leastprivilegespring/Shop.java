package com.jk.explore.security.leastprivilegespring;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.web.SecurityFilterChain;

/** The online store with least privilege: scoped authorities, deny by default, a role hierarchy, method security and time-limited grants. */
@Configuration
@EnableAutoConfiguration
@EnableMethodSecurity
@ComponentScan(basePackageClasses = Shop.class)
public class Shop {

    @Bean
    SimulatedClock clock() {
        return new SimulatedClock();
    }

    @Bean
    Grants grants(SimulatedClock clock) {
        return new Grants(clock);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        List<UserDetails> users = new ArrayList<>();
        for (Catalogue.Account account : Catalogue.ACCOUNTS.values()) {
            List<String> authorities = new ArrayList<>();
            authorities.add("ROLE_" + account.role());
            authorities.addAll(account.permissions());
            users.add(new org.springframework.security.core.userdetails.User(account.name(), encoder.encode(Catalogue.PASSWORD),
                    AuthorityUtils.createAuthorityList(authorities.toArray(String[]::new))));
        }
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /** Admin includes the other jobs, so a rule for "support" lets an admin in without listing every role. */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("FINANCE")
                .role("ADMIN").implies("SUPPORT")
                .role("ADMIN").implies("WAREHOUSE")
                .build();
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, Grants grants) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/orders").hasAuthority("orders:read")
                        .requestMatchers(HttpMethod.GET, "/reports").hasAuthority("reports:read")
                        .requestMatchers(HttpMethod.GET, "/secrets").hasAuthority("secrets:read")
                        .requestMatchers(HttpMethod.GET, "/staff/**").hasRole("SUPPORT")
                        .requestMatchers(HttpMethod.PUT, "/payments/**").access(new GrantAuthorization("payments:write", grants))
                        .requestMatchers("/refunds", "/refunds/**").authenticated()
                        .anyRequest().denyAll());
        return http.build();
    }
}
