package com.jk.explore.security.oauth;

import java.util.Map;

/** The bank's token endpoint as the client sees it: in-process for most acts, over a real local HTTP server in act one. */
@FunctionalInterface
interface TokenEndpoint {

    AuthorizationServer.Tokens call(Map<String, String> form) throws OAuthException;
}
