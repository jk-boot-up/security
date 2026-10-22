package com.jk.explore.security.corsspring;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** The requests that reached the bank's own code, so the demo can show what CORS does not stop. */
@Component
class Processed {

    private final List<String> requests = new CopyOnWriteArrayList<>();

    void add(String request) {
        requests.add(request);
    }

    List<String> list() {
        return List.copyOf(requests);
    }
}
