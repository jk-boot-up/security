package com.jk.explore.security.corsspring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** The bank's API. {@code /account} and {@code /transfer} need a login, {@code /rates} is public. Every call is recorded before it does anything. */
@RestController
class BankController {

    private final Processed processed;

    BankController(Processed processed) {
        this.processed = processed;
    }

    @GetMapping("/account")
    String account() {
        processed.add("GET /account");
        return "balance: 125000 cents";
    }

    /** A simple request for a browser: no custom header and a form or text body, so no preflight. */
    @PostMapping("/transfer")
    String transferSimple() {
        processed.add("POST /transfer");
        return "sent 500 to mallory";
    }

    /** A request with a JSON body: the browser asks first. */
    @PutMapping("/transfer")
    String transferJson() {
        processed.add("PUT /transfer");
        return "sent 500 to mallory";
    }

    @GetMapping("/rates")
    String rates() {
        processed.add("GET /rates");
        return "rates: 4.1%";
    }
}
