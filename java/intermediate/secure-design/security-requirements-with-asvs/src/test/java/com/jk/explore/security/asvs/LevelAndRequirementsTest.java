package com.jk.explore.security.asvs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LevelAndRequirementsTest {

    @TempDir
    Path dir;

    @Test
    void levelsIncludeTheOnesBelow() {
        assertTrue(Level.L3.covers(Level.L1));
        assertTrue(Level.L2.covers(Level.L2));
        assertFalse(Level.L1.covers(Level.L2));
    }

    @Test
    void theStoreIsL2AndTheBankL3() {
        assertEquals(Level.L1, LevelChooser.choose(AsvsDemo.BROCHURE).level());
        assertEquals(Level.L2, LevelChooser.choose(AsvsDemo.STORE).level());
        assertEquals(Level.L3, LevelChooser.choose(AsvsDemo.BANK).level());
        assertEquals(3, LevelChooser.choose(AsvsDemo.BANK).reasons().size());
    }

    @Test
    void movingMoneyAloneIsNotL3() {
        Profile wallet = new Profile("a small wallet", Profile.Sensitivity.MEDIUM, true, true, false);
        assertEquals(Level.L2, LevelChooser.choose(wallet).level());
    }

    @Test
    void everyAutomatedRequirementNamesAKnownCheckAndIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (Requirement r : Catalogue.all()) {
            assertTrue(ids.add(r.id()), "duplicate id " + r.id());
            if (r.verification() != Verification.MANUAL) {
                assertTrue(Checks.all().containsKey(r.check()), r.id() + " names an unknown check");
            }
        }
    }

    @Test
    void aManualRequirementCannotNameACheckAndAnAutomatedOneMust() {
        assertThrows(IllegalArgumentException.class,
                () -> new Requirement("X-1", "Authorization", Level.L1, Verification.MANUAL, "headers", "5.0", "s"));
        assertThrows(IllegalArgumentException.class,
                () -> new Requirement("X-2", "Authorization", Level.L1, Verification.AUTOMATED, "", "5.0", "s"));
    }

    @Test
    void theRequirementsFileRoundTrips() throws IOException {
        Path file = dir.resolve("r.tsv");
        RequirementsFile.write(file, Catalogue.all());
        assertEquals(Catalogue.all(), RequirementsFile.read(file));
        assertEquals(RequirementsFile.HEADER, Files.readAllLines(file).get(0));
    }

    @Test
    void aBrokenRequirementsFileIsRefused() throws IOException {
        Path noHeader = dir.resolve("a.tsv");
        Files.write(noHeader, List.of("SR-01\tx"));
        assertThrows(IOException.class, () -> RequirementsFile.read(noHeader));
        Path shortLine = dir.resolve("b.tsv");
        Files.write(shortLine, List.of(RequirementsFile.HEADER, "SR-01\tAuthorization\tL1"));
        assertThrows(IOException.class, () -> RequirementsFile.read(shortLine));
        Path badLevel = dir.resolve("c.tsv");
        Files.write(badLevel, List.of(RequirementsFile.HEADER, "SR-01\tAuthorization\tL9\tMANUAL\t\t5.0\tstatement"));
        assertThrows(IOException.class, () -> RequirementsFile.read(badLevel));
    }
}
