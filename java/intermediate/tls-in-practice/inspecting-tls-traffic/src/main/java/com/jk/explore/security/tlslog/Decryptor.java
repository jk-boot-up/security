package com.jk.explore.security.tlslog;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.PSource;

/** What an analyst can do with a capture, depending on what they hold. Each way returns the readable text of the records, or an empty list. */
public final class Decryptor {

    private Decryptor() {
    }

    /** With nothing: the records are opaque. */
    public static List<String> withoutKeys(Capture c) {
        return List.of();
    }

    /** With a key log: the lines for this connection's client random give the traffic secrets directly. */
    public static List<String> withKeyLog(Capture c, String keyLog) {
        Optional<byte[]> client = KeyLog.find(keyLog, "CLIENT_TRAFFIC_SECRET_0", c.clientRandom());
        Optional<byte[]> server = KeyLog.find(keyLog, "SERVER_TRAFFIC_SECRET_0", c.clientRandom());
        return open(c, client.orElse(null), server.orElse(null));
    }

    /**
     * With the server's long-term private key. For a static RSA connection the key decrypts the premaster secret, and from it everything follows.
     * For an ephemeral connection the long-term key only signed the handshake, so it recovers nothing.
     */
    public static List<String> withServerLongTermKey(Capture c, PrivateKey serverKey) {
        if (c.ecdhe()) {
            return List.of();
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.DECRYPT_MODE, serverKey, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
            byte[] premaster = cipher.doFinal(c.clientKeyShare());
            Secrets s = Secrets.derive(premaster, Hkdf.sha256(c.clientRandom(), c.clientKeyShare(), c.serverRandom()));
            return open(c, s.client(), s.server());
        } catch (GeneralSecurityException e) {
            return List.of();
        }
    }

    private static List<String> open(Capture c, byte[] clientSecret, byte[] serverSecret) {
        List<String> out = new ArrayList<>();
        long clientSeq = 0;
        long serverSeq = 0;
        for (Packet p : c.packets()) {
            byte[] secret = p.fromClient() ? clientSecret : serverSecret;
            if (secret == null) {
                continue;
            }
            String text = Records.open(secret, p.fromClient() ? clientSeq : serverSeq, p.bytes());
            if (p.fromClient()) {
                clientSeq++;
            } else {
                serverSeq++;
            }
            if (text != null) {
                out.add((p.fromClient() ? "client: " : "server: ") + text);
            }
        }
        return out;
    }
}
