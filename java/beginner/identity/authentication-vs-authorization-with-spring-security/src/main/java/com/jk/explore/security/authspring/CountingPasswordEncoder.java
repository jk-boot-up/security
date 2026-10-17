package com.jk.explore.security.authspring;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt, which is slow on purpose, with a counter of how many passwords were checked. The cost is
 * the lowest BCrypt allows (4), so the demo is quick; a real system uses 10 or more.
 */
public class CountingPasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
    private final AtomicInteger checks = new AtomicInteger();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        checks.incrementAndGet();
        return bcrypt.matches(rawPassword, encodedPassword);
    }

    public int checks() {
        return checks.get();
    }
}
