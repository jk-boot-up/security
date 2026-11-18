package com.jk.explore.security.files;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Runs the invoice download, photo upload and "return to" story in seven numbered acts. Each act prints the request and
 * the response in separate, labelled sections, and the paths step by step. The temporary folder is printed as {@code ...},
 * so the output is the same on every run.
 */
public class FilesDemo {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};
    static final byte[] HTML_PAGE = "<html><script>steal()</script></html>".getBytes(StandardCharsets.ISO_8859_1);
    static final byte[] SETTINGS = "admin=mallory".getBytes(StandardCharsets.UTF_8);
    static final byte[] OVERSIZED = new byte[5000];

    private final StringBuilder out = new StringBuilder();
    private final Layout layout;
    private final String root;
    private final String realRoot;

    private FilesDemo(Layout layout) throws IOException {
        this.layout = layout;
        this.root = layout.root().toString();
        this.realRoot = layout.root().toRealPath().toString();
    }

    public static void main(String[] args) throws IOException {
        System.out.print(run());
    }

    public static String run() throws IOException {
        try (Layout layout = Layout.create()) {
            FilesDemo demo = new FilesDemo(layout);
            demo.honestDownload();
            demo.dotDot();
            demo.textPrefix();
            demo.layers();
            demo.naiveUpload();
            demo.safeUpload();
            demo.returnLinks();
            return demo.out.toString();
        }
    }

    private void honestDownload() throws IOException {
        act("ONE", "An honest name downloads one invoice.");
        section("THE FOLDERS (in a temporary folder, shown as .../):");
        for (Path dir : List.of(layout.invoices(), layout.invoicesPrivate(), layout.secrets(), layout.config(),
                layout.webroot().resolve("uploads"), layout.uploadsPrivate())) {
            line(String.format("%-18s %s", layout.root().relativize(dir) + "/", filesIn(dir)));
        }
        section("THE REQUEST (the name the customer sends):");
        line("invoice-1001.txt");
        section("THE PATH (the invoices folder joined with the name):");
        line(shown(layout.invoices().resolve("invoice-1001.txt")));
        section("THE RESPONSE:");
        line(attempt(() -> "read: " + new NaiveDownloads(layout).read("invoice-1001.txt")));
    }

    private void dotDot() {
        act("TWO", "A name with .. climbs out of the folder.");
        Downloads naive = new NaiveDownloads(layout);
        String[] marks = {"  <- THE PASSWORD", "  <- THE SETTINGS"};
        String[] names = {"../secrets/db.txt", "../config/app.properties"};
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            Path joined = layout.invoices().resolve(name);
            section("THE REQUEST:");
            line(name);
            section("THE PATH, JOINED:");
            line(shown(joined));
            section("THE PATH THE DISK OPENS (.. is the folder above):");
            line(shown(joined.normalize()));
            section("THE RESPONSE:");
            line(attempt(() -> "read: " + naive.read(name)) + marks[i]);
        }
    }

    private void textPrefix() {
        act("THREE", "A text check is not a path check.");
        Downloads prefix = new PrefixDownloads(layout);
        Path base = layout.invoices();
        section("THE CHECK (the second version reads only if):");
        line("the path's text starts with \"" + shown(base) + "\"");
        String[] names = {"../secrets/db.txt", "../invoices-private/payroll.txt"};
        for (String name : names) {
            Path file = base.resolve(name).normalize();
            boolean text = file.toString().startsWith(base.toString());
            section("THE REQUEST:");
            line(name);
            section("THE PATH (joined, then .. removed):");
            line(shown(file));
            section("THE COMPARISON:");
            line(String.format("as text    starts with \"%s\"?  %s", shown(base), yes(text)));
            line(String.format("as a path  \"%s\" is \"%s\"?  %s", file.getName(base.getNameCount() - 1), base.getFileName(),
                    yes(file.startsWith(base))));
            String result = attempt(() -> "read: " + prefix.read(name));
            section("THE RESPONSE:");
            line(result + (result.startsWith("read") ? "  <- ANOTHER FOLDER" : ""));
        }
    }

    private void layers() throws IOException {
        act("FOUR", "The fixed version checks a name in four layers.");
        Downloads safe = new SafeDownloads(layout);
        section("THE LAYERS (in order; the first that fails refuses):");
        line("1 decode once    %2e%2e%2f becomes " + SafeDownloads.decodeOnce("%2e%2e%2f"));
        line("2 the shape      invoice-, four digits, .txt");
        line("3 the path       remove .., then compare as a path");
        line("4 the real path  follow links, then compare again");
        section("THE REQUEST:");
        line("invoice-1001.txt");
        section("THE RESPONSE:");
        line(attempt(() -> "read: " + safe.read("invoice-1001.txt")));
        String[] names = {"../secrets/db.txt", "%2e%2e%2fsecrets%2fdb.txt", "../invoices-private/payroll.txt", "invoice-9999.txt"};
        section("THE REQUESTS (four names that are not an invoice):");
        for (String name : names) {
            String decoded = SafeDownloads.decodeOnce(name);
            line(name);
            if (!decoded.equals(name)) {
                line("  decoded once: " + decoded);
            }
        }
        section("THE RESPONSES:");
        for (String name : names) {
            line(attempt(() -> "read: " + safe.read(name)));
        }
        Path link = layout.invoices().resolve("invoice-5555.txt");
        Files.createSymbolicLink(link, layout.secrets().resolve("db.txt"));
        section("THE LINK (made inside the invoices folder):");
        line(shown(link) + " -> " + shown(Files.readSymbolicLink(link)));
        section("THE REQUEST (a good shape, a path inside the folder):");
        line("invoice-5555.txt");
        section("THE RESPONSES:");
        line("naive   " + attempt(() -> "read: " + new NaiveDownloads(layout).read("invoice-5555.txt")));
        line("prefix  " + attempt(() -> "read: " + new PrefixDownloads(layout).read("invoice-5555.txt")));
        line("fixed   " + attempt(() -> "read: " + safe.read("invoice-5555.txt")));
        section("THE REAL PATH (layer 4 follows the link):");
        line(shown(link.toRealPath()) + "  <- OUTSIDE THE FOLDER");
    }

    private void naiveUpload() throws IOException {
        act("FIVE", "The naive upload trusts the customer's name.");
        Uploads naive = new NaiveUploads(layout);
        section("THE UPLOADS (the customer's name, the first bytes):");
        line(upload("cat.png", PNG));
        line(upload("page.html", HTML_PAGE));
        line(upload("../../config/app.properties", SETTINGS));
        section("THE RESPONSES (saved in " + shown(layout.webroot().resolve("uploads")) + "/):");
        line("cat.png");
        line("  -> " + naive.save("cat.png", PNG));
        line("page.html");
        Served page = new WebRoot(layout.webroot()).serve(naive.save("page.html", HTML_PAGE));
        line("  -> served as " + page.contentType() + ", inline  <- OUR PAGE");
        line("../../config/app.properties");
        naive.save("../../config/app.properties", SETTINGS);
        Path config = layout.config().resolve("app.properties");
        line("  -> " + shown(config) + ": " + Files.readString(config));
        Files.writeString(config, "admin=ana");
    }

    private void safeUpload() throws IOException {
        act("SIX", "The fixed upload decides for itself.");
        SafeUploads safe = new SafeUploads(layout);
        section("THE RULES (the customer's name is not used):");
        line("size     1 to " + SafeUploads.MAX_BYTES + " bytes");
        line("PNG      first bytes 89 50 4e 47");
        line("JPEG     first bytes ff d8 ff");
        line("name     photo-NNNN, counted by the server");
        line("place    " + shown(layout.uploadsPrivate()) + "/, outside the web root");
        record Upload(String label, String name, byte[] bytes) {
        }
        List<Upload> uploads = List.of(new Upload("cat.png", "cat.png", PNG), new Upload("page.html", "page.html", HTML_PAGE),
                new Upload("cat.png (page.html renamed)", "cat.png", HTML_PAGE),
                new Upload("../../config/app.properties", "../../config/app.properties", PNG),
                new Upload("big.png", "big.png", OVERSIZED));
        section("THE UPLOADS (name, first bytes, size):");
        for (Upload u : uploads) {
            line(String.format("%-27s %s  %4d bytes", u.label(), firstBytes(u.bytes()), u.bytes().length));
        }
        section("THE RESPONSES:");
        String first = null;
        for (Upload u : uploads) {
            String result = attempt(() -> safe.save(u.name(), u.bytes()));
            first = first == null ? result : first;
            line(result);
        }
        String id = first.substring("/photos/".length());
        Served served = safe.fetch(id);
        section("THE DOWNLOAD OF " + id + " (its headers):");
        line("Content-Type: " + served.contentType());
        line("Content-Disposition: " + (served.attachment() ? "attachment" : "inline"));
        line("X-Content-Type-Options: " + (served.noSniff() ? "nosniff" : "(none)"));
        section("THE SETTINGS AFTER THOSE UPLOADS:");
        Path config = layout.config().resolve("app.properties");
        line(shown(config) + " reads: " + Files.readString(config));
    }

    private void returnLinks() {
        act("SEVEN", "Return links stay on our own site.");
        String[] links = {"/orders?page=2", "https://evil.example/login", "//evil.example", "/\\evil.example", "javascript:steal()"};
        section("THE REQUESTS (the returnTo value after sign-in):");
        for (String link : links) {
            line(link);
        }
        section("THE NAIVE RESPONSES (Location: the link as it is):");
        for (String link : links) {
            line(String.format("%-27s%s", ReturnLink.naive(link), ReturnLink.reason(link) == null ? "" : "  <- NOT OUR SITE"));
        }
        section("THE FIXED RESPONSES (a path on our site, or /account):");
        for (String link : links) {
            String why = ReturnLink.reason(link);
            line(ReturnLink.safe(link) + (why == null ? "" : "  (" + why + ")"));
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
        byte[] head = java.util.Arrays.copyOf(bytes, 4);
        StringBuilder text = new StringBuilder();
        for (byte b : head) {
            text.append(b >= 0x20 && b < 0x7f ? (char) b : '.');
        }
        return HexFormat.ofDelimiter(" ").formatHex(head) + "  " + text;
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    private static String attempt(Supplier<String> action) {
        try {
            return action.get();
        } catch (Refused e) {
            return "refused: " + e.getMessage();
        }
    }
}
