package com.jk.explore.security.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CanonicalTest {

    @ParameterizedTest
    @ValueSource(strings = {"ana", "ANA", "Ana", "ａｎａ", " ana ", "ＡＮＡ"})
    void allSpellingsOfOneNameHaveOneCanonicalForm(String raw) throws Rejected {
        assertEquals("ana", Username.parse(raw).value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"an​a", "ana\nadmin", "ana\u0000", "‮ana", "an­a"})
    void controlAndInvisibleCharactersAreRefused(String raw) {
        assertEquals("no control or invisible characters", assertThrows(Rejected.class, () -> Username.parse(raw)).rule());
    }

    @ParameterizedTest
    @ValueSource(strings = {"аna", "josé", "an", "aaaaaaaaaaaaaaaaaaaaa", "_ana", "ana!"})
    void namesOutsideTheAllowListAreRefused(String raw) {
        assertThrows(Rejected.class, () -> Username.parse(raw));
    }

    @Test
    void aTakenNameCannotBeRegisteredInAnotherSpelling() throws Rejected {
        Accounts accounts = new Accounts();
        accounts.register("ana");
        assertEquals("already taken", assertThrows(Rejected.class, () -> accounts.register("ＡＮＡ")).rule());
        assertTrue(new Accounts().registerNaively("ＡＮＡ"));
    }

    @Test
    void theCanonicalFormDoesNotDependOnTheMachinesLocale() throws Rejected {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.of("tr"));
            assertEquals("ivan", Username.parse("IVAN").value());
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void displayNamesAreStoredInNfc() throws Rejected {
        assertEquals("José", DisplayName.parse("José").value());
    }

    @Test
    void lookAlikesOfStaffNamesAreRefusedAndOtherScriptsAreWelcome() {
        assertThrows(Rejected.class, () -> DisplayName.parse("Shоp Suppоrt"));
        assertThrows(Rejected.class, () -> DisplayName.parse("Аna"));
        assertDoesNotThrow(() -> DisplayName.parse("Анна"));
        assertDoesNotThrow(() -> DisplayName.parse("アナ"));
    }
}
