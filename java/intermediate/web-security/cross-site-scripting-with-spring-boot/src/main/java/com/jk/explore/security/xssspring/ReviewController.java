package com.jk.explore.security.xssspring;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The product page, written twice: the way Thymeleaf writes it by default, and with th:utext, the one switch that turns escaping off. */
@Controller
class ReviewController {

    static final String REVIEW = "<b>Great boots</b>";
    static final String FIT_NOTE = "Fits 5\" boots";
    static final String SEARCH = "size 5 & up";
    static final String SCRIPT_NOTE = "It's \"great\"";

    private static void fill(Model model, HttpServletRequest request) {
        request.getSession(true);
        model.addAttribute("review", REVIEW);
        model.addAttribute("fitNote", FIT_NOTE);
        model.addAttribute("search", SEARCH);
        model.addAttribute("scriptNote", SCRIPT_NOTE);
        model.addAttribute("cspNonce", NonceFilter.nonceOf(request));
    }

    @GetMapping("/product")
    String product(Model model, HttpServletRequest request) {
        fill(model, request);
        return "product";
    }

    @GetMapping("/product-unescaped")
    String unescaped(Model model, HttpServletRequest request) {
        fill(model, request);
        return "product-unescaped";
    }
}
