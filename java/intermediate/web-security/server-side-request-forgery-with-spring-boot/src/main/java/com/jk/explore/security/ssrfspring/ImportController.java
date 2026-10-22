package com.jk.explore.security.ssrfspring;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** "Import a product image from a web address", as a staff member's form posts it. */
@RestController
class ImportController {

    private final Importer importer;

    ImportController(Importer importer) {
        this.importer = importer;
    }

    @PostMapping("/import")
    ResponseEntity<String> importImage(@RequestParam String url) {
        try {
            return ResponseEntity.ok(importer.importImage(url));
        } catch (ImportRefused e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("refused: " + e.getMessage());
        }
    }
}
