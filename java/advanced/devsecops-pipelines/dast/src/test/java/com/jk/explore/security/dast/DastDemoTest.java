package com.jk.explore.security.dast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class DastDemoTest {

    @Test
    void aLongPageIsCutBeforeATagOrAnEntity() {
        assertEquals(List.of("<p>ab ", "&lt;cd", "</p>"), DastDemo.wrap("<p>ab &lt;cd</p>", 8));
    }

    @Test
    void everyPlantedFlawHasAKindAndTheKindsAddUpToNine() {
        assertEquals(3, DastDemo.planted("script"));
        assertEquals(1, DastDemo.planted("query"));
        assertEquals(5, DastDemo.planted("cookie", "header"));
    }
}
