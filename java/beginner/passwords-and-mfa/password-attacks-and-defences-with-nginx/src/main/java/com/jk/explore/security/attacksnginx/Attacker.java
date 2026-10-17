package com.jk.explore.security.attacksnginx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.IntFunction;

/** Sends login attempts to the nginx login, one after another as fast as it can, and counts the answers. */
public class Attacker {

    private final HttpClient client = HttpClient.newHttpClient();
    private final int port;

    public Attacker(int port) {
        this.port = port;
    }

    /** One attempt; returns the HTTP status. {@code forwardedFor} may be null. */
    public int attempt(String user, String forwardedFor) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/login?user=" + user))
                    .POST(HttpRequest.BodyPublishers.ofString("password=guess"));
            if (forwardedFor != null) {
                request.header("X-Forwarded-For", forwardedFor);
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Makes {@code count} attempts. The functions say which username and which X-Forwarded-For value each one uses. */
    public Tally run(int count, IntFunction<String> user, IntFunction<String> forwardedFor) {
        int answered = 0;
        int refused = 0;
        for (int i = 0; i < count; i++) {
            int status = attempt(user.apply(i), forwardedFor.apply(i));
            if (status == 401) {
                answered++;
            } else if (status == 429) {
                refused++;
            } else {
                throw new IllegalStateException("unexpected status " + status);
            }
        }
        return new Tally(answered, refused);
    }
}
