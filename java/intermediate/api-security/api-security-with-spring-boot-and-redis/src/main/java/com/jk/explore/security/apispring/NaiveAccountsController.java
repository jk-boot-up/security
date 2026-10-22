package com.jk.explore.security.apispring;

import java.security.Principal;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The first version of the API: bind the body to the stored record, return the stored record, and let the caller choose the page size. */
@RestController
@Profile("naive")
class NaiveAccountsController {

    private final Bank bank;

    NaiveAccountsController(Bank bank) {
        this.bank = bank;
    }

    /** Copies every field that the caller sent onto the stored record. */
    @PatchMapping("/accounts/me")
    Account update(Principal who, @RequestBody Account patch) {
        Account mine = bank.ownedBy(who.getName());
        if (patch.getDisplayName() != null) {
            mine.setDisplayName(patch.getDisplayName());
        }
        if (patch.getEmail() != null) {
            mine.setEmail(patch.getEmail());
        }
        if (patch.getRole() != null && !patch.getRole().equals("customer")) {
            mine.setRole(patch.getRole());
        }
        if (patch.getCreditLimitCents() != 0) {
            mine.setCreditLimitCents(patch.getCreditLimitCents());
        }
        if (patch.getRiskScore() != 0) {
            mine.setRiskScore(patch.getRiskScore());
        }
        return mine;
    }

    @GetMapping("/accounts/{id}")
    Account one(@PathVariable String id) {
        return bank.account(id);
    }

    @GetMapping("/accounts")
    List<Account> list(@RequestParam(defaultValue = "20") int size) {
        return bank.all().stream().limit(size).toList();
    }
}
