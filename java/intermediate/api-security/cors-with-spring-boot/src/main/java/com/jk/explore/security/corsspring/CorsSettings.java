package com.jk.explore.security.corsspring;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The CORS policy for each mode of the lesson, built the way Spring wants it: a {@link CorsConfiguration} per path pattern. {@code /rates} is public data
 * and always gets the wildcard without credentials; everything else gets the policy of the mode.
 */
@Component
class CorsSettings {

    static final String PARTNER = "https://partner.example";

    private final String mode;
    private final String where;

    CorsSettings(@Value("${bank.cors.mode}") String mode, @Value("${bank.cors.where}") String where) {
        this.mode = mode;
        this.where = where;
    }

    boolean enabled() {
        return !mode.equals("none");
    }

    boolean inSecurity() {
        return enabled() && where.equals("security");
    }

    boolean inMvc() {
        return enabled() && where.equals("mvc");
    }

    CorsConfiguration publicData() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of("*"));
        c.setAllowedMethods(List.of("GET"));
        return c;
    }

    CorsConfiguration accounts() {
        CorsConfiguration c = new CorsConfiguration();
        switch (mode) {
            case "star-with-credentials" -> c.setAllowedOrigins(List.of("*"));
            case "reflect" -> c.setAllowedOriginPatterns(List.of("*"));
            case "exact-and-null" -> c.setAllowedOrigins(List.of(PARTNER, "null"));
            case "suffix" -> c.setAllowedOriginPatterns(List.of("https://*bank.example"));
            case "exact" -> c.setAllowedOrigins(List.of(PARTNER));
            default -> throw new IllegalStateException("no policy for mode " + mode);
        }
        c.setAllowedMethods(List.of("GET", "POST", "PUT"));
        c.setAllowedHeaders(List.of("Content-Type"));
        c.setAllowCredentials(true);
        c.setMaxAge(600L);
        return c;
    }

    CorsConfigurationSource source() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/rates", publicData());
        source.registerCorsConfiguration("/**", accounts());
        return source;
    }
}
