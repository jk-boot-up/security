package com.jk.explore.security.oidcspring;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The store's customers. Ana already has an account, created when she first signed in with the bank, and the store knows her e-mail address. */
@Component
class StoreAccounts {

    private final Map<String, String> bySubject = new LinkedHashMap<>();
    private final Map<String, String> byEmail = new LinkedHashMap<>();

    StoreAccounts(@Value("${store.bank-issuer}") String bankIssuer) {
        bySubject.put(bankIssuer + "|" + AccountsKeys.ANA, "account of Ana");
        byEmail.put("ana@mail.example", "account of Ana");
    }

    /** An account keyed by e-mail address: whoever shows the address gets the account. */
    synchronized String byEmail(String email) {
        return byEmail.computeIfAbsent(email, e -> "new account for " + e);
    }

    /** An account keyed by issuer and subject, which is what identifies a person at a provider. */
    synchronized String bySubject(String issuer, String subject) {
        return bySubject.computeIfAbsent(issuer + "|" + subject, k -> "new account for " + k.replaceAll("^https?://[^|]*/realms/", "").replace("|", " | "));
    }
}
