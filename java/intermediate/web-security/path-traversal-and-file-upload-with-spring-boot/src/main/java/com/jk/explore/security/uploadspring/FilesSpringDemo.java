package com.jk.explore.security.uploadspring;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Runs the invoice download, photo upload and "return to" story in eight numbered acts against real Spring Boot servers
 * on loopback. Each act prints the HTTP request and the response in separate, labelled sections, and the paths step by
 * step. Ports are never printed and the temporary folder is printed as {@code ...}, so the output is the same on every run.
 */
public class FilesSpringDemo {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};
    static final byte[] HTML_PAGE = "<html><script>steal()</script></html>".getBytes(StandardCharsets.ISO_8859_1);
    static final byte[] SETTINGS = "admin=mallory".getBytes(StandardCharsets.UTF_8);
    /** The fixed shop's multipart limits: Spring refuses a bigger upload before the controller runs. */
    static final String MAX_FILE_SIZE = "2KB";

    /** A shop started on a free loopback port with one set of versions. Closing it stops the server. */
    record Running(ConfigurableApplicationContext context, int port) implements AutoCloseable {

        /** An invoice download as the web form sends it. */
        Wire.Reply invoice(String name) throws IOException, InterruptedException {
            return Wire.get(port, invoicePath(name));
        }

        /** The same download as one line of text: "read: " and the invoice, or the refusal. */
        String download(String name) throws IOException, InterruptedException {
            Wire.Reply reply = invoice(name);
            return reply.status() == 200 ? "read: " + reply.body() : reply.body();
        }

        /** Uploads a file and returns the shop's answer: the address of the stored file, or the refusal. */
        String uploaded(String fileName, byte[] bytes) throws IOException, InterruptedException {
            return upload(fileName, bytes).body();
        }

        Wire.Reply upload(String fileName, byte[] bytes) throws IOException, InterruptedException {
            return Wire.upload(port, fileName, bytes);
        }

        Wire.Reply get(String path) throws IOException, InterruptedException {
            return Wire.get(port, path);
        }

        @Override
        public void close() {
            context.close();
        }
    }

    static Running start(Layout layout, String profile, String... properties) {
        String[] all = new String[properties.length + 4];
        System.arraycopy(properties, 0, all, 0, properties.length);
        all[properties.length] = "server.port=0";
        all[properties.length + 1] = "server.address=127.0.0.1";
        all[properties.length + 2] = "logging.level.root=OFF";
        all[properties.length + 3] = "spring.main.banner-mode=off";
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ShopApp.class)
                .profiles(profile)
                .initializers(c -> c.getBeanFactory().registerSingleton("layout", layout))
                .properties(all)
                .run();
        return new Running(context, Integer.parseInt(context.getEnvironment().getProperty("local.server.port")));
    }

    private final StringBuilder out = new StringBuilder();
    private final Layout layout;
    private final String root;
    private final String realRoot;
    private final Running naive;
    private final Running prefix;
    private final Running safe;

    private FilesSpringDemo(Layout layout, Running naive, Running prefix, Running safe) throws IOException {
        this.layout = layout;
        this.root = layout.root().toString();
        this.realRoot = layout.root().toRealPath().toString();
        this.naive = naive;
        this.prefix = prefix;
        this.safe = safe;
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        try (Layout layout = Layout.create();
                Running naive = start(layout, "naive");
                Running prefix = start(layout, "prefix");
                // the fixed shop limits an upload before the controller sees it: 2 kilobytes of file, 4 of request
                Running safe = start(layout, "safe", "spring.servlet.multipart.max-file-size=" + MAX_FILE_SIZE,
                        "spring.servlet.multipart.max-request-size=4KB")) {
            FilesSpringDemo demo = new FilesSpringDemo(layout, naive, prefix, safe);
            demo.honestDownload();
            demo.dotDot();
            demo.textPrefix();
            demo.layers();
            demo.staticHandler();
            demo.naiveUpload();
            demo.safeUpload();
            demo.returnLinks();
            return demo.out.toString();
        }
    }

    private void honestDownload() throws IOException, InterruptedException {
        act("ONE", "An honest name downloads one invoice.");
        section("THE FOLDERS (in a temporary folder, shown as .../):");
        for (Path dir : List.of(layout.invoices(), layout.invoicesPrivate(), layout.secrets(), layout.config(),
                layout.webroot().resolve("uploads"), layout.uploadsPrivate())) {
            line(String.format("%-18s %s", layout.root().relativize(dir) + "/", filesIn(dir)));
        }
        section("THE REQUEST (the form sends the name):");
        line("GET " + invoicePath("invoice-1001.txt"));
        section("THE PATH (the invoices folder joined with the name):");
        line(shown(layout.invoices().resolve("invoice-1001.txt")));
        section("THE RESPONSE:");
        line(answer(naive.invoice("invoice-1001.txt")));
    }

    private void dotDot() throws IOException, InterruptedException {
        act("TWO", "A name with .. climbs out of the folder.");
        String[] names = {"../secrets/db.txt", "../config/app.properties"};
        String[] marks = {"  <- THE PASSWORD", "  <- THE SETTINGS"};
        for (int i = 0; i < names.length; i++) {
            Path joined = layout.invoices().resolve(names[i]);
            section("THE REQUEST:");
            line("GET " + invoicePath(names[i]));
            section("THE NAME (Spring decodes the parameter once):");
            line(names[i]);
            section("THE PATH THE CONTROLLER OPENS (.. is the folder above):");
            line(shown(joined));
            line("= " + shown(joined.normalize()));
            section("THE RESPONSE:");
            line(answer(naive.invoice(names[i])) + marks[i]);
        }
    }

    private void textPrefix() throws IOException, InterruptedException {
        act("THREE", "A text check is not a path check.");
        Path base = layout.invoices();
        section("THE CHECK (the second version reads only if):");
        line("the path's text starts with \"" + shown(base) + "\"");
        for (String name : new String[] {"../secrets/db.txt", "../invoices-private/payroll.txt"}) {
            Path file = base.resolve(name).normalize();
            section("THE REQUEST:");
            line("GET " + invoicePath(name));
            section("THE PATH (joined, then .. removed):");
            line(shown(file));
            section("THE COMPARISON:");
            line(String.format("as text    starts with \"%s\"?  %s", shown(base), yes(file.toString().startsWith(base.toString()))));
            line(String.format("as a path  \"%s\" is \"%s\"?  %s", file.getName(base.getNameCount() - 1), base.getFileName(),
                    yes(file.startsWith(base))));
            String result = answer(prefix.invoice(name));
            section("THE RESPONSE:");
            line(result + (result.startsWith("200") ? "  <- ANOTHER FOLDER" : ""));
        }
    }

    private void layers() throws IOException, InterruptedException {
        act("FOUR", "The fixed version checks in four layers.");
        section("THE LAYERS (in order; the first that fails refuses):");
        line("1 decode once    by Spring, never again");
        line("2 the shape      invoice-, four digits, .txt");
        line("3 the path       remove .., then compare as a path");
        line("4 the real path  follow links, then compare again");
        section("THE REQUEST:");
        line("GET " + invoicePath("invoice-1001.txt"));
        section("THE RESPONSE:");
        line(answer(safe.invoice("invoice-1001.txt")));
        // the name parameter exactly as it travels; Spring decodes a query parameter once, as URLDecoder does
        String[] sent = {Wire.encode("../secrets/db.txt"), "%2e%2e%2fsecrets%2fdb.txt", "%252e%252e%252fsecrets%252fdb.txt", "invoice-9999.txt"};
        section("THE REQUESTS (GET /invoices?name=..., as sent):");
        for (String raw : sent) {
            line("name=" + raw);
            String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
            if (raw.contains("%2e") || raw.contains("%25")) {
                line("  Spring decodes it once: " + decoded);
            }
        }
        section("THE RESPONSES:");
        for (String raw : sent) {
            line(answer(safe.get("/invoices?name=" + raw)));
        }
        Path link = layout.invoices().resolve("invoice-5555.txt");
        Files.createSymbolicLink(link, layout.secrets().resolve("db.txt"));
        section("THE LINK (made inside the invoices folder):");
        line(shown(link) + " -> " + shown(Files.readSymbolicLink(link)));
        section("THE REQUEST (a good shape, a path inside the folder):");
        line("GET " + invoicePath("invoice-5555.txt"));
        section("THE RESPONSES:");
        line("naive   " + answer(naive.invoice("invoice-5555.txt")));
        line("prefix  " + answer(prefix.invoice("invoice-5555.txt")));
        line("fixed   " + answer(safe.invoice("invoice-5555.txt")));
        section("THE REAL PATH (layer 4 follows the link):");
        line(shown(link.toRealPath()) + "  <- OUTSIDE THE FOLDER");
    }

    private void staticHandler() throws IOException, InterruptedException {
        act("FIVE", "Spring's static handler follows a link.");
        section("THE HANDLER (/static-invoices/** serves the folder):");
        line("addResourceLocations(" + shown(layout.invoices()) + "/)");
        String[] paths = {"/static-invoices/..%2fsecrets/db.txt", "/static-invoices/%2e%2e/secrets/db.txt", "/static-invoices/invoice-5555.txt"};
        section("THE REQUESTS:");
        for (String path : paths) {
            line("GET " + path);
        }
        section("THE RESPONSES:");
        for (String path : paths) {
            Wire.Reply reply = safe.get(path);
            line(reply.status() == 200 ? reply.status() + " " + reply.body() + "  <- FOLLOWS THE LINK" : reply.status() + " (refused)");
        }
    }

    private void naiveUpload() throws IOException, InterruptedException {
        act("SIX", "The naive upload trusts the customer's name.");
        section("THE REQUEST (one part of a multipart POST /uploads):");
        line("Content-Disposition: form-data; name=\"file\";");
        line("    filename=\"../../config/app.properties\"");
        section("THE UPLOADS (getOriginalFilename(), the first bytes):");
        line(upload("cat.png", PNG));
        line(upload("page.html", HTML_PAGE));
        line(upload("../../config/app.properties", SETTINGS));
        section("THE RESPONSES (saved in " + shown(layout.webroot().resolve("uploads")) + "/):");
        line("cat.png");
        line("  -> " + answer(naive.upload("cat.png", PNG)));
        line("page.html");
        Wire.Reply page = naive.get(naive.upload("page.html", HTML_PAGE).body());
        line("  -> served as " + page.contentType().split(";")[0] + ", " + page.disposition() + "  <- OUR PAGE");
        line("../../config/app.properties");
        naive.upload("../../config/app.properties", SETTINGS);
        Path config = layout.config().resolve("app.properties");
        line("  -> " + shown(config) + ": " + Files.readString(config));
        Files.writeString(config, "admin=ana");
    }

    private void safeUpload() throws IOException, InterruptedException {
        act("SEVEN", "The fixed upload decides for itself.");
        section("THE RULES (the customer's name is not used):");
        line("size     Spring: max-file-size=" + MAX_FILE_SIZE);
        line("         the code: 1 to " + SafeUploads.MAX_BYTES + " bytes");
        line("PNG      first bytes 89 50 4e 47");
        line("JPEG     first bytes ff d8 ff");
        line("name     photo-NNNN, counted by the server");
        line("place    " + shown(layout.uploadsPrivate()) + "/, outside the web root");
        record Upload(String label, String name, byte[] bytes) {
        }
        List<Upload> uploads = List.of(new Upload("cat.png", "cat.png", PNG), new Upload("page.html", "page.html", HTML_PAGE),
                new Upload("cat.png (page.html renamed)", "cat.png", HTML_PAGE),
                new Upload("../../config/app.properties", "../../config/app.properties", PNG),
                new Upload("big.png", "big.png", pngOf(1500)), new Upload("huge.png", "huge.png", pngOf(5000)));
        section("THE UPLOADS (name, first bytes, size):");
        for (Upload u : uploads) {
            line(String.format("%-27s %s  %4d bytes", u.label(), firstBytes(u.bytes()), u.bytes().length));
        }
        section("THE RESPONSES:");
        String first = null;
        for (Upload u : uploads) {
            Wire.Reply reply = safe.upload(u.name(), u.bytes());
            first = first == null ? reply.body() : first;
            line(reply.status() == 413 ? "413 refused by Spring, before the controller" : answer(reply));
        }
        Wire.Reply served = safe.get(first);
        section("THE RESPONSE TO GET " + first + ":");
        line("Content-Type: " + served.contentType());
        line("Content-Disposition: " + served.disposition());
        line("X-Content-Type-Options: " + (served.noSniff() ? "nosniff" : "(none)"));
        section("THE SETTINGS AFTER THOSE UPLOADS:");
        Path config = layout.config().resolve("app.properties");
        line(shown(config) + ": " + Files.readString(config));
    }

    private void returnLinks() throws IOException, InterruptedException {
        act("EIGHT", "Return links stay on our own site.");
        String[] links = {"/orders?page=2", "https://evil.example/login", "//evil.example", "/\\evil.example", "javascript:steal()"};
        section("THE REQUESTS (GET /continue?returnTo=...):");
        for (String link : links) {
            line(link);
        }
        section("THE NAIVE RESPONSES (302, Location:):");
        for (String link : links) {
            String location = naive.get(continuePath(link)).location();
            line(String.format("%-27s%s", location, ReturnLink.reason(link) == null ? "" : "  <- NOT OUR SITE"));
        }
        section("THE FIXED RESPONSES (302, Location:, and why):");
        for (String link : links) {
            String why = ReturnLink.reason(link);
            line(safe.get(continuePath(link)).location() + (why == null ? "" : "  (" + why + ")"));
        }
        section("THE HOST CHECK (the host must be shop.example):");
        for (String address : new String[] {"https://shop.example/x", "https://shop.example.evil.example/x", "https://shop.example@evil.example/x"}) {
            URI uri = URI.create(address);
            String user = uri.getUserInfo() == null ? "" : ", user " + uri.getUserInfo();
            line(address);
            line("  host " + uri.getHost() + user + " -> " + (ReturnLink.isOwnHost(address) ? "allowed" : "refused"));
        }
    }

    // ------------------------------------------------------------------ printing

    static String invoicePath(String name) {
        return "/invoices?name=" + Wire.encode(name);
    }

    private static String continuePath(String link) {
        return "/continue?returnTo=" + Wire.encode(link);
    }

    /** The status, then the body; "refused: " is left out, since the status already says so. */
    private static String answer(Wire.Reply reply) {
        return reply.status() + " " + reply.body().replaceFirst("^refused: ", "");
    }

    private void act(String number, String title) {
        out.append(number).append(". ").append(title).append('\n');
    }

    private void section(String header) {
        out.append("  ").append(header).append('\n');
    }

    private void line(String text) {
        out.append(("    " + text).stripTrailing()).append('\n');
    }

    /** A path with the random temporary folder replaced by "...", so the output never changes. */
    private String shown(Path p) {
        return p.toString().replace(realRoot, "...").replace(root, "...");
    }

    private String filesIn(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            List<String> names = files.map(f -> f.getFileName().toString()).sorted().toList();
            return names.isEmpty() ? "(empty)" : String.join("  ", names);
        }
    }

    private static String upload(String name, byte[] bytes) {
        return String.format("%-27s %s", name, firstBytes(bytes));
    }

    /** The first four bytes in hexadecimal, then as characters ("." for a byte that is not a printable character). */
    static String firstBytes(byte[] bytes) {
        byte[] head = Arrays.copyOf(bytes, 4);
        StringBuilder text = new StringBuilder();
        for (byte b : head) {
            text.append(b >= 0x20 && b < 0x7f ? (char) b : '.');
        }
        return HexFormat.ofDelimiter(" ").formatHex(head) + "  " + text;
    }

    private static byte[] pngOf(int size) {
        byte[] bytes = new byte[size];
        System.arraycopy(PNG, 0, bytes, 0, PNG.length);
        return bytes;
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }
}
