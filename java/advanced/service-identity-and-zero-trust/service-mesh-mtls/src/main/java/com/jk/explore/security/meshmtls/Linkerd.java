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
            download("https://github.com/linkerd/linkerd2/releases/download/" + RELEASE + "/"
                    + assetName(System.getProperty("os.name"), System.getProperty("os.arch")), file);
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
        }
        return file.toAbsolutePath();
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
