package com.jk.explore.security.tlsattacks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class TlsRecordsTest {

    /** A ClientHello message (type 1) with a given version and compression methods, wrapped in one record. */
    static byte[] clientHelloRecord(int version, int... compression) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(version >> 8);
        body.write(version);
        body.writeBytes(new byte[32]);
        body.write(0);                       // empty session id
        body.write(0);
        body.write(2);                       // one cipher suite
        body.write(0x00);
        body.write(0x9c);
        body.write(compression.length);
        for (int c : compression) {
            body.write(c);
        }
        byte[] b = body.toByteArray();
        ByteArrayOutputStream msg = new ByteArrayOutputStream();
        msg.write(1);
        msg.write(0);
        msg.write(b.length >> 8);
        msg.write(b.length);
        msg.writeBytes(b);
        return record(22, 0x0301, msg.toByteArray());
    }

    static byte[] record(int type, int version, byte[] body) {
        ByteArrayOutputStream r = new ByteArrayOutputStream();
        r.write(type);
        r.write(version >> 8);
        r.write(version);
        r.write(body.length >> 8);
        r.write(body.length);
        r.writeBytes(body);
        return r.toByteArray();
    }

    @Test
    void clientHelloVersionAndCompressionAreRead() {
        byte[] rec = clientHelloRecord(0x0303, 0);
        assertEquals(0x0303, TlsRecords.clientHelloVersion(rec));
        byte[] hello = TlsRecords.message(TlsRecords.clearHandshake(TlsRecords.records(rec)), TlsRecords.CLIENT_HELLO);
        assertNotNull(hello);
        assertEquals(List.of(0), TlsRecords.compressionMethods(hello));
        byte[] withDeflate = clientHelloRecord(0x0303, 1, 0);
        byte[] hello2 = TlsRecords.message(TlsRecords.clearHandshake(TlsRecords.records(withDeflate)), TlsRecords.CLIENT_HELLO);
        assertEquals(List.of(1, 0), TlsRecords.compressionMethods(hello2));
    }

    @Test
    void aRecordCutOffAtTheEndIsIgnored() {
        byte[] whole = record(23, 0x0303, new byte[] {1, 2, 3});
        byte[] two = new byte[whole.length * 2 - 2];
        System.arraycopy(whole, 0, two, 0, whole.length);
        System.arraycopy(whole, 0, two, whole.length, whole.length - 2);
        List<TlsRecords.Rec> recs = TlsRecords.records(two);
        assertEquals(1, recs.size());
        assertArrayEquals(new byte[] {1, 2, 3}, recs.get(0).body());
    }

    @Test
    void notAClientHelloHasNoVersion() {
        assertEquals(-1, TlsRecords.clientHelloVersion(record(23, 0x0303, new byte[20])));
    }

    @Test
    void theDowngradeSentinelIsRecognised() {
        byte[] hello = new byte[80];
        byte[] marker = {0x44, 0x4F, 0x57, 0x4E, 0x47, 0x52, 0x44, 0x01};
        System.arraycopy(marker, 0, hello, 6 + 24, 8);
        assertEquals("TLS 1.2", TlsRecords.downgradeSentinel(hello));
        hello[6 + 31] = 0;
        assertEquals("TLS 1.1 or lower", TlsRecords.downgradeSentinel(hello));
        hello[6 + 31] = 7;
        assertEquals("none", TlsRecords.downgradeSentinel(hello));
    }
}
