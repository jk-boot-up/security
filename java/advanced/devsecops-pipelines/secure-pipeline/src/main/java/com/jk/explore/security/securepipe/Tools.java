package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The three outside tools, each one pinned release. Their answers decide the golden output, so the demo downloads each release from its project's GitHub
 * releases into build/tools the first time and refuses it unless its SHA-256 (from the release's checksums file) matches.
 */
final class Tools {

    static final String GITLEAKS_VERSION = "8.30.1";
    static final String TRIVY_VERSION = "0.76.1";
    static final String COSIGN_VERSION = "3.1.4";

    private static final Map<String, String> SHA256 = Map.ofEntries(
            Map.entry("gitleaks_8.30.1_darwin_arm64.tar.gz", "b40ab0ae55c505963e365f271a8d3846efbc170aa17f2607f13df610a9aeb6a5"),
            Map.entry("gitleaks_8.30.1_darwin_x64.tar.gz", "dfe101a4db2255fc85120ac7f3d25e4342c3c20cf749f2c20a18081af1952709"),
            Map.entry("gitleaks_8.30.1_linux_arm64.tar.gz", "e4a487ee7ccd7d3a7f7ec08657610aa3606637dab924210b3aee62570fb4b080"),
            Map.entry("gitleaks_8.30.1_linux_x64.tar.gz", "551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb"),
            Map.entry("trivy_0.76.1_macOS-ARM64.tar.gz", "11a9b6cd2752011b54f01ea4bd0dbfa9f0c41dc6538d5a3f9d23955f471bf4e5"),
            Map.entry("trivy_0.76.1_macOS-64bit.tar.gz", "7aa762eacd582052d053b62dcf972a1ab058abe39a57ccae234ed8894945b6b7"),
            Map.entry("trivy_0.76.1_Linux-ARM64.tar.gz", "e174dff2a6dba5e3ae41f8d0537a6324405bd08132497340304c6b20e0f16a1f"),
            Map.entry("trivy_0.76.1_Linux-64bit.tar.gz", "c7cbe3137fb8a48a17574bede3dd6beb16742d1e9772929940fc90082c39d9c0"),
            Map.entry("cosign-darwin-arm64", "9f9b8cd557cf2e5b3df3373a00baa005a86b02391e3e06dd0ce37b3a1533d3a4"),
            Map.entry("cosign-darwin-amd64", "9d1e4abe2b6be279ed0d2ff706fde1ccd738747c834e3e6d44039c238867334f"),
            Map.entry("cosign-linux-arm64", "ade83b7aac62e131cacfe96d958948b2150cc9efec46a570a2649b8e27dedad2"),
            Map.entry("cosign-linux-amd64", "424e2c97d0b45fd7a6b608a87f090f35633f4c23ece5f092d48bba11aaf4bbfa"));

    private static final Path HOME = Path.of("build", "tools").toAbsolutePath();

    private Tools() {
    }

    private static boolean mac() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
    }

    private static boolean arm() {
        String arch = System.getProperty("os.arch");
        return arch.equals("aarch64") || arch.equals("arm64");
    }

    /** The gitleaks release asset for this machine, for example gitleaks_8.30.1_darwin_arm64.tar.gz. */
    static String gitleaksAsset(boolean mac, boolean arm) {
        return "gitleaks_" + GITLEAKS_VERSION + "_" + (mac ? "darwin" : "linux") + "_" + (arm ? "arm64" : "x64") + ".tar.gz";
    }

    static String trivyAsset(boolean mac, boolean arm) {
        return "trivy_" + TRIVY_VERSION + "_" + (mac ? "macOS" : "Linux") + "-" + (arm ? "ARM64" : "64bit") + ".tar.gz";
    }

    static String cosignAsset(boolean mac, boolean arm) {
        return "cosign-" + (mac ? "darwin" : "linux") + "-" + (arm ? "arm64" : "amd64");
    }

    static String gitleaks() throws IOException, InterruptedException {
        String asset = gitleaksAsset(mac(), arm());
        return fetch("gitleaks-" + GITLEAKS_VERSION, asset, "https://github.com/gitleaks/gitleaks/releases/download/v" + GITLEAKS_VERSION + "/" + asset, "gitleaks");
    }

    static String trivy() throws IOException, InterruptedException {
        String asset = trivyAsset(mac(), arm());
        return fetch("trivy-" + TRIVY_VERSION, asset, "https://github.com/aquasecurity/trivy/releases/download/v" + TRIVY_VERSION + "/" + asset, "trivy");
    }

    static String cosign() throws IOException, InterruptedException {
        String asset = cosignAsset(mac(), arm());
        return fetch("cosign-" + COSIGN_VERSION, asset, "https://github.com/sigstore/cosign/releases/download/v" + COSIGN_VERSION + "/" + asset, null);
    }

    /** Downloads one release asset once, checks its SHA-256 and returns the program: the file itself, or `program` taken out of the tar archive. */
    private static synchronized String fetch(String folder, String asset, String url, String program) throws IOException, InterruptedException {
        Path home = HOME.resolve(folder);
        Path bin = home.resolve(program == null ? "cosign" : program);
        if (Files.isExecutable(bin)) {
            return bin.toString();
        }
        Files.createDirectories(home);
        Path download = home.resolve(asset);
        try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            HttpResponse<Path> r = client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofFile(download));
            if (r.statusCode() != 200) {
                throw new IOException("download of " + asset + " answered " + r.statusCode());
            }
        }
        String actual = Digest.sha256(download);
        if (!actual.equals(SHA256.get(asset))) {
            Files.deleteIfExists(download);
            throw new IOException(asset + " does not match its pinned SHA-256 (got " + actual + ")");
        }
        if (program == null) {
            Files.move(download, bin);
        } else {
            Sh.must(home, Map.of(), List.of("tar", "-xzf", asset, program));
            Files.delete(download);
        }
        if (!bin.toFile().setExecutable(true)) {
            throw new IOException("cannot make " + bin + " executable");
        }
        return bin.toString();
    }

    /** trivy needs its vulnerability database; it is downloaded once, and the gates then run with --skip-db-update. */
    static void trivyDatabase() throws IOException, InterruptedException {
        Path empty = Scratch.file("empty-bom", ".json");
        Files.writeString(empty, Sbom.cycloneDx(List.of()));
        if (!Sh.run(null, Map.of(), List.of(trivy(), "sbom", "--quiet", "--skip-db-update", empty.toString())).ok()) {
            Sh.must(null, Map.of(), List.of(trivy(), "image", "--quiet", "--download-db-only"));
        }
    }
}
