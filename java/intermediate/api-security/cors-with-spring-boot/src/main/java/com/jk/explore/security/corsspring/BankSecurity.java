package com.jk.explore.security.corsspring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The bank takes the customer's credentials in a header that the browser attaches itself, which is what "credentials" means in CORS (cookies and
 * HTTP authentication both count). Token checks for forged requests are the subject of the CSRF projects, so they are off here.
 * The CORS policy is registered in one of two places, chosen by {@code bank.cors.where}: in Spring Security, before the login check, or in Spring MVC, after it.
 */
@Configuration
class BankSecurity implements WebMvcConfigurer {

    static final String PASSWORD = "lab-password";

    private final CorsSettings cors;

    BankSecurity(CorsSettings cors) {
        this.cors = cors;
    }

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(User.withUsername("ana").password("{noop}" + PASSWORD).roles("CUSTOMER").build());
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a.requestMatchers("/rates").permitAll().anyRequest().authenticated());
        if (cors.inSecurity()) {
            http.cors(c -> c.configurationSource(cors.source()));
        }
        return http.build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (cors.inMvc()) {
            registry.addMapping("/rates").combine(cors.publicData());
            registry.addMapping("/**").combine(cors.accounts());
        }
    }
}
