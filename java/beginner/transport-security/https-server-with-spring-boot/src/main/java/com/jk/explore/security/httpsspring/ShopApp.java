package com.jk.explore.security.httpsspring;

import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The online store as a Spring Boot application. TLS itself is configuration (an SSL bundle named by server.ssl.bundle); the code here only
 * adds what configuration cannot: a second, plain HTTP connector whose requests Spring Security redirects to HTTPS, and the HSTS header.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
class ShopApp {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> plainConnector(@Value("${shop.http-port}") int httpPort) {
        return factory -> {
            Connector connector = new Connector();
            connector.setPort(httpPort);
            connector.setProperty("address", "127.0.0.1");
            factory.addAdditionalConnectors(connector);
        };
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, @Value("${shop.http-port}") int httpPort, @Value("${server.port}") int httpsPort) {
        http.portMapper(ports -> ports.http(httpPort).mapsTo(httpsPort))
                .redirectToHttps(Customizer.withDefaults())
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        return http.build();
    }

    @RestController
    static class ShopController {

        @GetMapping("/products")
        String products() {
            return "kettle, toaster, lamp";
        }

        @GetMapping("/login")
        String login(HttpServletRequest request) {
            request.getSession(true);
            return "welcome";
        }

        /** Who the server saw at the other end of the connection, from the client certificate the TLS handshake checked. */
        @GetMapping("/whoami")
        String whoami(HttpServletRequest request) {
            Object chain = request.getAttribute("jakarta.servlet.request.X509Certificate");
            if (chain instanceof X509Certificate[] certificates && certificates.length > 0) {
                return certificates[0].getSubjectX500Principal().getName();
            }
            return "nobody";
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("internal detail: database password is hunter2");
        }
    }
}
