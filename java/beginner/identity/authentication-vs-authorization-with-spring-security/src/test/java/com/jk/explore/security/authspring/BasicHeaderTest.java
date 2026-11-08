package com.jk.explore.security.authspring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The Basic header is the username and password, joined and written in Base64, and anyone can turn it back. */
class BasicHeaderTest {

    @Test
    void theHeaderIsTheJoinedPairInBase64() {
        assertEquals("ben:ben-pass", BasicHeader.joined("ben", "ben-pass"));
        assertEquals("Basic YmVuOmJlbi1wYXNz", BasicHeader.header("ben", "ben-pass"));
    }

    @Test
    void anyoneCanDecodeIt() {
        assertEquals("ben:ben-pass", BasicHeader.decode("YmVuOmJlbi1wYXNz"));
    }

    @Test
    void everyThreeBytesBecomeFourLettersAndTheStepsAddUpToTheWhole() {
        List<String[]> steps = BasicHeader.steps("ben:ben-pass");
        assertEquals(4, steps.size());
        assertEquals("ben", steps.get(0)[0]);
        assertEquals("YmVu", steps.get(0)[1]);
        assertEquals("YmVuOmJlbi1wYXNz", String.join("", steps.stream().map(s -> s[1]).toList()));
    }

    @Test
    void theShopSendsExactlyThisHeader() {
        try (Shop shop = new Shop(LoginOnlyConfig.class)) {
            assertEquals(200, shop.as("ben", org.springframework.http.HttpMethod.GET, "/orders/O-2").status());
        }
    }
}
