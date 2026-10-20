package com.jk.explore.security.saml;

/** The shop's side of single sign-on: reads the identity provider's response and decides who the customer is. */
public interface ServiceProvider {

    /** Returns the signed-in user's name, or throws {@link SamlRefused}. */
    String login(String responseXml, String pendingRequestId);
}
