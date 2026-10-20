package com.jk.explore.security.ssrf;

/** What a server answers: a status, a content type, a redirect target for 3xx, and a body. */
public record Response(int status, String contentType, String location, String body) {

    public static Response ok(String contentType, String body) {
        return new Response(200, contentType, null, body);
    }

    public static Response redirect(String location) {
        return new Response(302, "text/plain", location, "");
    }

    public static Response notFound() {
        return new Response(404, "text/plain", null, "not found");
    }

    public boolean isRedirect() {
        return status >= 300 && status < 400 && location != null;
    }
}
