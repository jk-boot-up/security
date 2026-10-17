package com.jk.explore.security.authspring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/** What every version of the shop shares: the controller, the ownership rules and the four accounts. */
@Configuration
public class CommonConfig {

    @Bean
    public ShopController shopController() {
        return new ShopController();
    }

    @Bean
    public OrderRules orders() {
        return new OrderRules();
    }

    @Bean
    public CountingPasswordEncoder passwordEncoder() {
        return new CountingPasswordEncoder();
    }

    @Bean
    public UserDetailsService users(CountingPasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("ana").password(encoder.encode("ana-pass")).roles("CUSTOMER").build(),
                User.withUsername("ben").password(encoder.encode("ben-pass")).roles("CUSTOMER").build(),
                User.withUsername("sam").password(encoder.encode("sam-pass")).roles("SUPPORT").build(),
                User.withUsername("alex").password(encoder.encode("alex-pass")).roles("ADMIN").build());
    }
}
