package com.jk.explore.security.xmlinj;

/** Turns a template and some data into an e-mail body. */
public interface TemplateEngine {

    String render(String template, Customer customer);
}
