package com.jk.explore.security.apisec;

import java.util.Map;

/** The bank's API as a customer's app sees it. {@code body} is the parsed JSON of a request. */
public interface Api {

    /** PATCH /accounts/{id}: change my profile. */
    ApiResponse patchProfile(String user, String accountId, Map<String, Object> body);

    /** GET /accounts/{id}. */
    ApiResponse getAccount(String user, String accountId);

    /** GET /accounts?page=&size=: the branch directory. */
    ApiResponse listAccounts(String user, int page, int size);
}
