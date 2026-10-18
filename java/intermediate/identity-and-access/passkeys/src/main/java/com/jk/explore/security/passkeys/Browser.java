package com.jk.explore.security.passkeys;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The browser between a web page and the authenticator. Two rules make passkeys resistant to phishing, and the page cannot change
 * either: the relying party id must be the page's own host or a parent domain of it, and the client data records the page's real origin.
 */
final class Browser {

    record Result(Optional<Authenticator.Registration> registration, Optional<Authenticator.Assertion> assertion, String refusal) {

        boolean ok() {
            return refusal == null;
        }
    }

    private final Authenticator authenticator;

    Browser(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    /** The WebAuthn rule: an https origin whose host is the relying party id or ends with "." followed by it. */
    static boolean rpIdAllowedFor(String origin, String rpId) {
        URI uri = URI.create(origin);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String id = rpId.toLowerCase(Locale.ROOT);
        return host.equals(id) || host.endsWith("." + id);
    }

    Result create(String pageOrigin, String rpId, String challenge, boolean userVerified) {
        if (!rpIdAllowedFor(pageOrigin, rpId)) {
            return new Result(Optional.empty(), Optional.empty(), "the browser will not use " + rpId + " for a page at " + pageOrigin);
        }
        byte[] clientData = new ClientData(ClientData.CREATE, challenge, pageOrigin).json();
        return new Result(Optional.of(authenticator.create(rpId, clientData, userVerified)), Optional.empty(), null);
    }

    Result get(String pageOrigin, String rpId, String challenge, List<byte[]> allowed, boolean userVerified) {
        if (!rpIdAllowedFor(pageOrigin, rpId)) {
            return new Result(Optional.empty(), Optional.empty(), "the browser will not use " + rpId + " for a page at " + pageOrigin);
        }
        byte[] clientData = new ClientData(ClientData.GET, challenge, pageOrigin).json();
        Optional<Authenticator.Assertion> assertion = authenticator.get(rpId, allowed, clientData, userVerified);
        return assertion.isPresent()
                ? new Result(Optional.empty(), assertion, null)
                : new Result(Optional.empty(), Optional.empty(), "the authenticator has no passkey for " + rpId);
    }
}
