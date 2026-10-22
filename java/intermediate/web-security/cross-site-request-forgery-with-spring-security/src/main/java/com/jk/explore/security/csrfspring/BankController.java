package com.jk.explore.security.csrfspring;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The bank's pages and API: the routes that read, the routes that move money, and one that a mistake left on GET. */
@RestController
class BankController {

    static final String WEBHOOK_SECRET = "lab-webhook-secret";
    private final Bank bank;

    BankController(Bank bank) {
        this.bank = bank;
    }

    @GetMapping("/balance")
    String balance(Principal who) {
        return "balance " + bank.balance(who.getName());
    }

    /** What the bank's own page puts in its form: the token for this session. */
    @GetMapping("/csrf")
    String csrf(CsrfToken token) {
        return token.getToken();
    }

    @PostMapping("/transfer")
    String transfer(Principal who, @RequestParam String to, @RequestParam long amount) {
        return bank.transfer(who.getName(), to, amount);
    }

    /** A mistake: money moves on GET, which Spring Security never checks for a token. */
    @GetMapping("/legacy/move")
    String legacyMove(Principal who, @RequestParam String to, @RequestParam long amount) {
        return bank.transfer(who.getName(), to, amount);
    }

    /** The payment provider calls this without a browser session, so it is exempt from tokens and needs its own proof of origin. */
    @PostMapping("/webhooks/payments")
    ResponseEntity<String> webhook(@RequestHeader(value = "X-Signature", required = false) String signature) {
        if (!WEBHOOK_SECRET.equals(signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad signature");
        }
        return ResponseEntity.ok("payment noted");
    }

    /** The API for apps: credentials in a header, no session, no cookie. */
    @PostMapping("/api/transfer")
    String apiTransfer(Principal who, @RequestParam String to, @RequestParam long amount) {
        return bank.transfer(who.getName(), to, amount);
    }
}
