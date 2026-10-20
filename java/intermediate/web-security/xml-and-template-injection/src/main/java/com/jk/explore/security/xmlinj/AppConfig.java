package com.jk.explore.security.xmlinj;

/** The application's own settings: what no customer should ever read. The value is a harmless marker. */
public record AppConfig(String smtpPassword, String environment) {

    public static AppConfig demo() {
        return new AppConfig("demo-smtp-password", "production");
    }
}
