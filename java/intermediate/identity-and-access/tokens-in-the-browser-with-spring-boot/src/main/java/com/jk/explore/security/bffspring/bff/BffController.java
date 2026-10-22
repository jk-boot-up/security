package com.jk.explore.security.bffspring.bff;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** The calls the page makes. Each one is passed on to the accounts API with the customer's access token, which is refreshed here when it has run out. */
@RestController
class BffController {

    private final OAuth2AuthorizedClientManager clients;
    private final RestClient api;

    BffController(OAuth2AuthorizedClientManager clients, @Value("${bff.api-url}") String apiUrl) {
        this.clients = clients;
        this.api = RestClient.builder().baseUrl(apiUrl).build();
    }

    @GetMapping("/")
    String home() {
        return "the bank's web app";
    }

    @GetMapping("/api/accounts")
    ResponseEntity<String> accounts(Authentication who, HttpServletRequest request, HttpServletResponse response) {
        return relay("GET", "/accounts", who, request, response);
    }

    @PostMapping("/api/transfers")
    ResponseEntity<String> transfer(Authentication who, HttpServletRequest request, HttpServletResponse response) {
        return relay("POST", "/transfers", who, request, response);
    }

    private ResponseEntity<String> relay(String method, String path, Authentication who, HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizedClient client = clients.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("keycloak").principal(who)
                .attribute(HttpServletRequest.class.getName(), request).attribute(HttpServletResponse.class.getName(), response).build());
        if (client == null) {
            return ResponseEntity.status(401).body("no tokens for this session");
        }
        try {
            RestClient.RequestBodySpec spec = api.method(org.springframework.http.HttpMethod.valueOf(method)).uri(path).headers(h -> h.setBearerAuth(client.getAccessToken().getTokenValue()));
            ResponseEntity<String> answer = spec.retrieve().toEntity(String.class);
            return ResponseEntity.status(answer.getStatusCode()).body(answer.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
