package com.jk.explore.security.netiso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real Docker networks and containers. Run with ./gradlew integrationTest. */
@Tag("integration")
class IsolationTest {

    @Test
    void onFlatNetworkEverythingReachesEverything() throws Exception {
        try (Lab lab = new Lab()) {
            lab.network("flat", false);
            for (Service s : Service.shop()) {
                lab.start(s, "flat", false);
            }
            Thread.sleep(1000);
            assertEquals(List.of("app", "db", "cache", "admin", "ci"), lab.reachesByName("web"));
            assertEquals(15, lab.pairs().size());
        }
    }

    @Test
    void aLayoutBuiltFromTheIntentMatchesItExactly() throws Exception {
        Intent intent = Intent.shop();
        try (Lab lab = new Lab()) {
            lab.build(intent, true);
            Thread.sleep(1000);
            assertEquals(intent.pairs(), lab.pairs());
            assertEquals(List.of("app"), lab.reachesByName("web"));
            assertEquals(List.of("app"), lab.reachesByAddress("web"));
        }
    }

    @Test
    void joiningAnotherNetworkIsDriftFoundByMeasuring() throws Exception {
        Intent intent = Intent.shop();
        try (Lab lab = new Lab()) {
            lab.build(intent, false);
            lab.network("oops", true);
            lab.join("web", "oops");
            lab.join("db", "oops");
            Thread.sleep(500);
            assertEquals(java.util.Set.of("db-web"), intent.extra(lab.pairs()));
        }
    }

    @Test
    void onlyThePublishedServiceIsReachableFromTheHostAndOnlyTheFrontCanLeave() throws Exception {
        try (Lab lab = new Lab()) {
            lab.build(Intent.shop(), true);
            lab.start(new Service("evil", 4444, "outside"), "edge", false);
            Thread.sleep(1000);
            assertTrue(lab.publishedPort("web") > 0);
            assertEquals(-1, lab.publishedPort("app"));
            assertTrue(lab.connects("web", "evil", 4444));
            assertFalse(lab.connects("db", "evil", 4444));
            assertFalse(lab.connects("app", "evil", 4444));
        }
    }
}
