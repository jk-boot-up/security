package com.jk.explore.security.dragonmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** The real OWASP Threat Dragon image. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DragonTest {

    private Dragon dragon;

    @BeforeAll
    void start() throws Exception {
        dragon = new Dragon();
    }

    @AfterAll
    void stop() {
        dragon.close();
    }

    @Test
    void everyModelOfThePlanIsValidForTheEditor() throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        BankConfig c = BankConfig.weak();
        files.put("step0.json", ModelBuilder.json(c));
        int i = 1;
        for (Planner.Step s : Planner.plan(BankTree.baseline(), Defence.all(), 5_000)) {
            c = c.with(s.defence().configChange());
            files.put("step" + i++ + ".json", ModelBuilder.json(c));
        }
        Map<String, Dragon.Verdict> verdicts = dragon.validate(files);
        assertEquals(5, verdicts.size());
        verdicts.values().forEach(v -> assertTrue(v.valid(), v.errors().toString()));
    }

    @Test
    void theEditorsValidatorRefusesWhatTheDemoBreaks() throws Exception {
        String model = ModelBuilder.json(BankConfig.weak());
        Map<String, String> files = new LinkedHashMap<>();
        for (String how : new String[] {"no title", "short type", "no id", "tiny"}) {
            files.put(how.replace(' ', '-') + ".json", ThreatDragonDemo.broken(model, how));
        }
        Map<String, Dragon.Verdict> v = dragon.validate(files);
        assertTrue(v.get("no-title.json").errors().get(0).contains("required property 'title'"));
        assertTrue(v.get("short-type.json").errors().get(0).contains("shorter than 3"));
        assertTrue(v.get("no-id.json").errors().get(0).contains("required property 'id'"));
        assertTrue(v.get("tiny.json").errors().get(0).contains(">= 10"));
        v.values().forEach(x -> assertFalse(x.valid()));
    }

    @Test
    void theServerStartsWithGeneratedKeysAndOffersOnlyALocalSession() throws Exception {
        dragon.startServer();
        assertEquals(200, dragon.get("/healthz").status());
        assertTrue(dragon.get("/").body().contains("OWASP Threat Dragon"));
        assertTrue(dragon.get("/api/config").body().contains("\"localEnabled\":true"));
        assertEquals(401, dragon.get("/api/threatmodel/organisation/models").status());
    }
}
