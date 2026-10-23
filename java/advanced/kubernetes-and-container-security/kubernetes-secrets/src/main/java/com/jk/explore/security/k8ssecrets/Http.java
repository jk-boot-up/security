package com.jk.explore.security.k8ssecrets;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** An HTTP answer from OpenBao: the status code and the JSON body (empty when there is none). */
record Http(int status, JsonNode body) {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Reads the output of curl run with {@code -w "\n%{http_code}"}: the body, then the code on the last line. */
    static Http parse(String curlOutput) {
        String text = curlOutput.stripTrailing();
        int cut = text.lastIndexOf('\n');
        String code = cut < 0 ? text : text.substring(cut + 1);
        String body = cut < 0 ? "" : text.substring(0, cut).strip();
        return new Http(Integer.parseInt(code.strip()), body.startsWith("{") ? JSON.readTree(body) : JSON.createObjectNode());
    }

    boolean ok() {
        return status == 200;
    }
}
