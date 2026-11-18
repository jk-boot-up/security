package com.jk.explore.security.hsmkeys;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/** Keys that never leave a token: a bank's payment-signing key on a SoftHSM2 token, used through PKCS#11, and what that does and does not protect. */
public class HardwareBackedKeysDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run(Path.of("build")));
    }

    static String yn(boolean b) {
        return b ? "yes" : "no";
    }

    /** An amount in cents as the bank writes it: 2500000000 is 25,000,000.00. */
    static String money(long cents) {
        return String.format("%,d.%02d", cents / 100, cents % 100);
    }

    static String lower(Enum<?> e) {
        return e.name().toLowerCase().replace('_', ' ');
    }

    public static String run(Path buildDir) throws Exception {
        StringBuilder out = new StringBuilder();
        Path scratch;
        try (Lab lab = new Lab(buildDir)) {
            scratch = lab.dir();
            Raw raw = new Raw(lab.library());
            long slot = raw.slotWithLabel(Lab.LABEL);

            // ---- ONE: the key ceremony
            out.append("ONE. The key ceremony.\n");
            out.append("  THE TOKEN:\n");
            out.append("    software  SoftHSM2 " + lab.version() + ", a token made of files\n");
            out.append("    used by   Java's SunPKCS11 provider and raw PKCS#11 calls\n");
            out.append("    PINs      a security officer PIN and a user PIN: " + yn(slot >= 0) + "\n");
            Provisioning.Provisioned made = Provisioning.rsaSigningKey(raw, Lab.LABEL, Lab.USER_PIN, Lab.KEY_ALIAS);
            Provider prov = lab.provider();
            TokenAccess.Login login = TokenAccess.login(prov, Lab.USER_PIN);
            KeyStore ks = login.store();
            PrivateKey key = (PrivateKey) ks.getKey(Lab.KEY_ALIAS, null);
            long rs = raw.openSession(slot);
            long handle = Provisioning.privateKeys(raw, rs).getFirst();
            out.append("  WHAT THE BANK ASKS FOR (C_GenerateKeyPair):\n");
            out.append("    key              RSA 2048, made inside the token\n");
            out.append("    CKA_SENSITIVE    true\n");
            out.append("    CKA_EXTRACTABLE  false\n");
            out.append("  ---\n");
            out.append("  THE KEY THE TOKEN MADE (attributes read back):\n");
            out.append("    CKA_SENSITIVE          " + yn(raw.flag(rs, handle, Raw.CKA_SENSITIVE)) + "\n");
            out.append("    CKA_EXTRACTABLE        " + yn(raw.flag(rs, handle, Raw.CKA_EXTRACTABLE)) + "\n");
            out.append("    CKA_NEVER_EXTRACTABLE  " + yn(raw.flag(rs, handle, Raw.CKA_NEVER_EXTRACTABLE)) + "\n");
            out.append("    CKA_ALWAYS_SENSITIVE   " + yn(raw.flag(rs, handle, Raw.CKA_ALWAYS_SENSITIVE)) + "\n");
            Certificate cert = ks.getCertificate(Lab.KEY_ALIAS);
            cert.verify(cert.getPublicKey());
            out.append("    certificate signed by the token key, verifies  yes\n");
            out.append("    its public key = the exported public key       " + yn(made.publicKey().equals(cert.getPublicKey())) + "\n");

            // ---- TWO: five ways out, and signing
            out.append("TWO. Five ways out, and signing.\n");
            out.append("  FIVE WAYS TO GET THE PRIVATE KEY OUT:\n");
            out.append("    1  read the private exponent (C_GetAttributeValue)\n");
            out.append("    2  ask Java for its bytes: getEncoded()\n");
            out.append("    3  wrap it under the bank's wrapping key (C_WrapKey)\n");
            out.append("    4  wrap it under a key the attacker can read\n");
            out.append("    5  export it into a PKCS12 file from Java\n");
            int tried = 5;
            int got = 0;
            Raw.Answer exp = raw.attribute(rs, handle, Raw.CKA_PRIVATE_EXPONENT);
            got += exp.ok() ? 1 : 0;
            got += key.getEncoded() != null ? 1 : 0;
            long bankWrap = raw.generateAes(rs, "bank-wrap-key", true, false);
            Raw.Answer w1 = raw.wrap(rs, Raw.CKM_AES_KEY_WRAP_PAD, bankWrap, handle);
            got += w1.ok() ? 1 : 0;
            long evilWrap = raw.generateAes(rs, "attacker-wrap-key", false, true);
            Raw.Answer w2 = raw.wrap(rs, Raw.CKM_AES_KEY_WRAP_PAD, evilWrap, handle);
            got += w2.ok() ? 1 : 0;
            boolean exported;
            try {
                KeyStore p12 = KeyStore.getInstance("PKCS12");
                p12.load(null, null);
                p12.setKeyEntry("copy", key, "x".toCharArray(), new Certificate[] {cert});
                ByteArrayCounter c = new ByteArrayCounter();
                p12.store(c, "x".toCharArray());
                exported = c.count > 0;
            } catch (GeneralSecurityException | IOException | RuntimeException refused) {
                exported = false;
            }
            got += exported ? 1 : 0;
            out.append("  THE TOKEN'S ANSWERS:\n");
            out.append("    1  " + exp.name() + "\n");
            out.append("    2  " + (key.getEncoded() == null ? "null" : "bytes") + " (getFormat(): " + key.getFormat() + ")\n");
            out.append("    3  " + w1.name() + "\n");
            out.append("    4  " + w2.name() + "\n");
            out.append("    5  " + (exported ? "written" : "refused") + "\n");
            out.append("    private key obtained  " + got + " of " + tried + "\n");
            out.append("  ---\n");
            byte[] first = PaymentFile.content("PAY-2026-001", 4, 1_250_000L);
            out.append("  ONE PAYMENT FILE, BEFORE SIGNING (" + first.length + " bytes):\n");
            for (String line : new String(first, StandardCharsets.UTF_8).split("\n")) {
                out.append("    " + line + "\n");
            }
            int signedOk = 0;
            int tamperedOk = 0;
            int sigLen = 0;
            for (int i = 1; i <= 5; i++) {
                byte[] file = PaymentFile.content("PAY-2026-00" + i, 3 + i, 1_250_000L * i);
                byte[] sig = TokenAccess.sign(prov, key, file);
                sigLen = sig.length;
                signedOk += TokenAccess.verify(made.publicKey(), file, sig) ? 1 : 0;
                byte[] changed = file.clone();
                changed[changed.length - 3] ^= 1;
                tamperedOk += TokenAccess.verify(made.publicKey(), changed, sig) ? 1 : 0;
            }
            out.append("  AFTER SIGNING ON THE TOKEN (5 files, SHA256withRSA):\n");
            out.append("    signature size  " + sigLen + " bytes = 2048 bits / 8\n");
            out.append("    verified with the exported public key  " + signedOk + " of 5\n");
            out.append("    the same signatures, one bit changed   " + tamperedOk + " of 5\n");

            // ---- THREE: the PIN, the key as a file, a copy of the token
            out.append("THREE. The PIN, a key in a file, and a copied token.\n");
            TokenAccess.logout(prov);
            raw.logout(rs);
            raw.closeSession(rs);
            TokenAccess.Login noPin = TokenAccess.login(prov, null);
            long anon = raw.openSession(slot);
            int before = Provisioning.privateKeys(raw, anon).size();
            raw.login(anon, Lab.USER_PIN);
            int after = Provisioning.privateKeys(raw, anon).size();
            raw.logout(anon);
            raw.closeSession(anon);
            out.append("  WITHOUT THE PIN, THEN WITH IT:\n");
            out.append("    KeyStore opened with no PIN  " + (noPin.result() == TokenAccess.LoginResult.OK ? "yes" : "refused") + "\n");
            out.append("    private keys a session sees  " + before + " before login, " + after + " after\n");
            int wrong = 0;
            boolean everLocked = false;
            for (int i = 0; i < 12; i++) {
                TokenAccess.LoginResult r = TokenAccess.login(prov, "000000").result();
                wrong += r == TokenAccess.LoginResult.PIN_INCORRECT ? 1 : 0;
                everLocked |= r == TokenAccess.LoginResult.PIN_LOCKED;
            }
            out.append("  12 WRONG PINS IN A ROW, THEN THE RIGHT ONE:\n");
            out.append("    refused       " + wrong + " of 12 (CKR_PIN_INCORRECT)\n");
            out.append("    token locked  " + yn(everLocked) + " (SoftHSM2 has no lockout)\n");
            out.append("    right PIN     " + (TokenAccess.login(prov, Lab.USER_PIN).result() == TokenAccess.LoginResult.OK ? "accepted" : "refused") + "\n");
            TokenAccess.logout(prov);
            int[] asked = {0};
            PinGuard guard = new PinGuard(3, pin -> {
                asked[0]++;
                return TokenAccess.login(prov, pin).result() == TokenAccess.LoginResult.OK;
            });
            List<String> seq = new ArrayList<>();
            for (String pin : List.of("111111", "222222", "333333", Lab.USER_PIN)) {
                seq.add(lower(guard.attempt(pin)));
            }
            out.append("  THE BANK'S OWN LOCKOUT (3 wrong PINs, then the right one):\n");
            out.append("    answers           " + String.join(", ", seq) + "\n");
            out.append("    token asked       " + asked[0] + " of 4 times\n");
            boolean direct = TokenAccess.login(prov, Lab.USER_PIN).result() == TokenAccess.LoginResult.OK;
            out.append("    PIN straight to the provider  " + (direct ? "accepted" : "refused") + "\n");
            TokenAccess.logout(prov);
            guard.reset();
            out.append("    after an officer resets it    " + lower(guard.attempt(Lab.USER_PIN)) + "\n");
            TokenAccess.logout(prov);
            out.append("  ---\n");

            Path soft = lab.dir().resolve("payments-key.p12");
            String softPassword = String.join("", "bank", "2026", "!");
            Certificate softCert = SoftKey.create(soft, softPassword);
            Path stolen = lab.dir().resolve("stolen-keystore.p12");
            Files.copy(soft, stolen);
            List<String> dictionary = List.of("password", "123456", "letmein", "qwerty2026", "bank1234", softPassword, "welcome1", "admin");
            int rank = SoftKey.crack(stolen, dictionary);
            PrivateKey stolenKey = SoftKey.open(stolen, softPassword);
            byte[] forgedByThief = PaymentFile.content("FORGED-1", 40, 9_900_000_000L);
            Signature thief = Signature.getInstance("SHA256withRSA");
            thief.initSign(stolenKey);
            thief.update(forgedByThief);
            out.append("  THE SAME KIND OF KEY IN A PKCS12 FILE, COPIED:\n");
            out.append("    password found     guess " + rank + " of " + dictionary.size() + ", offline, no limit, no log\n");
            out.append("    getEncoded()       " + (stolenKey.getEncoded() != null ? "bytes" : "null") + "\n");
            out.append("    forged file signed on the thief's machine verifies  "
                    + yn(TokenAccess.verify(softCert.getPublicKey(), forgedByThief, thief.sign())) + "\n");

            Path copyDir = lab.dir().resolve("stolen-token");
            copyTree(lab.tokens(), copyDir.resolve("tokens"));
            Path copyConf = copyDir.resolve("softhsm2.conf");
            Lab.writeSoftHsmConf(copyConf, copyDir.resolve("tokens"));
            Path copyCfg = copyDir.resolve("pkcs11.cfg");
            Files.writeString(copyCfg, Lab.providerConfigText(lab.library(), "stolen", Lab.KEY_ALIAS));
            List<String> guesses = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                guesses.add(String.format("%06d", 100_000 + i * 3_001 % 900_000));
            }
            guesses.remove(Lab.USER_PIN);
            Map<String, String> g = child(copyConf, List.of("guess", copyCfg.toString(), lab.library().toString()), guesses);
            double perGuess = Long.parseLong(g.get("fastest_micros")) / 1_000_000.0;
            out.append("  A COPY OF THE TOKEN DIRECTORY, ON ANOTHER MACHINE:\n");
            out.append("    wrong PIN guesses  " + g.get("tried") + ", hits " + g.get("hits") + ", locked " + yn(!"0".equals(g.get("locked_answers"))) + "\n");
            out.append("    each guess < 86.4 ms  " + yn(perGuess < 0.0864) + ": 1,000,000 PINs in < 1 day\n");
            byte[] forgedFromCopy = PaymentFile.content("FORGED-2", 40, 9_800_000_000L);
            Map<String, String> s = child(copyConf, List.of("sign", copyCfg.toString(), lab.library().toString(), Lab.USER_PIN,
                    new String(forgedFromCopy, StandardCharsets.UTF_8)), List.of());
            out.append("    with the PIN: signs " + yn("OK".equals(s.get("login")) && s.get("signature") != null)
                    + ", verifies with the bank's key "
                    + yn(s.get("signature") != null && TokenAccess.verify(made.publicKey(), forgedFromCopy, HexFormat.of().parseHex(s.get("signature")))) + "\n");
            out.append("    a hardware token is built so this fails (not measured)\n");

            // ---- FOUR: limits and audit kept by the bank's code
            out.append("FOUR. Rate, approvers and audit.\n");
            PrivateKey serviceKey = (PrivateKey) TokenAccess.login(prov, Lab.USER_PIN).store().getKey(Lab.KEY_ALIAS, null);
            long dual = 10_000_000L;
            SigningService service = new SigningService(data -> TokenAccess.sign(prov, serviceKey, data), 8, 60, dual);
            out.append("  THE BANK'S RULES:\n");
            out.append("    rate       at most 8 signatures in 60 seconds\n");
            out.append("    approvers  2 people other than the requester, from " + money(dual) + "\n");
            out.append("  14 REQUESTS FROM ALICE, ONE EVERY 4 SECONDS:\n");
            out.append("    0-13       " + money(150_000L) + " each, except 4, 9 and 13\n");
            out.append("    4, 9, 13   " + money(25_000_000L) + ", approved by carol+dave, carol, alice+bob\n");
            Map<SigningService.Decision, Integer> counts = new LinkedHashMap<>();
            StringBuilder decisions = new StringBuilder();
            int verified = 0;
            for (int i = 0; i < 14; i++) {
                boolean big = i == 4 || i == 9 || i == 13;
                List<String> approvers = i == 4 ? List.of("carol", "dave") : i == 9 ? List.of("carol") : i == 13 ? List.of("alice", "bob") : List.of();
                byte[] content = PaymentFile.content("BATCH-" + i, 5, big ? 25_000_000L : 150_000L);
                SigningService.Result r = service.submit(new SigningService.Request("BATCH-" + i, "alice", approvers, big ? 25_000_000L : 150_000L, i * 4L, content));
                counts.merge(r.decision(), 1, Integer::sum);
                decisions.append(String.format("%3s", switch (r.decision()) {
                    case SIGNED -> "S";
                    case RATE_LIMITED -> "R";
                    case NEEDS_TWO_APPROVERS -> "A";
                }));
                if (r.decision() == SigningService.Decision.SIGNED) {
                    verified += TokenAccess.verify(made.publicKey(), content, r.signature()) ? 1 : 0;
                }
            }
            int signed = counts.getOrDefault(SigningService.Decision.SIGNED, 0);
            int rate = counts.getOrDefault(SigningService.Decision.RATE_LIMITED, 0);
            int appr = counts.getOrDefault(SigningService.Decision.NEEDS_TWO_APPROVERS, 0);
            out.append("  THE DECISIONS (S signed, R rate, A approvers):\n");
            StringBuilder numbers = new StringBuilder();
            for (int i = 0; i < 14; i++) {
                numbers.append(String.format("%3d", i));
            }
            out.append("    request" + numbers + "\n");
            out.append("    answer " + decisions + "\n");
            out.append("    signed " + signed + " + rate " + rate + " + approvers " + appr + " = " + (signed + rate + appr) + "\n");
            out.append("    signatures that verify  " + verified + " of " + signed + "\n");
            out.append("  ---\n");
            AuditLog audit = service.audit();
            List<AuditLog.Entry> entries = audit.entries();
            out.append("  HOW AN AUDIT ENTRY IS MADE (hashes cut to 12 hex digits):\n");
            out.append("    hash = SHA-256(seq|event|file|content hash|previous)\n");
            for (AuditLog.Entry e : entries.subList(0, 3)) {
                out.append(String.format("    %d %-6s %-7s previous %-12.12s hash %.12s%n", e.seq(), e.event(), e.file(), e.previous(), e.hash()));
            }
            List<AuditLog.Entry> edited = new ArrayList<>(entries);
            AuditLog.Entry e3 = edited.get(3);
            edited.set(3, new AuditLog.Entry(e3.seq(), e3.event(), "BATCH-X", e3.contentHash(), e3.previous(), e3.hash()));
            List<AuditLog.Entry> removed = new ArrayList<>(entries);
            removed.remove(5);
            out.append("  THE CHAIN CHECKED:\n");
            out.append("    as written, " + entries.size() + " entries  " + yn(audit.verify()) + "\n");
            out.append("    entry 4 edited       " + yn(AuditLog.verify(edited)) + "\n");
            out.append("    entry 6 removed      " + yn(AuditLog.verify(removed)) + "\n");

            // ---- FIVE: a compromised host
            out.append("FIVE. A compromised host.\n");
            out.append("  FIVE THINGS AN ATTACKER ON THE HOST TRIES:\n");
            out.append("    1  another process, no PIN, opens the token\n");
            out.append("    2  asks the bank's service to sign " + money(9_999_999_900L) + "\n");
            out.append("    3  code inside the application, no PIN: 500 forged files\n");
            out.append("    4  the same code, after the application logs out\n");
            out.append("    5  another process that has learned the PIN\n");
            Path lib = lab.library();
            Path conf = lab.softhsmConf();
            Map<String, String> np = child(conf, List.of("nopin", lab.providerConfig().toString(), lib.toString()), List.of());
            byte[] drain = PaymentFile.content("DRAIN", 200, 9_999_999_900L);
            SigningService.Result policy = service.submit(new SigningService.Request("DRAIN", "mallory", List.of(), 9_999_999_900L, 70, drain));
            int auditBefore = audit.entries().size();
            int forged = 0;
            for (int i = 0; i < 500; i++) {
                byte[] f = PaymentFile.content("FORGED-LOOP-" + i, 10, 9_999_999_900L);
                byte[] sig = Malware.signWithoutPin(f);
                forged += sig != null && TokenAccess.verify(made.publicKey(), f, sig) ? 1 : 0;
            }
            int added = audit.entries().size() - auditBefore;
            PrivateKey held = Malware.grabKey();
            TokenAccess.logout(prov);
            byte[] late = PaymentFile.content("FORGED-LATE", 10, 9_999_999_900L);
            Map<String, String> stolenPin = child(conf, List.of("sign", lab.providerConfig().toString(), lib.toString(), Lab.USER_PIN, "FORGED-PIN"), List.of());
            out.append("  THE RESULT:\n");
            out.append("    1  login " + ("NO_PIN_GIVEN".equals(np.get("login")) ? "refused" : np.get("login")) + ", private keys seen " + np.get("visible_private_keys") + "\n");
            out.append("    2  refused: " + lower(policy.decision()) + "\n");
            out.append("    3  " + forged + " of 500 signed, all verify with the bank's key\n");
            out.append("       audit entries added " + added + ", chain still verifies " + yn(audit.verify()) + "\n");
            out.append("    4  old key reference signs " + yn(Malware.signWithKey(held, late) != null) + ", fresh KeyStore signs "
                    + yn(Malware.signWithoutPin(late) != null) + "\n");
            out.append("    5  signs " + yn(stolenPin.get("signature") != null) + ", verifies "
                    + yn(stolenPin.get("signature") != null && TokenAccess.verify(made.publicKey(), "FORGED-PIN".getBytes(StandardCharsets.UTF_8),
                    HexFormat.of().parseHex(stolenPin.get("signature")))) + "\n");

            // ---- SIX: backup by wrapping, and the limits
            out.append("SIX. A key that can be backed up, and the limits.\n");
            long ws = raw.openSession(slot);
            raw.login(ws, Lab.USER_PIN);
            long wrapKey = raw.generateAes(ws, "backup-wrap-key", true, false);
            long[] release = raw.generateEcPair(ws, "release-key", true);
            PublicKey releasePub = ecPublic(raw.attribute(ws, release[0], Raw.CKA_EC_POINT).bytes());
            byte[] releaseFile = "release payments-app 4.2".getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(releaseFile);
            Raw.Answer blob = raw.wrap(ws, Raw.CKM_AES_KEY_WRAP_PAD, wrapKey, release[1]);
            raw.destroy(ws, release[1]);
            Raw.Answer gone = raw.signEcdsa(ws, release[1], digest);
            Raw.Answer back = raw.unwrapEc(ws, Raw.CKM_AES_KEY_WRAP_PAD, wrapKey, blob.bytes(), "release-key-restored");
            Raw.Answer again = raw.signEcdsa(ws, Raw.handle(back), digest);
            out.append("  THE RELEASE-SIGNING KEY, EXTRACTABLE ON PURPOSE:\n");
            out.append("    key      EC P-256, made on the token, CKA_EXTRACTABLE true\n");
            out.append("    wrapped  under a sensitive AES token key (key wrap, padded)\n");
            out.append("  THE BACKUP CYCLE:\n");
            out.append("    wrap                " + blob.name() + ", a blob of " + blob.bytes().length + " bytes\n");
            out.append("    destroy, then sign  " + gone.name() + "\n");
            out.append("    unwrap the blob     " + back.name() + ", signs " + yn(again.ok()) + ", verifies "
                    + yn(again.ok() && verifyEc(releasePub, releaseFile, again.bytes())) + "\n");
            long[] second = raw.generateEcPair(ws, "release-key-2", true);
            PublicKey secondPub = ecPublic(raw.attribute(ws, second[0], Raw.CKA_EC_POINT).bytes());
            long mine = raw.generateAes(ws, "attacker-wrap-2", false, true);
            Raw.Answer evilBlob = raw.wrap(ws, Raw.CKM_AES_KEY_WRAP_PAD, mine, second[1]);
            Raw.Answer aesValue = raw.attribute(ws, mine, Raw.CKA_VALUE);
            boolean stolenEc = false;
            int keyBytes = 0;
            if (evilBlob.ok() && aesValue.ok()) {
                Cipher open = Cipher.getInstance("AES/KWP/NoPadding");
                open.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesValue.bytes(), "AES"));
                keyBytes = open.doFinal(evilBlob.bytes()).length;
                Cipher unwrap = Cipher.getInstance("AES/KWP/NoPadding");
                unwrap.init(Cipher.UNWRAP_MODE, new SecretKeySpec(aesValue.bytes(), "AES"));
                PrivateKey ecKey = (PrivateKey) unwrap.unwrap(evilBlob.bytes(), "EC", Cipher.PRIVATE_KEY);
                Signature sg = Signature.getInstance("SHA256withECDSAinP1363Format");
                sg.initSign(ecKey);
                sg.update(releaseFile);
                stolenEc = verifyEc(secondPub, releaseFile, sg.sign());
            }
            out.append("  AN ATTACKER WITH A LOGGED-IN SESSION:\n");
            out.append("    wraps it under a key they can read  " + evilBlob.name() + "\n");
            out.append(String.format("    opens it in Java: %d key bytes, padded to %d, + 8 = %d%n", keyBytes, (keyBytes + 7) / 8 * 8, evilBlob.bytes().length));
            out.append("    a forged release signature verifies  " + yn(stolenEc) + "\n");
            raw.destroy(ws, wrapKey);
            Raw.Answer lost = raw.unwrapEc(ws, Raw.CKM_AES_KEY_WRAP_PAD, wrapKey, blob.bytes(), "never");
            out.append("  THE WRAPPING KEY LOST WITH THE TOKEN:\n");
            out.append("    restores the key  " + yn(lost.ok()) + " (" + lost.name() + ")\n");
            raw.logout(ws);
            raw.closeSession(ws);
            out.append("  ---\n");
            out.append("  WHAT THE TOKEN PROTECTS (ways to copy a key out that worked):\n");
            out.append("    non-extractable token key  " + got + " of " + tried + "\n");
            out.append("    extractable token key      " + (stolenEc ? 1 : 0) + " of 1\n");
            out.append("    key in a file              " + (stolenKey.getEncoded() != null ? 1 : 0) + " of 1\n");
            out.append("  WHAT IT DOES NOT PROTECT:\n");
            out.append("    use while logged in, by anyone in the process  " + yn(forged == 500) + "\n");
            out.append("    rate, approvers, audit bypassed from inside    " + yn(policy.decision() != SigningService.Decision.SIGNED && forged == 500) + "\n");
        }
        out.append("  CLEAN UP:\n");
        out.append("    token, keystore file and copies removed  " + yn(Files.notExists(scratch)) + "\n");
        return out.toString();
    }

    private static final class ByteArrayCounter extends OutputStream {

        int count;

        @Override
        public void write(int b) {
            count++;
        }
    }

    static PublicKey ecPublic(byte[] ecPointAttribute) throws GeneralSecurityException {
        byte[] point = Arrays.copyOfRange(ecPointAttribute, 2, ecPointAttribute.length);
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec spec = params.getParameterSpec(ECParameterSpec.class);
        ECPoint p = new ECPoint(new BigInteger(1, Arrays.copyOfRange(point, 1, 33)), new BigInteger(1, Arrays.copyOfRange(point, 33, 65)));
        return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(p, spec));
    }

    static boolean verifyEc(PublicKey key, byte[] data, byte[] rawSignature) throws GeneralSecurityException {
        Signature v = Signature.getInstance("SHA256withECDSAinP1363Format");
        v.initVerify(key);
        v.update(data);
        return v.verify(rawSignature);
    }

    static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> s = Files.walk(from)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(p, target);
                }
            }
        }
    }

    /** Runs the Attacker class in a child JVM with its own SoftHSM configuration and reads its key=value lines. */
    static Map<String, String> child(Path softhsmConf, List<String> head, List<String> tail) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "--enable-native-access=ALL-UNNAMED",
                "-cp", System.getProperty("java.class.path"), Attacker.class.getName()));
        cmd.addAll(head);
        cmd.addAll(tail);
        Sh.Result r = Sh.run(null, Map.of("SOFTHSM2_CONF", softhsmConf.toString()), "", cmd);
        Map<String, String> m = new LinkedHashMap<>();
        for (String line : r.output().split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0 && !line.contains(" ")) {
                m.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        if (!r.ok() && m.isEmpty()) {
            throw new IllegalStateException("child failed: " + r.output());
        }
        return m;
    }
}
