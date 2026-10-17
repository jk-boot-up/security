package com.jk.explore.security.xss;

/** A customer's review of a product: the author's name, a short note about fit, and the text. All three are untrusted. */
record Review(String author, String fitNote, String text) {
}
