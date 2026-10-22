package com.jk.explore.security.bffspring.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** The transfers the API has made, so the demo can count them. */
@Component
public class Ledger {

    private final List<String> transfers = new CopyOnWriteArrayList<>();

    void add(String transfer) {
        transfers.add(transfer);
    }

    public List<String> transfers() {
        return List.copyOf(transfers);
    }
}
