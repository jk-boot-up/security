package com.jk.explore.security.httpspring;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The store's three endpoints: a page, a cart that starts a session, and a search that puts the customer's text in a header. */
@RestController
class ShopController {

    @GetMapping("/products")
    String products() {
        return "shoes";
    }

    @GetMapping("/cart")
    String cart(HttpSession session) {
        session.setAttribute("cart", "A-1001");
        return "cart";
    }

    @GetMapping("/echo")
    String echo(@RequestParam("name") String name, HttpServletResponse response) {
        response.setHeader("X-Echo", name);
        return "ok";
    }
}
