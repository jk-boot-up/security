package com.jk.explore.security.authkeycloak;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** The online store's order API as an OAuth 2 resource server: it holds no passwords and trusts only tokens the login server signed. */
@Configuration
@EnableAutoConfiguration
@EnableWebSecurity
public class Shop {

    /** Order id to owner. */
    static final Map<String, String> ORDERS = Map.of("A-1", "ana", "B-2", "ben");

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter())))
                .csrf(c -> c.disable())
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults());
        return http.build();
    }

    /** Keycloak puts realm roles in realm_access.roles; Spring wants authorities named ROLE_x. */
    static JwtAuthenticationConverter converter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        Converter<Jwt, Collection<GrantedAuthority>> roles = jwt -> {
            Object access = jwt.getClaim("realm_access");
            if (!(access instanceof Map<?, ?> map) || !(map.get("roles") instanceof List<?> list)) {
                return List.of();
            }
            return list.stream().<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + String.valueOf(r).toUpperCase(Locale.ROOT))).toList();
        };
        converter.setJwtGrantedAuthoritiesConverter(roles);
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    @RestController
    static class Api {

        @GetMapping("/me")
        String me(@AuthenticationPrincipal Jwt jwt, org.springframework.security.core.Authentication who) {
            return jwt.getClaimAsString("preferred_username") + " " + who.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(a -> a.startsWith("ROLE_")).sorted().toList();
        }

        @GetMapping("/orders/{id}")
        String order(@PathVariable String id, @AuthenticationPrincipal Jwt jwt, org.springframework.security.core.Authentication who) {
            String owner = ORDERS.get(id);
            if (owner == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            boolean staff = who.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPPORT") || a.getAuthority().equals("ROLE_ADMIN"));
            if (!staff && !owner.equals(jwt.getClaimAsString("preferred_username"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return id + " owner " + owner;
        }

        @GetMapping("/admin/report")
        String report() {
            return "sales";
        }
    }
}
