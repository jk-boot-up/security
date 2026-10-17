package com.jk.explore.security.asymmetric;

import java.util.ArrayList;
import java.util.List;

/**
 * A network line with an eavesdropper on it. Everything sent is delivered, and a copy goes to Eve, who sees every byte
 * but changes nothing. It stands for any shared network, from a coffee shop to the open internet.
 */
public class Wire {

    private final List<byte[]> seenByEve = new ArrayList<>();

    /** Sends a message: the receiver gets it, and so does Eve. */
    public byte[] send(byte[] message) {
        seenByEve.add(message.clone());
        return message;
    }

    public List<byte[]> seenByEve() {
        return List.copyOf(seenByEve);
    }
}
