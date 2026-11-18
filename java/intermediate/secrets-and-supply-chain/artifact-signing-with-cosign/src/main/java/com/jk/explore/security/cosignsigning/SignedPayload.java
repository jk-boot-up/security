package com.jk.explore.security.cosignsigning;

import java.util.Map;
import java.util.TreeMap;

/**
 * What cosign really signs for an image: a small JSON document (the "simple signing" format) that names the image's manifest digest,
 * the repository it was signed for, and any claims added with {@code -a key=value}. The signature covers these bytes, so the digest and
 * the claims cannot be changed without breaking it. A signature never covers the tag.
 */
record SignedPayload(String type, String digest, String reference, Map<String, String> claims) {

    @SuppressWarnings("unchecked")
    static SignedPayload of(Map<String, Object> json) {
        Map<String, Object> critical = (Map<String, Object>) json.get("critical");
        Map<String, Object> identity = (Map<String, Object>) critical.get("identity");
        Map<String, Object> image = (Map<String, Object>) critical.get("image");
        Map<String, String> claims = new TreeMap<>();
        Object optional = json.get("optional");
        if (optional instanceof Map<?, ?> map) {
            map.forEach((k, v) -> claims.put(String.valueOf(k), String.valueOf(v)));
        }
        return new SignedPayload(String.valueOf(critical.get("type")), String.valueOf(image.get("docker-manifest-digest")),
                String.valueOf(identity.get("docker-reference")), claims);
    }

    /** The claims as "key=value key=value", or "(none)". */
    String claimsText() {
        if (claims.isEmpty()) {
            return "(none)";
        }
        StringBuilder text = new StringBuilder();
        claims.forEach((k, v) -> text.append(text.isEmpty() ? "" : " ").append(k).append('=').append(v));
        return text.toString();
    }
}
