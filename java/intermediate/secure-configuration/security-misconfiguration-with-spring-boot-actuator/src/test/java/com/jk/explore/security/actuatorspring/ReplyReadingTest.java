package com.jk.explore.security.actuatorspring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** The demo reads values out of the actuator's JSON replies; these tests pin how, without starting a store. */
class ReplyReadingTest {

    @Test
    void readsTheFirstValueOfAnEnvReply() {
        assertEquals("pay-live", ActuatorSpringDemo.value("{\"property\":{\"source\":\"x\",\"value\":\"pay-live\"},\"value\":\"other\"}"));
        assertEquals("(none)", ActuatorSpringDemo.value("{}"));
    }

    @Test
    void listsEndpointsWithoutSelfOrTemplates() {
        String links = "{\"_links\":{\"self\":{\"href\":\"a\"},\"health\":{\"href\":\"b\"},\"health-path\":{\"href\":\"c\"},\"env\":{\"href\":\"d\"}}}";
        assertEquals(Set.of("env", "health"), ActuatorSpringDemo.endpoints(links));
    }

    @Test
    void listsTheTopLevelHealthComponentsOnly() {
        String health = "{\"components\":{\"diskSpace\":{\"status\":\"UP\",\"details\":{\"free\":1}},\"ping\":{\"status\":\"UP\"}},\"status\":\"UP\"}";
        assertEquals(Set.of("diskSpace", "ping"), ActuatorSpringDemo.components(health));
        assertEquals(Set.of(), ActuatorSpringDemo.components("{\"status\":\"UP\"}"));
    }

    @Test
    void readsTheConfiguredLevel() {
        assertEquals("OFF", ActuatorSpringDemo.level("{\"configuredLevel\":\"OFF\",\"effectiveLevel\":\"OFF\"}"));
    }
}
