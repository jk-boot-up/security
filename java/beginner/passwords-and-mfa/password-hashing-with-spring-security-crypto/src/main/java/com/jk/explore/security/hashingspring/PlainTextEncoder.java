package com.jk.explore.security.hashingspring;

import org.springframework.security.crypto.password.PasswordEncoder;

/** The worst encoder: it stores the password as it is. Written out here to show the problem; the library no longer recommends this. */
public class PlainTextEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return rawPassword.toString().equals(encodedPassword);
    }
}
