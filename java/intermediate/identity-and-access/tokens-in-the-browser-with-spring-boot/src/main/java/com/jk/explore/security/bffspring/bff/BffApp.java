package com.jk.explore.security.bffspring.bff;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The bank's backend for the frontend: it signs the customer in with Keycloak, keeps the tokens on the server, and calls the accounts API for the browser. */
@SpringBootApplication
public class BffApp {
}
