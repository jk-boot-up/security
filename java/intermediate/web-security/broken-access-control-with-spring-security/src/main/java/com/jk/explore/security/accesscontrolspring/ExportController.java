package com.jk.explore.security.accesscontrolspring;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** A route a developer adds later and forgets to give a rule. Switched on with the "with-export" profile. */
@RestController
@Profile("with-export")
class ExportController {

    @GetMapping("/admin/export")
    String export() {
        return "every customer, every order";
    }
}
