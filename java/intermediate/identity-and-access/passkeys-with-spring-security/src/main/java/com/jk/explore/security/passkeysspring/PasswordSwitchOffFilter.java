package com.jk.explore.security.passkeysspring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/** A customer with two passkeys has a way back in if one is lost, so the password, which can be phished, is switched off for them. */
final class PasswordSwitchOffFilter extends OncePerRequestFilter {

    static final int PASSKEYS_NEEDED = 2;

    private final PublicKeyCredentialUserEntityRepository userEntities;
    private final UserCredentialRepository credentials;

    PasswordSwitchOffFilter(PublicKeyCredentialUserEntityRepository userEntities, UserCredentialRepository credentials) {
        this.userEntities = userEntities;
        this.credentials = credentials;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (request.getMethod().equals("POST") && request.getRequestURI().equals("/login") && request.getParameter("username") != null) {
            PublicKeyCredentialUserEntity user = userEntities.findByUsername(request.getParameter("username"));
            if (user != null && credentials.findByUserId(user.getId()).size() >= PASSKEYS_NEEDED) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "password sign-in is switched off for this customer");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
