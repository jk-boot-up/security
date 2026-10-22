package com.jk.explore.security.sessionredis;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BankController {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;
    private final InMemoryUserDetailsManager users;

    BankController(FindByIndexNameSessionRepository<? extends Session> sessions, InMemoryUserDetailsManager users) {
        this.sessions = sessions;
        this.users = users;
    }

    /** A page for visitors who have not signed in. Touching the session is what gives the visitor an id. */
    @GetMapping("/welcome")
    String welcome(HttpServletRequest request) {
        request.getSession(true).setAttribute("visitor", true);   // an attribute is what makes Spring Session save the session and set the cookie
        return "welcome to the bank";
    }

    @GetMapping("/account")
    String account(Principal who) {
        return who.getName() + "'s balance: 125000 cents";
    }

    /** Ends every session of the signed-in customer, this one included. */
    @PostMapping("/sign-out-everywhere")
    String signOutEverywhere(Principal who) {
        List<String> ids = List.copyOf(sessions.findByPrincipalName(who.getName()).keySet());
        ids.forEach(sessions::deleteById);
        return "ended " + ids.size() + " sessions";
    }

    /** Changes the password, ends the customer's other sessions, and gives this one a new id. */
    @PostMapping("/password")
    ResponseEntity<String> changePassword(Principal who, HttpServletRequest request) {
        UserDetails old = users.loadUserByUsername(who.getName());
        users.updateUser(User.withUserDetails(old).password("{noop}new-" + System.nanoTime()).build());
        String current = request.getSession().getId();
        List<String> others = sessions.findByPrincipalName(who.getName()).keySet().stream().filter(id -> !id.equals(current)).toList();
        others.forEach(sessions::deleteById);
        request.changeSessionId();
        return ResponseEntity.ok("ended " + others.size() + " other sessions");
    }
}
