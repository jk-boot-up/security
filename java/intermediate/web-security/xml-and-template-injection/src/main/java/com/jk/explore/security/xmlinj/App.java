package com.jk.explore.security.xmlinj;

/** The running application, which a careless template engine leaves reachable from every template. */
public record App(AppConfig config, String version) {

    public static App demo() {
        return new App(AppConfig.demo(), "3.4.1");
    }
}
