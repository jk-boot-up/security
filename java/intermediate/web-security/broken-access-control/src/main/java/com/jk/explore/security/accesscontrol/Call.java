package com.jk.explore.security.accesscontrol;

import java.util.Map;
import java.util.Optional;

/** What a handler receives: who is asking (if anyone), the values from the path, the body, and the store's data. */
record Call(Optional<User> user, Map<String, String> params, String body, ShopData data) {

    String param(String name) {
        return params.get(name);
    }
}
