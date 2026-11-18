package com.jk.explore.security.ssrfspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The importers against the invented internet, with real HTTP over the loopback address and no Spring server in between. */
class ImporterTest {

    private static Internet internet;
    private static Fetcher fetcher;

    @BeforeAll
    static void start() throws Exception {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host");
        internet = new Internet();
        fetcher = new Fetcher();
    }

    @AfterAll
    static void stop() {
        internet.close();
    }

    private Importer naive() {
        return new NaiveImporter(internet, internet, fetcher);
    }

    private Importer blocklist() {
        return new BlocklistImporter(internet, internet, fetcher);
    }

    private Importer guarded() {
        return new GuardedImporter(internet, internet, fetcher);
    }

    private String refusal(Importer importer, String address) {
        internet.resetNameServer();
        return assertThrows(ImportRefused.class, () -> importer.importImage(address)).getMessage();
    }

    @Test
    void everyImporterFetchesAHonestImage() throws Exception {
        for (Importer importer : new Importer[] {naive(), blocklist(), guarded()}) {
            assertEquals("imported: PNG-cat", importer.importImage(SsrfSpringDemo.HONEST));
        }
    }

    @Test
    void theNaiveImporterReachesTheMetadataAddressTheLocalhostAndTheOffice() throws Exception {
        assertEquals("imported: AccessKeyId=DEMO-NOT-A-REAL-KEY", naive().importImage("http://169.254.169.254/latest/meta-data/"));
        assertEquals("imported: DB_PASSWORD=demo-not-real", naive().importImage("http://localhost/"));
        assertEquals("imported: internal admin panel: 3 customers", naive().importImage("http://10.0.0.5/"));
    }

    @Test
    void theBlocklistStopsTheListedNameAndNothingElse() throws Exception {
        assertEquals("blocked host name 169.254.169.254", refusal(blocklist(), "http://169.254.169.254/"));
        assertEquals("imported: AccessKeyId=DEMO-NOT-A-REAL-KEY", blocklist().importImage("http://2852039166/latest/meta-data/"));
        assertEquals("imported: DB_PASSWORD=demo-not-real", blocklist().importImage("http://127.1/"));
        assertEquals("imported: AccessKeyId=DEMO-NOT-A-REAL-KEY", blocklist().importImage("http://[::ffff:a9fe:a9fe]/"));
        assertEquals("imported: internal admin panel: 3 customers", blocklist().importImage("http://inside.evil.example/"));
        assertEquals("imported: AccessKeyId=DEMO-NOT-A-REAL-KEY", blocklist().importImage("http://evil.example/redirect"));
    }

    @Test
    void theGuardedImporterRefusesEverySpellingAndTheRedirect() {
        for (String address : new String[] {"http://169.254.169.254/", "http://2852039166/", "http://127.1/", "http://[::ffff:a9fe:a9fe]/",
                "http://inside.evil.example/", "http://evil.example/redirect", "http://localhost/", "http://dual.evil.example/cat.png"}) {
            String why = refusal(guarded(), address);
            assertTrue(why.endsWith("not public"), address + " -> " + why);
        }
    }

    @Test
    void theGuardedImporterRefusesUserInfoOddPortsAndOtherSchemes() {
        assertEquals("user info in the address", refusal(guarded(), "http://images.example@169.254.169.254/"));
        assertEquals("port 22 is not allowed", refusal(guarded(), "http://images.example:22/"));
        assertEquals("only http and https addresses are imported", refusal(guarded(), "file:///etc/passwd"));
    }

    @Test
    void theGuardedImporterConnectsToTheAddressItJudged() throws Exception {
        internet.resetNameServer();
        assertEquals("imported: PNG-attacker-cat", guarded().importImage("http://rebind.evil.example/cat.png"));
    }

    @Test
    void checkingAndThenConnectingByNameFallsToARebindingNameServer() throws Exception {
        internet.resetNameServer();
        Importer twoStep = new TwoStepImporter(internet, internet, fetcher);
        assertEquals("imported: internal admin panel: 3 customers", twoStep.importImage("http://rebind.evil.example/cat.png"));
        assertEquals(List.of("check: look up rebind.evil.example: [203.0.113.66], public", "look up rebind.evil.example: [10.0.0.5]",
                "GET /cat.png at 10.0.0.5, Host: rebind.evil.example", "200 text/plain, 33 bytes"), twoStep.trace());
    }

    @Test
    void theGuardedImporterLogsEveryCheckOfARedirect() {
        Importer guarded = guarded();
        assertEquals("169.254.169.254 is not public", refusal(guarded, "http://evil.example/redirect"));
        assertEquals(List.of("port 80, no user info: allowed", "look up evil.example once: [203.0.113.66]", "203.0.113.66 is public: connect to it",
                "GET /redirect at 203.0.113.66, Host: evil.example", "302 redirect to 169.254.169.254: check it again",
                "port 80, no user info: allowed", "look up 169.254.169.254 once: [169.254.169.254]", "refused: 169.254.169.254 is not public"),
                guarded.trace());
    }

    @Test
    void theFormCarriesTheAddressPercentEncoded() {
        assertEquals("url=http%3A%2F%2Fimages.example%2Fcat.png", SsrfSpringDemo.form(SsrfSpringDemo.HONEST));
    }

    @Test
    void limitsOnTypeSizeAndTime() {
        assertEquals("not an image (text/html)", refusal(guarded(), "http://evil.example/page.html"));
        assertEquals("larger than 1024 bytes", refusal(guarded(), "http://evil.example/big.png"));
        assertEquals("no answer within 1500 ms", refusal(guarded(), "http://evil.example/slow.png"));
    }
}
