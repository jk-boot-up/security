package com.jk.explore.security.webhooks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The endpoint on a real loopback server: only POST, a size limit, and the same checks as the receiver. */
class ServerTest {

    private final SimClock clock = new SimClock(5000);
    private final byte[] secret = "srv".getBytes(StandardCharsets.UTF_8);

    @Test
    void signedEventsPassAndEverythingElseIsRefused() throws Exception {
        Orders orders = new Orders().add("A-1", 10);
        try (WebhookServer server = new WebhookServer(new Receiver(Receiver.Policy.complete(), clock, orders, secret));
                HttpClient client = HttpClient.newHttpClient()) {
            Provider.Delivery d = new Provider(clock, secret).send(new Event("evt_1", Event.PAID, "A-1", 10));
            assertEquals(200, WebhookDemo.post(server.endpoint(), d.body(), d.signature()).status());
            assertEquals(400, WebhookDemo.post(server.endpoint(), d.body(), null).status());
            HttpResponse<String> get = client.send(HttpRequest.newBuilder(server.endpoint()).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(405, get.statusCode());
            byte[] big = new byte[WebhookServer.MAX_BODY + 1];
            assertEquals(413, WebhookDemo.post(server.endpoint(), big, d.signature()).status());
            assertEquals(Orders.State.PAID, orders.state("A-1"));
        }
    }
}
