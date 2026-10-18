package com.jk.explore.security.bff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A third-party script included in the banking page, say a compromised analytics library. It is modelled only by what any script on
 * the page is allowed to do: read what scripts can read, send it to its own server, and make requests from the page while it is open.
 */
final class PageScript {

    private final List<String> sentHome = new ArrayList<>();

    /** Reads everything readable on the page and keeps the values that look like credentials. Returns how many it took. */
    int collect(Browser browser) {
        int taken = 0;
        for (Map.Entry<String, String> entry : browser.readableByScripts().entrySet()) {
            if (entry.getKey().contains("token") || entry.getKey().startsWith("cookie ")) {
                sentHome.add(entry.getValue());
                taken++;
            }
        }
        return taken;
    }

    List<String> sentHome() {
        return List.copyOf(sentHome);
    }

    /** A request made from inside the page, with whatever the page itself would send. Possible only while the page is open. */
    Response requestFromPage(Browser browser, Request request) {
        if (!browser.pageOpen()) {
            return Response.of(0, "the script is no longer running");
        }
        return browser.send(Browser.BANK_SITE, request);
    }
}
