package com.jk.explore.security.sessionredis;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Session ends a session that has been idle too long, but it has no limit on how old a busy session may get. This filter adds one:
 * a session older than the limit is ended, however recently it was used.
 */
final class AbsoluteTimeoutFilter extends OncePerRequestFilter {

    private final Clock clock;
    private final Duration limit;

    AbsoluteTimeoutFilter(Clock clock, Duration limit) {
        this.clock = clock;
        this.limit = limit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && Instant.ofEpochMilli(session.getCreationTime()).plus(limit).isBefore(clock.instant())) {
            session.invalidate();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "session too old, sign in again");
            return;
        }
        chain.doFilter(request, response);
    }
}
