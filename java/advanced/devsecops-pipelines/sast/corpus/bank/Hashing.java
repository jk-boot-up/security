package bank;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Fingerprints of statements. */
public class Hashing {

    byte[] fingerprint(byte[] statement) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5").digest(statement); // VULN weak-hash
    }

    byte[] strong(byte[] statement) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(statement); // SAFE hash-strong
    }
}
