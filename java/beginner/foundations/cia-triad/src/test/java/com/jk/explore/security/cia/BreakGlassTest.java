package com.jk.explore.security.cia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BreakGlassTest {

    private final SecureStore store = new SecureStore(new Seal("test-secret"));

    BreakGlassTest() {
        store.save(CiaDemo.MAYA);
    }

    @Test
    void emergencyAccessShowsTheAddressAndIsLogged() {
        OrderView view = store.breakGlass(CiaDemo.RAJ, "O-101", "courier at the wrong door");
        assertEquals("12 Palm Street", view.address());
        assertEquals(1, store.auditLog().size());
        assertTrue(store.auditLog().get(0).contains("raj"));
    }

    @Test
    void emergencyAccessNeedsAReason() {
        assertThrows(AccessDeniedException.class, () -> store.breakGlass(CiaDemo.RAJ, "O-101", " "));
        assertEquals(0, store.auditLog().size());
    }
}
