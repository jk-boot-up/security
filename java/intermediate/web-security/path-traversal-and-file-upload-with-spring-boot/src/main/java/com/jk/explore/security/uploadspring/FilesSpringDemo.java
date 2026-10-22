package com.jk.explore.security.uploadspring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs the invoice download, photo upload and "return to" story in six numbered acts against real Spring Boot servers on loopback. */
public class FilesSpringDemo {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};
    static final byte[] HTML_PAGE = "<html><script>steal()</script></html>".getBytes(StandardCharsets.ISO_8859_1);

    /** A shop started on a free loopback port with one set of versions. Closing it stops the server. */
    record Running(ConfigurableApplicationContext context, int port) implements AutoCloseable {

        /** An invoice download as the web form sends it, and what the shop answers. */
        String download(String name) throws IOException, InterruptedException {
            Wire.Reply reply = Wire.get(port, "/invoices?name=" + Wire.encode(name));
            return reply.status() == 200 ? "read: " + reply.body() : reply.body();
        }

        /** Uploads a file and returns the shop's answer: the address of the stored file, or the refusal. */
        String uploaded(String fileName, byte[] bytes) throws IOException, InterruptedException {
            return Wire.upload(port, fileName, bytes).body();
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

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only results are printed, never ports or temporary paths. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Layout layout = Layout.create();
                Running naive = start(layout, "naive");
                Running prefix = start(layout, "prefix");
                // the fixed shop limits an upload before the controller sees it: 2 kilobytes of file, 4 of request
                Running safe = start(layout, "safe", "spring.servlet.multipart.max-file-size=2KB", "spring.servlet.multipart.max-request-size=4KB")) {
            out.append("== 1. Downloading an invoice ==\n");
            row(out, "invoice-1001.txt", naive.download("invoice-1001.txt"));

            out.append("\n== 2. A name with .. leaves the folder ==\n");
            row(out, "../secrets/db.txt", naive.download("../secrets/db.txt"));
            row(out, "../config/app.properties", naive.download("../config/app.properties"));

            out.append("\n== 3. A text check on the path is not a path check ==\n");
            row(out, "../secrets/db.txt", prefix.download("../secrets/db.txt"));
            row(out, "../invoices-private/payroll.txt", prefix.download("../invoices-private/payroll.txt"));

            out.append("\n== 4. Names, paths and links, all checked ==\n");
            row(out, "invoice-1001.txt", safe.download("invoice-1001.txt"));
            row(out, "../secrets/db.txt", safe.download("../secrets/db.txt"));
            row(out, "%2e%2e%2fsecrets%2fdb.txt", safe.download("%2e%2e%2fsecrets%2fdb.txt"));
            row(out, "../invoices-private/payroll.txt", safe.download("../invoices-private/payroll.txt"));
            row(out, "invoice-9999.txt", safe.download("invoice-9999.txt"));
            Files.createSymbolicLink(layout.invoices().resolve("invoice-5555.txt"), layout.secrets().resolve("db.txt"));
            row(out, "invoice-5555.txt (a link)", naive.download("invoice-5555.txt"));
            row(out, "invoice-5555.txt (a link)", prefix.download("invoice-5555.txt"));
            row(out, "invoice-5555.txt (a link)", safe.download("invoice-5555.txt"));
            out.append("Spring's own static handler for the invoice folder:\n");
            row(out, "an encoded slash, ..%2f", String.valueOf(Wire.get(safe.port(), "/static-invoices/..%2fsecrets/db.txt").status()));
            row(out, "a dot-dot segment, %2e%2e/", String.valueOf(Wire.get(safe.port(), "/static-invoices/%2e%2e/secrets/db.txt").status()));
            Wire.Reply link = Wire.get(safe.port(), "/static-invoices/invoice-5555.txt");
            row(out, "invoice-5555.txt (a link)", link.status() + " " + link.body() + "  <- THE HANDLER FOLLOWS THE LINK");

            out.append("\n== 5. Uploads ==\n");
            row(out, "a photo called cat.png", naive.uploaded("cat.png", PNG));
            Wire.upload(naive.port(), "page.html", HTML_PAGE);
            Wire.Reply page = Wire.get(naive.port(), "/uploads/page.html");
            row(out, "a script called page.html", "served as " + page.contentType().split(";")[0] + ", " + page.disposition());
            Wire.upload(naive.port(), "../../config/app.properties", "admin=mallory".getBytes(StandardCharsets.UTF_8));
            row(out, "a name with ..", "config now reads: " + Files.readString(layout.config().resolve("app.properties")));

            Files.writeString(layout.config().resolve("app.properties"), "admin=ana");
            Wire.Reply photo = Wire.upload(safe.port(), "cat.png", PNG);
            Wire.Reply served = Wire.get(safe.port(), photo.body());
            row(out, "a photo called cat.png", photo.body() + " (" + served.contentType() + ", download=" + served.disposition().equals("attachment")
                    + ", nosniff=" + served.noSniff() + ")");
            row(out, "a script called page.html", Wire.upload(safe.port(), "page.html", HTML_PAGE).body());
            row(out, "a script renamed cat.png", Wire.upload(safe.port(), "cat.png", HTML_PAGE).body());
            row(out, "a name with ..", Wire.upload(safe.port(), "../../config/app.properties", PNG).body());
            row(out, "after those uploads", "config still reads: " + Files.readString(layout.config().resolve("app.properties")));
            row(out, "a photo of 1500 bytes", Wire.upload(safe.port(), "big.png", pngOf(1500)).body());
            Wire.Reply huge = Wire.upload(safe.port(), "big.png", pngOf(5000));
            row(out, "a photo of 5000 bytes", huge.status() + ", refused by spring.servlet.multipart.max-file-size before the controller runs");

            out.append("\n== 6. Where to send the customer next ==\n");
            for (String target : new String[] {"/orders?page=2", "https://evil.example/login", "//evil.example", "/\\evil.example", "javascript:steal()", ""}) {
                out.append(String.format("%-34s -> naive: %-26s safe: %s%n", "\"" + target + "\"", location(naive, target), location(safe, target)));
            }
            row(out, "own host, exact match", String.valueOf(ReturnLink.isOwnHost("https://shop.example/x")));
            row(out, "shop.example.evil.example", String.valueOf(ReturnLink.isOwnHost("https://shop.example.evil.example/x")));
            row(out, "shop.example@evil.example", String.valueOf(ReturnLink.isOwnHost("https://shop.example@evil.example/x")));
        }
        return out.toString();
    }

    private static String location(Running shop, String target) throws IOException, InterruptedException {
        return Wire.get(shop.port(), "/continue?returnTo=" + Wire.encode(target)).location();
    }

    private static byte[] pngOf(int size) {
        byte[] bytes = new byte[size];
        System.arraycopy(PNG, 0, bytes, 0, PNG.length);
        return bytes;
    }

    private static void row(StringBuilder out, String label, String result) {
        out.append(String.format("%-34s -> %s%n", label, result));
    }
}
