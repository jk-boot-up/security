package com.jk.explore.security.runtimesec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AuditTest {

    static final String CID = "a".repeat(64);
    static final String CG = "CG=0::/../node/kubelet.slice/kubelet-kubepods-pod1.slice/cri-containerd-" + CID + ".scope";

    static String syscall(String id, String key, String comm, String success) {
        return "type=SYSCALL msg=audit(1792743287.839:" + id + "): arch=c00000b7 syscall=56 success=" + success
                + " exit=3 a2=241 items=2 ppid=1 pid=2 comm=\"" + comm + "\" exe=\"/bin/busybox\" key=\"" + key + "\"ARCH=aarch64 " + CG;
    }

    @Test
    void anIPv4SocketAddressIsDecodedFromHex() {
        assertEquals("10.0.0.2:4444", Audit.address("0200115C0A0000020000000000000000"));
    }

    @Test
    void otherAddressFamiliesAreNotDecoded() {
        assertNull(Audit.address("01002F72756E2F6E7363642E736F636B"));
        assertNull(Audit.address("0A"));
    }

    @Test
    void theContainerIsFoundInTheCgroupPath() {
        assertEquals(CID, Audit.containerId(CG));
        assertNull(Audit.containerId("0::/init.scope"));
    }

    @Test
    void recordsOfOneEventAreGroupedWhateverTheirOrder() {
        String log = String.join("\n",
                "type=PATH msg=audit(1792743287.839:7): item=1 name=\"/usr/bin/miner\" inode=1 nametype=CREATE",
                syscall("7", "write", "tee", "yes"),
                "type=PATH msg=audit(1792743287.839:7): item=0 name=\"/usr/bin/\" inode=1 nametype=PARENT");
        List<Event> events = Audit.parse(log);
        assertEquals(1, events.size());
        assertEquals(List.of("/usr/bin/miner"), events.get(0).paths());
        assertEquals("write", events.get(0).key());
        assertEquals(CID, events.get(0).containerId());
    }

    @Test
    void aRefusedOpenNamesOnlyTheFolder() {
        String log = syscall("8", "write", "tee", "no") + "\ntype=PATH msg=audit(1792743287.839:8): item=0 name=\"/usr/bin/\" inode=1 nametype=PARENT";
        Event e = Audit.parse(log).get(0);
        assertEquals(List.of("/usr/bin/"), e.paths());
        assertTrue(!e.success());
    }

    @Test
    void aRelativeNameIsResolvedAgainstTheWorkingDirectory() {
        String log = syscall("9", "write", "sh", "yes") + "\ntype=CWD msg=audit(1792743287.839:9): cwd=\"/etc\"\ntype=PATH msg=audit(1792743287.839:9): item=0 name=\"passwd\" nametype=NORMAL";
        assertEquals(List.of("/etc/passwd"), Audit.parse(log).get(0).paths());
    }

    @Test
    void recordsWithoutASyscallRecordAreDropped() {
        assertTrue(Audit.parse("type=PATH msg=audit(1792743287.839:5): item=0 name=\"/x\" nametype=NORMAL").isEmpty());
    }

    @Test
    void aConnectEventCarriesItsAddress() {
        String log = syscall("10", "connect", "nc", "yes") + "\ntype=SOCKADDR msg=audit(1792743287.839:10): saddr=0200115C0A0000020000000000000000";
        Event e = Audit.parse(log).get(0);
        assertEquals("10.0.0.2:4444", e.address());
        assertEquals("10.0.0.2", e.ip());
    }
}
