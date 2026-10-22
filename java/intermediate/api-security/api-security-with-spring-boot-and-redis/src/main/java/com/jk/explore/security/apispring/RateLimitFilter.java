package com.jk.explore.security.apispring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Takes one token from the signed-in caller's bucket before the request reaches the controller. An empty bucket gets 429 with the seconds to wait.
 * If the limiter itself cannot answer, the request is refused with 503: an API that cannot count does not guess.
 */
final class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;

    RateLimitFilter(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (who == null || !who.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }
        RateLimiter.Verdict verdict;
        try {
            verdict = limiter.take(who.getName());
        } catch (RuntimeException e) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        if (!verdict.allowed()) {
            response.setHeader("Retry-After", Long.toString(Math.max(1, (verdict.retryAfterMillis() + 999) / 1000)));
            response.sendError(429);
            return;
        }
        chain.doFilter(request, response);
    }
}
