package com.jk.explore.security.files;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Runs the invoice download, photo upload and "return to" story in five numbered acts and prints exact, repeatable results. */
public class FilesDemo {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};
    static final byte[] HTML_PAGE = "<html><script>steal()</script></html>".getBytes(StandardCharsets.ISO_8859_1);
    static final byte[] DISGUISED = HTML_PAGE;

    public static void main(String[] args) throws IOException {
        System.out.print(run());
    }

    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        try (Layout layout = Layout.create()) {
            out.append("== 1. Downloading an invoice ==\n");
            line(out, "invoice-1001.txt", new NaiveDownloads(layout), "invoice-1001.txt");

            out.append("\n== 2. A name with .. leaves the folder ==\n");
            Downloads naive = new NaiveDownloads(layout);
            line(out, "../secrets/db.txt", naive, "../secrets/db.txt");
            line(out, "../config/app.properties", naive, "../config/app.properties");

            out.append("\n== 3. A text check on the path is not a path check ==\n");
            Downloads prefix = new PrefixDownloads(layout);
            line(out, "../secrets/db.txt", prefix, "../secrets/db.txt");
            line(out, "../invoices-private/payroll.txt", prefix, "../invoices-private/payroll.txt");

            out.append("\n== 4. Names, paths and links, all checked ==\n");
            Downloads safe = new SafeDownloads(layout);
            line(out, "invoice-1001.txt", safe, "invoice-1001.txt");
            line(out, "../secrets/db.txt", safe, "../secrets/db.txt");
            line(out, "%2e%2e%2fsecrets%2fdb.txt", safe, "%2e%2e%2fsecrets%2fdb.txt");
            line(out, "../invoices-private/payroll.txt", safe, "../invoices-private/payroll.txt");
            line(out, "invoice-9999.txt", safe, "invoice-9999.txt");
            Files.createSymbolicLink(layout.invoices().resolve("invoice-5555.txt"), layout.secrets().resolve("db.txt"));
            line(out, "invoice-5555.txt (a link)", naive, "invoice-5555.txt");
            line(out, "invoice-5555.txt (a link)", prefix, "invoice-5555.txt");
            line(out, "invoice-5555.txt (a link)", safe, "invoice-5555.txt");

            out.append("\n== 5. Uploads ==\n");
            Uploads naiveUp = new NaiveUploads(layout);
            out.append(String.format("%-34s -> %s%n", "a photo called cat.png", naiveUp.save("cat.png", PNG)));
            String url = naiveUp.save("page.html", HTML_PAGE);
            Served page = new WebRoot(layout.webroot()).serve(url);
            out.append(String.format("%-34s -> served as %s, inline%n", "a script called page.html", page.contentType()));
            naiveUp.save("../../config/app.properties", "admin=mallory".getBytes(StandardCharsets.UTF_8));
            out.append(String.format("%-34s -> config now reads: %s%n", "a name with ..", Files.readString(layout.config().resolve("app.properties"))));

            Files.writeString(layout.config().resolve("app.properties"), "admin=ana");
            SafeUploads safeUp = new SafeUploads(layout);
            String photo = safeUp.save("cat.png", PNG);
            Served served = safeUp.fetch(photo.substring("/photos/".length()));
            out.append(String.format("%-34s -> %s (%s, download=%s, nosniff=%s)%n", "a photo called cat.png", photo, served.contentType(),
                    served.attachment(), served.noSniff()));
            out.append(String.format("%-34s -> %s%n", "a script called page.html", attempt(() -> safeUp.save("page.html", HTML_PAGE))));
            out.append(String.format("%-34s -> %s%n", "a script renamed cat.png", attempt(() -> safeUp.save("cat.png", DISGUISED))));
            out.append(String.format("%-34s -> %s%n", "a name with ..", safeUp.save("../../config/app.properties", PNG)));
            out.append(String.format("%-34s -> config still reads: %s%n", "after those uploads", Files.readString(layout.config().resolve("app.properties"))));
            out.append(String.format("%-34s -> %s%n", "an oversized photo", attempt(() -> safeUp.save("big.png", new byte[5000]))));
        }

        out.append("\n== 6. Where to send the customer next ==\n");
        for (String target : new String[] {"/orders?page=2", "https://evil.example/login", "//evil.example", "/\\evil.example", "javascript:steal()", ""}) {
            out.append(String.format("%-34s -> naive: %-26s safe: %s%n", "\"" + target + "\"", ReturnLink.naive(target), ReturnLink.safe(target)));
        }
        out.append(String.format("%-34s -> %s%n", "own host, exact match", ReturnLink.isOwnHost("https://shop.example/x")));
        out.append(String.format("%-34s -> %s%n", "shop.example.evil.example", ReturnLink.isOwnHost("https://shop.example.evil.example/x")));
        out.append(String.format("%-34s -> %s%n", "shop.example@evil.example", ReturnLink.isOwnHost("https://shop.example@evil.example/x")));
        return out.toString();
    }

    private static void line(StringBuilder out, String label, Downloads d, String name) {
        out.append(String.format("%-34s -> %s%n", label, attempt(() -> "read: " + d.read(name))));
    }

    private static String attempt(java.util.function.Supplier<String> action) {
        try {
            return action.get();
        } catch (Refused e) {
            return "refused: " + e.getMessage();
        }
    }
}
