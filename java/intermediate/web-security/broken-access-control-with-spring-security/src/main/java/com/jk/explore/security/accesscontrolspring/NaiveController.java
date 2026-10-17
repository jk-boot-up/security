package com.jk.explore.security.accesscontrolspring;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The naive store's routes: orders looked up by id alone, the refund service bypassed, and one handler, cancelling an order, written
 * carefully. It is what a store looks like when each handler author decides on their own what to check.
 */
@RestController
@Profile("naive")
class NaiveController {

    private final Orders orders;
    private final InMemoryUserDetailsManager users;

    NaiveController(Orders orders, InMemoryUserDetailsManager users) {
        this.orders = orders;
        this.users = users;
    }

    @GetMapping("/menu")
    String menu() {
        return "laptop, headphones, keyboard, monitor";
    }

    @GetMapping("/account")
    String account(Authentication who) {
        return "links shown: " + Pages.links(who);
    }

    @GetMapping("/orders/{id}")
    ResponseEntity<String> order(@PathVariable String id) {
        return orders.findById(id).map(o -> ResponseEntity.ok(o.describe())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/orders/{id}/address")
    ResponseEntity<String> changeAddress(@PathVariable String id, @RequestParam String to) {
        return orders.findById(id).map(o -> {
            o.moveTo(to);
            return ResponseEntity.ok(o.describe());
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** The one handler written carefully: the owner or an admin. */
    @DeleteMapping("/orders/{id}")
    ResponseEntity<String> cancel(@PathVariable String id, Authentication who) {
        boolean admin = who.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return orders.findById(id).map(o -> {
            if (!admin && !o.getOwner().equals(who.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<String>build();
            }
            orders.cancel(id);
            return ResponseEntity.ok("cancelled " + id);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/users")
    String listUsers() {
        return String.join(", ", Catalogue.ACCOUNTS.stream().map(Catalogue.Account::name).filter(users::userExists).toList());
    }

    @DeleteMapping("/admin/users/{name}")
    String deleteUser(@PathVariable String name) {
        users.deleteUser(name);
        return "deleted " + name;
    }

    @PostMapping("/admin/refunds")
    String refund(@RequestParam String order) {
        orders.refund(order);
        return "refund of " + order + " recorded";
    }

    @GetMapping("/admin/reports")
    String salesReport() {
        return "sales report: 5 orders";
    }
}
