package com.jk.explore.security.samlkeycloak;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ShopController {

    @GetMapping("/")
    String home() {
        return "the shop";
    }

    /** Who is signed in, as the signed statement says. */
    @GetMapping("/me")
    Map<String, Object> me(Saml2AssertionAuthentication authentication) {
        Saml2ResponseAssertionAccessor assertion = authentication.getCredentials();
        Map<String, Object> me = new LinkedHashMap<>();
        me.put("name", assertion.getNameId());
        me.put("email", assertion.getFirstAttribute("email"));
        me.put("registration", authentication.getRelyingPartyRegistrationId());
        me.put("sessionIndexes", assertion.getSessionIndexes().size());
        return me;
    }
}
