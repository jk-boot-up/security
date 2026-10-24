package com.jk.explore.security.meshmtls;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The Linkerd release the demo uses, downloaded from the project's own GitHub releases into build/ the first time. */
final class Linkerd {

    static final String RELEASE = "edge-26.10.1";
    static final String GATEWAY_API = "v1.5.1";

    // SHA-256 of the standard-install.yaml for GATEWAY_API, and of each CLI release asset, so a changed or substituted download is caught.
    private static final String GATEWAY_API_SHA256 = "751002b3b91a87f7ae3bd2517c79a47a8d7ed6702901808a1cf9bd97d284f9b8";
    private static final java.util.Map<String, String> CLI_SHA256 = java.util.Map.of(
            "linkerd2-cli-" + RELEASE + "-darwin-arm64", "abaaaeba9334feae7c1f16dd43a6f85b6051eadf49425f0ae30cbf16eba29f70",
            "linkerd2-cli-" + RELEASE + "-linux-amd64", "66d634f5318c8016f0b5face246a688964b6e2c86d6c3be34be68ab9fcd2a8cc",
            "linkerd2-cli-" + RELEASE + "-linux-arm64", "431989b15b48acfd6e344ff16a0d3505574c0cce551a2d4aeae82246ce65a5cc");

    private static final Path HOME = Path.of("build", "linkerd");

    private Linkerd() {
    }

    /** The release asset name for this machine, for example linkerd2-cli-edge-26.10.1-darwin-arm64. */
    static String assetName(String os, String arch) {
        String o = os.toLowerCase(Locale.ROOT).contains("mac") ? "darwin" : "linux";
        String a = arch.equals("aarch64") || arch.equals("arm64") ? "arm64" : "amd64";
        return "linkerd2-cli-" + RELEASE + "-" + o + "-" + a;
    }

    static Path cli() throws IOException, InterruptedException {
        Path file = HOME.resolve("linkerd-" + RELEASE);
        if (!Files.isExecutable(file)) {
            String asset = assetName(System.getProperty("os.name"), System.getProperty("os.arch"));
            download("https://github.com/linkerd/linkerd2/releases/download/" + RELEASE + "/" + asset, file);
            verifySha256(file, CLI_SHA256.get(asset));
            if (!file.toFile().setExecutable(true)) {
                throw new IOException("cannot make " + file + " executable");
            }
        }
        return file.toAbsolutePath();
    }

    /** The Gateway API definitions that Linkerd needs installed first. */
    static Path gatewayApi() throws IOException, InterruptedException {
        Path file = HOME.resolve("gateway-api-" + GATEWAY_API + ".yaml");
        if (!Files.exists(file)) {
            download("https://github.com/kubernetes-sigs/gateway-api/releases/download/" + GATEWAY_API + "/standard-install.yaml", file);
            verifySha256(file, GATEWAY_API_SHA256);
        }
        return file.toAbsolutePath();
    }

    /** Fails, and removes the file, unless its SHA-256 is the pinned one. */
    private static void verifySha256(Path file, String expected) throws IOException {
        String actual;
        try {
            actual = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        if (!actual.equals(expected)) {
            Files.deleteIfExists(file);
            throw new IOException(file.getFileName() + " does not match its pinned SHA-256 (got " + actual + ", expected " + expected + ")");
        }
    }

    /** Linkerd's own default for how long a workload certificate lives, read from the CLI's help text. */
    static String defaultIssuanceLifetime() throws IOException, InterruptedException {
        String help = Sh.must(java.util.List.of(cli().toString(), "install", "--help"));
        Matcher m = Pattern.compile("--identity-issuance-lifetime duration.*\\(default ([^)]+)\\)").matcher(help);
        if (!m.find()) {
            throw new IllegalStateException("the CLI help does not state the default lifetime");
        }
        return m.group(1);
    }

    private static void download(String url, Path target) throws IOException, InterruptedException {
        Files.createDirectories(HOME);
        Path part = HOME.resolve(target.getFileName() + ".part");
        try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            HttpResponse<Path> r = client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofFile(part));
            if (r.statusCode() != 200) {
                throw new IOException("download of " + url + " answered " + r.statusCode());
            }
        }
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
