package com.jk.explore.security.threats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SurfaceTest {

    @Test
    void theStoreStartsWithSixWaysIn() {
        Surface surface = ThreatsDemo.storeSurface();
        assertEquals(6, surface.size());
        assertEquals(2, surface.count(Exposure.PUBLIC));
        assertEquals(2, surface.count(Exposure.AUTHENTICATED));
        assertEquals(2, surface.count(Exposure.INTERNAL));
    }

    @Test
    void everyNewFeatureAddsWaysIn() {
        Surface surface = ThreatsDemo.storeSurface();
        surface.add(new EntryPoint("POST /reviews", Exposure.AUTHENTICATED, "review text"));
        surface.add(new EntryPoint("GET /debug/config", Exposure.PUBLIC, "settings and keys"));
        assertEquals("8 entry points: 3 public, 3 authenticated, 2 internal", surface.summary());
    }
}
