package com.jk.explore.security.apispring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The second version: a request model with its limits, a response model with only what the caller may see, and a page size that has a ceiling. */
@RestController
@Profile("safe")
class SafeAccountsController {

    private final Bank bank;
    private final int maxPage;

    SafeAccountsController(Bank bank, @Value("${bank.page.max}") int maxPage) {
        this.bank = bank;
        this.maxPage = maxPage;
    }

    @PatchMapping("/accounts/me")
    AccountView update(Principal who, @Valid @RequestBody UpdateProfileRequest request) {
        Account mine = bank.ownedBy(who.getName());
        if (request.displayName() != null) {
            mine.setDisplayName(request.displayName());
        }
        if (request.email() != null) {
            mine.setEmail(request.email());
        }
        return AccountView.of(mine);
    }

    @GetMapping("/accounts/{id}")
    AccountView one(@PathVariable String id) {
        return AccountView.of(bank.account(id));
    }

    @GetMapping("/accounts")
    List<DirectoryEntry> list(@RequestParam(defaultValue = "20") @Min(1) int size) {
        return bank.all().stream().limit(Math.min(size, maxPage)).map(a -> new DirectoryEntry(a.getId(), a.getDisplayName())).toList();
    }
}
