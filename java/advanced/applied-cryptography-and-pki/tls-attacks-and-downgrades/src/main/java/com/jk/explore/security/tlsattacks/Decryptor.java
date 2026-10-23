package com.jk.explore.security.tlsattacks;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.interfaces.RSAKey;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * What an attacker with a recording and the server's private key can read.
 * It follows TLS 1.2 with AES-128-GCM and a pre-master secret encrypted to the server's RSA key (RFC 5246 sections 7.4.7.1, 5 and 6.2.3.3).
 * When the key exchange is not an RSA one there is no pre-master secret to decrypt and it finds nothing.
 */
final class Decryptor {

    private Decryptor() {
    }

    /** The client's application data from a recorded connection, or nothing when the key does not open it. */
    static Optional<String> clientData(byte[] clientToServer, byte[] serverToClient, PrivateKey key) {
        try {
            List<TlsRecords.Rec> up = TlsRecords.records(clientToServer);
            List<byte[]> clientHs = TlsRecords.clearHandshake(up);
            List<byte[]> serverHs = TlsRecords.clearHandshake(TlsRecords.records(serverToClient));
            byte[] hello = TlsRecords.message(clientHs, TlsRecords.CLIENT_HELLO);
            byte[] serverHello = TlsRecords.message(serverHs, TlsRecords.SERVER_HELLO);
            byte[] exchange = TlsRecords.message(clientHs, TlsRecords.CLIENT_KEY_EXCHANGE);
            if (hello == null || serverHello == null || exchange == null) {
                return Optional.empty();
            }
            byte[] premaster = premaster(exchange, key);
            if (premaster == null) {
                return Optional.empty();
            }
            byte[] clientRandom = TlsRecords.random(hello);
            byte[] serverRandom = TlsRecords.random(serverHello);
            byte[] master = prf(premaster, "master secret", concat(clientRandom, serverRandom), 48);
            byte[] block = prf(master, "key expansion", concat(serverRandom, clientRandom), 40);
            byte[] clientKey = Arrays.copyOfRange(block, 0, 16);
            byte[] clientSalt = Arrays.copyOfRange(block, 32, 36);
            return readEncrypted(up, clientKey, clientSalt);
        } catch (GeneralSecurityException | RuntimeException cannot) {
            return Optional.empty();
        }
    }

    /** The pre-master secret, only if the key exchange message is an RSA-encrypted 48-byte secret that this key opens. */
    static byte[] premaster(byte[] clientKeyExchange, PrivateKey key) throws GeneralSecurityException {
        int modulusBytes = (((RSAKey) key).getModulus().bitLength() + 7) / 8;
        int declared = TlsRecords.u16(clientKeyExchange, 4);
        if (clientKeyExchange.length - 6 != modulusBytes || declared != modulusBytes) {
            return null;
        }
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.DECRYPT_MODE, key);
        byte[] secret = rsa.doFinal(clientKeyExchange, 6, modulusBytes);
        return secret.length == 48 && secret[0] == 3 && secret[1] == 3 ? secret : null;
    }

    private static Optional<String> readEncrypted(List<TlsRecords.Rec> up, byte[] key, byte[] salt) throws GeneralSecurityException {
        boolean encrypted = false;
        long sequence = 0;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for (TlsRecords.Rec r : up) {
            if (r.type() == TlsRecords.CHANGE_CIPHER_SPEC) {
                encrypted = true;
                continue;
            }
            if (!encrypted) {
                continue;
            }
            byte[] body = r.body();
            int plainLength = body.length - 8 - 16;
            if (plainLength < 0) {
                return Optional.empty();
            }
            byte[] nonce = concat(salt, Arrays.copyOfRange(body, 0, 8));
            byte[] aad = ByteBuffer.allocate(13).putLong(sequence).put((byte) r.type()).putShort((short) r.version()).putShort((short) plainLength).array();
            Cipher gcm = Cipher.getInstance("AES/GCM/NoPadding");
            gcm.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            gcm.updateAAD(aad);
            byte[] plain = gcm.doFinal(body, 8, body.length - 8);
            if (r.type() == TlsRecords.APPLICATION_DATA) {
                data.writeBytes(plain);
            }
            sequence++;
        }
        return data.size() == 0 ? Optional.empty() : Optional.of(data.toString(StandardCharsets.UTF_8));
    }

    /** The TLS 1.2 pseudo-random function with SHA-256 (RFC 5246 section 5). */
    static byte[] prf(byte[] secret, String label, byte[] seed, int length) throws GeneralSecurityException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(secret, "HmacSHA256"));
        byte[] labelSeed = concat(label.getBytes(StandardCharsets.US_ASCII), seed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] a = labelSeed;
        while (out.size() < length) {
            a = hmac.doFinal(a);
            out.writeBytes(hmac.doFinal(concat(a, labelSeed)));
        }
        return Arrays.copyOf(out.toByteArray(), length);
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] r = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
