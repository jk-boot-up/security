package com.jk.explore.security.uploadspring;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

/** The store's file routes: invoice downloads, photo uploads, the uploaded files, and the "return to" redirect. */
@RestController
class FilesController {

    private final Downloads downloads;
    private final Uploads uploads;
    private final Redirects redirects;

    FilesController(Downloads downloads, Uploads uploads, Redirects redirects) {
        this.downloads = downloads;
        this.uploads = uploads;
        this.redirects = redirects;
    }

    /** The name arrives as a query parameter, decoded once by Spring, exactly as the customer typed it. */
    @GetMapping("/invoices")
    String invoice(@RequestParam String name) {
        return downloads.read(name);
    }

    /** {@link MultipartFile#getOriginalFilename()} is whatever the client sent, {@code ..} and all. */
    @PostMapping("/uploads")
    String upload(@RequestParam MultipartFile file) throws IOException {
        return uploads.save(file.getOriginalFilename(), file.getBytes());
    }

    @GetMapping("/uploads/{name:.+}")
    ResponseEntity<byte[]> uploaded(@PathVariable String name) {
        Served served = uploads.fetch(name);
        ResponseEntity.BodyBuilder reply = ResponseEntity.ok().contentType(MediaType.parseMediaType(served.contentType()));
        reply.header(HttpHeaders.CONTENT_DISPOSITION, served.attachment() ? "attachment" : "inline");
        if (served.noSniff()) {
            reply.header("X-Content-Type-Options", "nosniff");
        }
        return reply.body(served.body().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
    }

    @GetMapping("/continue")
    ResponseEntity<Void> next(@RequestParam(defaultValue = "") String returnTo) {
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, redirects.target(returnTo)).build();
    }

    @ExceptionHandler(Refused.class)
    ResponseEntity<String> refused(Refused e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("refused: " + e.getMessage());
    }

    /** Spring (and Tomcat) stop an upload above spring.servlet.multipart.max-file-size before the controller sees a byte. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<String> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body("refused: size not allowed");
    }
}
