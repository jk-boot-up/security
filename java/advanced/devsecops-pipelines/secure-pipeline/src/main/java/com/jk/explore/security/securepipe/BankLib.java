package com.jk.explore.security.securepipe;

import java.io.IOException;

/** The small Gradle library of a bank that the pipelines build: a git repository with two commits, in one of several flavours. */
final class BankLib {

    /** What is wrong with the second commit, or with the way the first one is built. */
    enum Flavour {
        CLEAN, FAILING_TEST, VULNERABLE_DEPENDENCY, SECRET, FILE_TIMES, BUILD_PATH
    }

    private BankLib() {
    }

    static String ledger(boolean broken) {
        return """
                package bank.ledger;

                import java.nio.charset.StandardCharsets;
                import org.apache.commons.codec.binary.Hex;

                public final class Ledger {

                    private Ledger() {
                    }

                    /** The fee for a transfer: one percent, in cents, rounded down. */
                    public static long feeCents(long amountCents) {
                        return amountCents / %d;
                    }

                    /** The balance after a transfer and its fee. */
                    public static long transfer(long balanceCents, long amountCents) {
                        long total = Math.addExact(amountCents, feeCents(amountCents));
                        if (total > balanceCents) {
                            throw new IllegalArgumentException("insufficient funds");
                        }
                        return balanceCents - total;
                    }

                    /** The first four hex digits of an account number, used on statements. */
                    public static String accountCheck(String account) {
                        return Hex.encodeHexString(account.getBytes(StandardCharsets.UTF_8)).substring(0, 4);
                    }
                }
                """.formatted(broken ? 10 : 100);
    }

    static final String LEDGER_TEST = """
            package bank.ledger;

            import static org.junit.jupiter.api.Assertions.assertEquals;
            import static org.junit.jupiter.api.Assertions.assertThrows;

            import org.junit.jupiter.api.Test;

            class LedgerTest {

                @Test
                void feeIsOnePercentRoundedDown() {
                    assertEquals(100, Ledger.feeCents(10_000));
                    assertEquals(0, Ledger.feeCents(99));
                }

                @Test
                void transferTakesTheFee() {
                    assertEquals(89_900, Ledger.transfer(100_000, 10_000));
                }

                @Test
                void transferRefusesWhenFundsAreShort() {
                    assertThrows(IllegalArgumentException.class, () -> Ledger.transfer(10_000, 10_000));
                }

                @Test
                void accountCheckIsFourHexDigits() {
                    assertEquals("3132", Ledger.accountCheck("12"));
                }
            }
            """;

    static String buildGradle(Flavour flavour) {
        boolean fixed = flavour != Flavour.FILE_TIMES;
        StringBuilder b = new StringBuilder("""
                plugins {
                    id 'java-library'
                }

                group = 'bank.example'
                version = '1.0.0'

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'commons-codec:commons-codec:1.17.1'
                """);
        if (flavour == Flavour.VULNERABLE_DEPENDENCY) {
            b.append("    implementation 'org.apache.commons:commons-text:1.9'\n");
        }
        b.append("""
                    testImplementation platform('org.junit:junit-bom:6.1.3')
                    testImplementation 'org.junit.jupiter:junit-jupiter'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
                }

                test {
                    useJUnitPlatform()
                }
                """);
        // Gradle 9 makes archives reproducible by default; many builds still set the old behaviour on purpose, which this flavour does.
        b.append("""

                // %s
                tasks.withType(AbstractArchiveTask).configureEach {
                    preserveFileTimestamps = %b
                    reproducibleFileOrder = %b
                }
                """.formatted(fixed ? "Reproducible archives: no file times, and the entries always in the same order." : "Archives that keep the file times and the order the disk gives.",
                !fixed, fixed));
        if (flavour == Flavour.BUILD_PATH) {
            b.append("""

                    // A "build info" file that records where the build ran.
                    def buildInfo = tasks.register('buildInfo') {
                        def out = layout.buildDirectory.file('generated/build-info.properties')
                        outputs.file(out)
                        doLast {
                            def f = out.get().asFile
                            f.parentFile.mkdirs()
                            f.text = "builtIn=${projectDir}\\n"
                        }
                    }
                    processResources {
                        from(buildInfo)
                    }
                    """);
        }
        return b.toString();
    }

    /** A repository with a first commit that builds and a second commit of the given flavour. */
    static Git origin(Flavour flavour) throws IOException, InterruptedException {
        Git g = Git.init();
        g.write("settings.gradle", "rootProject.name = 'bank-ledger'\n");
        g.write(".gitignore", "build/\n.gradle/\n");
        g.write("build.gradle", buildGradle(flavour));
        g.write("src/main/java/bank/ledger/Ledger.java", ledger(false));
        g.write("src/test/java/bank/ledger/LedgerTest.java", LEDGER_TEST);
        g.commit("ledger with a one percent fee");
        switch (flavour) {
            case FAILING_TEST -> g.write("src/main/java/bank/ledger/Ledger.java", ledger(true));
            case SECRET -> g.write("config/payments.properties", "gateway.user=ledger\ngateway.access_key=" + "AK" + "IAQ3RT7KJH2WNP5XCV" + "\n");
            default -> g.write("README.md", "The fee is one percent of the amount, rounded down.\n");
        }
        g.commit("next change");
        return g;
    }
}
