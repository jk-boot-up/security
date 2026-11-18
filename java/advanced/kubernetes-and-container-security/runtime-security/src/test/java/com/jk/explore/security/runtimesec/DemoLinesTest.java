package com.jk.explore.security.runtimesec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The teaching steps and the demo's lines, with no tools. */
class DemoLinesTest {

    @Test
    void theSocketAddressIsReadStepByStep() {
        assertEquals(List.of(
                "saddr      0200 115C, then 4 bytes of address",
                "family     02 00, low byte first: 2 = IPv4",
                "port       11 5C, high byte first: 0x115C = 4444"), Audit.explain("0200115C0A0000020000000000000000"));
    }

    @Test
    void theExplainedPortIsThePortTheParserReads() {
        String hex = "020015380A6000010000000000000000";
        assertEquals("10.96.0.1:5432", Audit.address(hex));
        assertEquals("port       15 38, high byte first: 0x1538 = 5432", Audit.explain(hex).get(2));
    }

    @Test
    void theRawAddressOfOneEventIsFoundByItsNumber() {
        String log = "type=SOCKADDR msg=audit(1.2:7): saddr=AAAA\ntype=SOCKADDR msg=audit(1.2:8): saddr=0200115C0A000002";
        assertEquals("0200115C0A000002", Audit.saddr(log, "8"));
    }

    @Test
    void theRuleNamesAreReadFromTheFalcoFile() throws Exception {
        assertEquals(List.of("Shell in a bank container", "Unexpected write in a bank container", "Unexpected outbound connection from a bank container"),
                Falco.ruleNames(Files.readString(Path.of("rules", "bank-runtime-rules.yaml"))));
        assertEquals("0.45.1", Falco.version());
    }

    @Test
    void anAlertIsPrintedAsRulePodAndDetailWithTheAddressNamed() {
        Set<Alert> alerts = new LinkedHashSet<>(List.of(new Alert(Rules.CONNECT, "payments-api", "172.18.0.9:4444")));
        assertEquals(List.of("rule       Unexpected outbound connection", "pod        payments-api", "detail     the outside server:4444"),
                RuntimeSecurityDemo.alerts(alerts, "172.18.0.9"));
        assertEquals(List.of("none"), RuntimeSecurityDemo.alerts(Set.of(), "172.18.0.9"));
    }
}
