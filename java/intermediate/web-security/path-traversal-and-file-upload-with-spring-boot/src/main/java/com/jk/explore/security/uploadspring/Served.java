package com.jk.explore.security.uploadspring;

/** What the web server sends back for a file: the type it claims, whether the browser must save instead of show, and the body. */
public record Served(String contentType, boolean attachment, boolean noSniff, String body) {
}
