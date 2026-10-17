package com.jk.explore.security.xss;

import java.util.List;

/**
 * The product page of the online store, with its reviews. The unsafe version pastes review text into the page; the safe
 * version encodes each value for the place it goes, and gives the shop's own script the response's nonce.
 */
final class ReviewPage {

    static final String PRODUCT = "Trail boots";

    private ReviewPage() {
    }

    static String unsafe(List<Review> reviews) {
        StringBuilder html = new StringBuilder("<html><body><h1>" + PRODUCT + "</h1>");
        for (Review r : reviews) {
            html.append("<div class=\"review\" data-fit=\"").append(r.fitNote()).append("\"><p>")
                    .append(r.text()).append("</p><span>").append(r.author()).append("</span></div>");
        }
        return html.append("</body></html>").toString();
    }

    static String safe(List<Review> reviews, String nonce) {
        StringBuilder html = new StringBuilder("<html><body><h1>" + PRODUCT + "</h1>");
        for (Review r : reviews) {
            html.append("<div class=\"review\" data-fit=\"").append(Encode.attribute(r.fitNote())).append("\"><p>")
                    .append(Encode.htmlBody(r.text())).append("</p><span>").append(Encode.htmlBody(r.author())).append("</span>")
                    .append("<a href=\"/reviews?author=").append(Encode.urlParameter(r.author())).append("\">more</a></div>");
        }
        String first = reviews.isEmpty() ? "" : reviews.get(0).author();
        html.append("<script nonce=\"").append(nonce).append("\">var firstReviewer = \"")
                .append(Encode.javaScriptString(first)).append("\";</script>");
        return html.append("</body></html>").toString();
    }
}
