package com.jk.explore.security.mtls;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The step command line and step-ca, one pinned release each. What they issue decides the golden output (names, chain, lifetimes), so the demo downloads
 * each release from Smallstep's GitHub releases into build/tools the first time and refuses it unless its SHA-256 (from the release's checksums file) matches.
 */
final class StepTools {

    static final String STEP_VERSION = "0.31.0";
    static final String STEP_CA_VERSION = "0.30.2";

    private static final Map<String, String> SHA256 = Map.of(
            "step_darwin_0.31.0_arm64.tar.gz", "91963c7c1ef6654dda61ff2f99cff2b8e95efce198d8c7b3532a00a8f253bf41",
            "step_darwin_0.31.0_amd64.tar.gz", "4b928a936e9d2ff3bfa6f7638003a9e46c702a27cf515169c6e5996ade8cd23e",
            "step_linux_0.31.0_arm64.tar.gz", "9937964c7d47de47a511cd775bd07101b8ee9854db210abd402c69093f0a6fd4",
            "step_linux_0.31.0_amd64.tar.gz", "a7e13726efa8782f3d5459174dacf602ab3514b9431f2d42a6a75950b7685e5f",
            "step-ca_darwin_0.30.2_arm64.tar.gz", "67bfba1bc440fca65ec820d4349b749f14ca72e37f22d9e80767159ebed1cca7",
            "step-ca_darwin_0.30.2_amd64.tar.gz", "430fa9078bf5adafc92ee68244a5dc4cbbbb2bc190af9785ad962be39c552887",
            "step-ca_linux_0.30.2_arm64.tar.gz", "43f79d1b0b8cab9895dbadd46b0a014006f682a6b1341e1fadf0be31d4b9f4b4",
            "step-ca_linux_0.30.2_amd64.tar.gz", "126615795bafe3f2d3f890e2d628fa6e2857315fb48d0671d34b23047cc37d73");

    private static final Path HOME = Path.of("build", "tools").toAbsolutePath();

    private StepTools() {
    }

    /** The release asset for this machine, for example step_darwin_0.31.0_arm64.tar.gz. */
    static String asset(String tool, String version, String os, String arch) {
        String o = os.toLowerCase(Locale.ROOT).contains("mac") ? "darwin" : "linux";
        String a = arch.equals("aarch64") || arch.equals("arm64") ? "arm64" : "amd64";
        return tool + "_" + o + "_" + version + "_" + a + ".tar.gz";
    }

    static String step() throws IOException, InterruptedException {
        String asset = asset("step", STEP_VERSION, System.getProperty("os.name"), System.getProperty("os.arch"));
        return fetch("step-" + STEP_VERSION, asset, "https://github.com/smallstep/cli/releases/download/v" + STEP_VERSION + "/" + asset,
                "step_" + STEP_VERSION + "/bin/step");
    }

    static String stepCa() throws IOException, InterruptedException {
        String asset = asset("step-ca", STEP_CA_VERSION, System.getProperty("os.name"), System.getProperty("os.arch"));
        return fetch("step-ca-" + STEP_CA_VERSION, asset, "https://github.com/smallstep/certificates/releases/download/v" + STEP_CA_VERSION + "/" + asset, "step-ca");
    }

    private static synchronized String fetch(String folder, String asset, String url, String member) throws IOException, InterruptedException {
        Path home = HOME.resolve(folder);
        Path bin = home.resolve(member);
        if (Files.isExecutable(bin)) {
            return bin.toString();
        }
        Files.createDirectories(home);
        Path archive = home.resolve(asset);
        try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            HttpResponse<Path> r = client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofFile(archive));
            if (r.statusCode() != 200) {
                throw new IOException("download of " + asset + " answered " + r.statusCode());
            }
        }
        String actual;
        try {
            actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(archive)));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        if (!actual.equals(SHA256.get(asset))) {
            Files.deleteIfExists(archive);
            throw new IOException(asset + " does not match its pinned SHA-256 (got " + actual + ")");
        }
        Sh.must(home, Map.of(), List.of("tar", "-xzf", asset, member));
        Files.delete(archive);
        return bin.toString();
    }
}
