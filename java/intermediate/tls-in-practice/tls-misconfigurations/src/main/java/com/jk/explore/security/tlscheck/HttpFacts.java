package com.jk.explore.security.tlscheck;

/** What the HTTPS site says about itself on top of TLS. {@code hstsMaxAgeSeconds} is -1 when the header is missing. */
public record HttpFacts(long hstsMaxAgeSeconds, boolean hstsIncludeSubDomains, boolean httpRedirectsToHttps, boolean sessionCookieSecure) {
}
