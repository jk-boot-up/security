package com.jk.explore.security.hsmkeys;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider;
import java.security.Security;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** A scratch directory with a SoftHSM2 configuration, one initialised token, and the SunPKCS11 provider pointed at it. Removed on close. */
final class Lab implements AutoCloseable {

    static final String LABEL = "bank-hsm";
    static final String KEY_ALIAS = "payments-key";
    /** Demo PINs, built from parts so that no token-shaped string sits in the source. */
    static final String USER_PIN = String.join("", "24", "68", "13");
    static final String SO_PIN = String.join("", "97", "53", "10", "86");

    private final Path dir;
    private final Path library;
    private final Path tokens;
    private final Path softhsmConf;
    private final Path providerConfig;
    private Provider provider;

    Lab(Path buildDir) throws IOException, InterruptedException {
        Files.createDirectories(buildDir);
        this.dir = Files.createTempDirectory(buildDir, "hsm-lab-");
        this.library = findLibrary();
        this.tokens = Files.createDirectories(dir.resolve("tokens"));
        this.softhsmConf = dir.resolve("softhsm2.conf");
        writeSoftHsmConf(softhsmConf, tokens);
        Native.setenv("SOFTHSM2_CONF", softhsmConf.toString());
        initToken(LABEL);
        this.providerConfig = dir.resolve("pkcs11.cfg");
        Files.writeString(providerConfig, providerConfigText(library, "hsm", KEY_ALIAS));
    }

    static void writeSoftHsmConf(Path conf, Path tokenDir) throws IOException {
        Files.writeString(conf, "directories.tokendir = " + tokenDir + "\nobjectstore.backend = file\nlog.level = ERROR\nslots.removable = false\n");
    }

    /** The SunPKCS11 configuration: the first token, and the attributes every key generated through it gets. */
    static String providerConfigText(Path library, String name, String alias) {
        String label = "0h" + java.util.HexFormat.of().formatHex(alias.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "name=" + name + "\nlibrary=" + library + "\nslotListIndex=0\n"
                + "attributes(*,CKO_PRIVATE_KEY,CKK_RSA) = {\n  CKA_TOKEN = true\n  CKA_PRIVATE = true\n  CKA_SENSITIVE = true\n  CKA_EXTRACTABLE = false\n  CKA_LABEL = " + label + "\n}\n"
                + "attributes(*,CKO_PUBLIC_KEY,CKK_RSA) = {\n  CKA_TOKEN = true\n  CKA_LABEL = " + label + "\n}\n";
    }

    static Path findLibrary() {
        for (String c : List.of("/opt/homebrew/lib/softhsm/libsofthsm2.so", "/usr/local/lib/softhsm/libsofthsm2.so", "/usr/lib/softhsm/libsofthsm2.so",
                "/usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so", "/usr/lib/aarch64-linux-gnu/softhsm/libsofthsm2.so")) {
            if (Files.isRegularFile(Path.of(c))) {
                return Path.of(c);
            }
        }
        throw new IllegalStateException("SoftHSM2 is not installed (brew install softhsm)");
    }

    Map<String, String> env() {
        return Map.of("SOFTHSM2_CONF", softhsmConf.toString());
    }

    Path dir() {
        return dir;
    }

    Path library() {
        return library;
    }

    Path tokens() {
        return tokens;
    }

    Path softhsmConf() {
        return softhsmConf;
    }

    Path providerConfig() {
        return providerConfig;
    }

    void initToken(String label) throws IOException, InterruptedException {
        // the child gets the environment explicitly: Java's process environment is a snapshot taken before our setenv
        Sh.Result r = Sh.run(null, env(), "", List.of("softhsm2-util", "--init-token", "--free", "--label", label, "--so-pin", SO_PIN, "--pin", USER_PIN));
        if (!r.ok()) {
            throw new IllegalStateException("softhsm2-util failed: " + r.output());
        }
    }

    /** A fresh SunPKCS11 provider on the token (not yet logged in). */
    Provider provider() {
        if (provider != null) {
            Security.removeProvider(provider.getName());
        }
        provider = Security.getProvider("SunPKCS11").configure(providerConfig.toString());
        Security.addProvider(provider);
        return provider;
    }

    String keytool() {
        return Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
    }

    /** keytool arguments that address the token. */
    List<String> keytoolToken() {
        return List.of(keytool(), "-storetype", "PKCS11", "-providerClass", "sun.security.pkcs11.SunPKCS11", "-providerArg", providerConfig.toString(),
                "-storepass", USER_PIN);
    }

    static void delete(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        if (provider != null) {
            Security.removeProvider(provider.getName());
        }
        delete(dir);
    }
}
