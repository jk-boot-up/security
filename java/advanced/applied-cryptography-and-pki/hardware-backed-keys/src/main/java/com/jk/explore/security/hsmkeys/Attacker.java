package com.jk.explore.security.hsmkeys;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * A second process on the same machine (or on a thief's machine with a copy of the token directory). It is started by the demo as a child JVM
 * and prints key=value lines. Modes: nopin, guess (a list of PINs), sign (one PIN and a text to sign).
 */
public final class Attacker {

    private Attacker() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path library = Path.of(args[2]);
        Provider p = Security.getProvider("SunPKCS11").configure(args[1]);
        Security.addProvider(p);
        switch (mode) {
            case "nopin" -> nopin(library, p);
            case "guess" -> guess(p, List.of(args).subList(3, args.length));
            case "sign" -> sign(p, args[3], args[4]);
            default -> throw new IllegalArgumentException(mode);
        }
    }

    private static void nopin(Path library, Provider p) {
        System.out.println("login=" + TokenAccess.login(p, null).result());
        Raw raw = new Raw(library);
        long session = raw.openSession(raw.slotWithLabel(Lab.LABEL));
        System.out.println("visible_private_keys=" + Provisioning.privateKeys(raw, session).size());
    }

    private static void guess(Provider p, List<String> pins) {
        int tried = 0;
        int hits = 0;
        int locked = 0;
        long start = System.nanoTime();
        for (String pin : pins) {
            tried++;
            TokenAccess.LoginResult r = TokenAccess.login(p, pin).result();
            if (r == TokenAccess.LoginResult.OK) {
                hits++;
            } else if (r == TokenAccess.LoginResult.PIN_LOCKED) {
                locked++;
            }
        }
        System.out.println("millis=" + (System.nanoTime() - start) / 1_000_000);
        System.out.println("tried=" + tried);
        System.out.println("hits=" + hits);
        System.out.println("locked_answers=" + locked);
    }

    private static void sign(Provider p, String pin, String text) throws GeneralSecurityException {
        TokenAccess.Login login = TokenAccess.login(p, pin);
        System.out.println("login=" + login.result());
        if (login.result() != TokenAccess.LoginResult.OK) {
            return;
        }
        for (String alias : Collections.list(login.store().aliases())) {
            Key k = login.store().getKey(alias, null);
            if (k instanceof PrivateKey pk) {
                System.out.println("signature=" + HexFormat.of().formatHex(TokenAccess.sign(p, pk, text.getBytes(StandardCharsets.UTF_8))));
            }
        }
    }
}
