package com.jk.explore.security.oidc;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenID Connect Discovery 1.0. The relying party is configured with one issuer and nothing else. It reads the metadata from
 * issuer + "/.well-known/openid-configuration", insists that the document names exactly that issuer (otherwise one provider could
 * pose as another: a "mix-up"), and loads the signing keys only from the same origin as the issuer.
 */
final class Discovery {

    interface Fetcher {
        String get(String url) throws Exception;
    }

    private Discovery() {
    }

    static String metadataUrl(String issuer) {
        return issuer + "/.well-known/openid-configuration";
    }

    static ProviderConfig discover(String configuredIssuer, Fetcher fetcher) throws InvalidTokenException {
        Map<String, Object> metadata = Json.parseObject(fetch(fetcher, metadataUrl(configuredIssuer)));
        if (!configuredIssuer.equals(metadata.get("issuer"))) {
            throw new InvalidTokenException("the metadata names another issuer");
        }
        if (!(metadata.get("id_token_signing_alg_values_supported") instanceof List<?> algs) || !algs.contains("EdDSA")) {
            throw new InvalidTokenException("the provider does not sign ID tokens with EdDSA");
        }
        String jwksUri = text(metadata, "jwks_uri");
        if (!sameOrigin(configuredIssuer, jwksUri)) {
            throw new InvalidTokenException("the key set is not on the issuer's own site");
        }
        KeySet keys = KeySet.fromJwks(Json.parseObject(fetch(fetcher, jwksUri)));
        return new ProviderConfig(configuredIssuer, text(metadata, "authorization_endpoint"), text(metadata, "token_endpoint"),
                text(metadata, "userinfo_endpoint"), text(metadata, "end_session_endpoint"), keys);
    }

    private static String fetch(Fetcher fetcher, String url) throws InvalidTokenException {
        try {
            return fetcher.get(url);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("the provider could not be reached");
        }
    }

    private static String text(Map<String, Object> metadata, String name) throws InvalidTokenException {
        if (!(metadata.get(name) instanceof String value)) {
            throw new InvalidTokenException("the metadata has no " + name);
        }
        return value;
    }

    static boolean sameOrigin(String a, String b) {
        try {
            URI x = URI.create(a);
            URI y = URI.create(b);
            return Objects.equals(x.getScheme(), y.getScheme()) && Objects.equals(x.getHost(), y.getHost()) && x.getPort() == y.getPort();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
