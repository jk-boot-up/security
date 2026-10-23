package com.jk.explore.security.securitychaos;

/** A failure Toxiproxy can inject between the service and a dependency. */
enum Fault {
    DOWN("the proxy is disabled, connections are refused", null),
    LATENCY("every answer is held back 5000 ms", "{\"name\":\"chaos\",\"type\":\"latency\",\"stream\":\"downstream\",\"toxicity\":1.0,\"attributes\":{\"latency\":5000,\"jitter\":0}}"),
    TIMEOUT("the connection is accepted and then nothing is ever sent", "{\"name\":\"chaos\",\"type\":\"timeout\",\"stream\":\"downstream\",\"toxicity\":1.0,\"attributes\":{\"timeout\":0}}"),
    RESET("the connection is reset by the peer", "{\"name\":\"chaos\",\"type\":\"reset_peer\",\"stream\":\"downstream\",\"toxicity\":1.0,\"attributes\":{\"timeout\":0}}"),
    BANDWIDTH("the answer trickles at 1 KB per second", "{\"name\":\"chaos\",\"type\":\"bandwidth\",\"stream\":\"downstream\",\"toxicity\":1.0,\"attributes\":{\"rate\":1}}"),
    SLOW_CLOSE("the connection closes 3000 ms after the answer", "{\"name\":\"chaos\",\"type\":\"slow_close\",\"stream\":\"downstream\",\"toxicity\":1.0,\"attributes\":{\"delay\":3000}}");

    private final String description;
    private final String toxicJson;

    Fault(String description, String toxicJson) {
        this.description = description;
        this.toxicJson = toxicJson;
    }

    String description() {
        return description;
    }

    /** The JSON body of the toxic, or null for DOWN, which disables the proxy instead. */
    String toxicJson() {
        return toxicJson;
    }
}
