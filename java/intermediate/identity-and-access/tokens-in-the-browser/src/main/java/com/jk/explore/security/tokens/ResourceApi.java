package com.jk.explore.security.tokens;

/**
 * The bank API at {@code https://api.bank.example}. A plain token is accepted from whoever presents it (a <em>bearer</em> token). A token bound to a key
 * is accepted only with a fresh proof signed by that key, so copying the token is not enough.
 */
public final class ResourceApi {

    public static final String BASE = "https://api.bank.example";

    private final AuthServer auth;
    private final DpopProof.ProofCheck proofs;

    public ResourceApi(AuthServer auth, SimClock clock) {
        this.auth = auth;
        this.proofs = new DpopProof.ProofCheck(clock);
    }

    /** {@code authorization} is the header value ("Bearer t" or "DPoP t"); {@code dpop} the DPoP header, or null. */
    public ApiReply call(String method, String path, String authorization, String dpop) {
        if (authorization == null || !authorization.contains(" ")) {
            return new ApiReply(401, "no token");
        }
        String scheme = authorization.substring(0, authorization.indexOf(' '));
        String token = authorization.substring(authorization.indexOf(' ') + 1);
        var grant = auth.lookup(token);
        if (grant.isEmpty()) {
            return new ApiReply(401, "unknown token");
        }
        if (grant.get().thumbprint() == null) {
            return scheme.equals("Bearer") ? new ApiReply(200, "balance of " + grant.get().user() + ": 125000 cents") : new ApiReply(401, "wrong scheme");
        }
        if (!scheme.equals("DPoP")) {
            return new ApiReply(401, "this token needs a proof of possession"); // a bound token is never accepted as a bearer token
        }
        try {
            DpopProof.Verified v = proofs.verify(dpop, method, BASE + path, token);
            if (!v.thumbprint().equals(grant.get().thumbprint())) {
                return new ApiReply(401, "proof was signed with another key");
            }
        } catch (DpopProof.Rejected e) {
            return new ApiReply(401, e.getMessage());
        }
        return new ApiReply(200, "balance of " + grant.get().user() + ": 125000 cents");
    }
}
