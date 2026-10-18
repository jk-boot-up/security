package com.jk.explore.security.oidc;

/** What the relying party knows about its provider after discovery: the exact issuer, its endpoints and its signing keys. */
record ProviderConfig(String issuer, String authorizationEndpoint, String tokenEndpoint, String userinfoEndpoint,
                      String endSessionEndpoint, KeySet keys) {
}
