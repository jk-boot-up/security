package com.jk.explore.security.xssspring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request a fresh nonce, before Spring Security writes its headers. The policy header and the page's own script
 * both read it from the request, so they always agree, and no two pages share one.
 */
final class NonceFilter extends OncePerRequestFilter {

    static final String ATTRIBUTE = "cspNonce";

    static String nonceOf(HttpServletRequest request) {
        return (String) request.getAttribute(ATTRIBUTE);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        request.setAttribute(ATTRIBUTE, ContentSecurityPolicy.newNonce());
        chain.doFilter(request, response);
    }
}
