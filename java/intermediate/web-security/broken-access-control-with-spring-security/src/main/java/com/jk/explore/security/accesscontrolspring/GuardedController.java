package com.jk.explore.security.accesscontrolspring;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The guarded store's routes. Route rules live in {@link AccessRules}, object rules in {@link OrderAccess}, refund rules on {@link RefundService}. */
@RestController
@Profile("!naive")
class GuardedController {

    private final OrderAccess access;
    private final RefundService refunds;
    private final InMemoryUserDetailsManager users;

    GuardedController(OrderAccess access, RefundService refunds, InMemoryUserDetailsManager users) {
        this.access = access;
        this.refunds = refunds;
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
    ResponseEntity<String> order(@PathVariable String id, Authentication who) {
        return access.readable(id, who).map(o -> ResponseEntity.ok(o.describe())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/orders/by-link/{id}")
    ResponseEntity<String> orderByLink(@PathVariable String id) {
        Order order = access.byLink(id);
        return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order.describe());
    }

    @PostMapping("/orders/{id}/address")
    ResponseEntity<String> changeAddress(@PathVariable String id, @RequestParam String to, Authentication who) {
        return access.changeable(id, who).map(o -> {
            o.moveTo(to);
            return ResponseEntity.ok(o.describe());
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/orders/{id}")
    ResponseEntity<String> cancel(@PathVariable String id, Authentication who) {
        return access.cancellable(id, who).map(o -> ResponseEntity.ok("cancelled " + o.getId()))
                .orElseGet(() -> ResponseEntity.notFound().build());
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
        refunds.refund(order);
        return "refund of " + order + " recorded";
    }

    @GetMapping("/admin/reports")
    String salesReport() {
        return "sales report: 5 orders";
    }
}
