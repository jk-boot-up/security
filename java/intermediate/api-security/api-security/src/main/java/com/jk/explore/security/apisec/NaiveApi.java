package com.jk.explore.security.apisec;

import java.util.List;
import java.util.Map;

/**
 * The first version. It does check who owns the account, which is a different lesson; what it lacks is three limits the API owes its callers:
 * which fields may be set, which fields may be shown, and how much anyone may ask for.
 */
public final class NaiveApi implements Api {

    private final Bank bank;

    public NaiveApi(Bank bank) {
        this.bank = bank;
    }

    @Override
    public ApiResponse patchProfile(String user, String accountId, Map<String, Object> body) {
        var account = bank.find(accountId).filter(a -> a.owner().equals(user));
        if (account.isEmpty()) {
            return ApiResponse.error(404, "not found");
        }
        body.forEach(account.get()::set); // every key the caller sends becomes a field: "role", "creditLimitCents", ...
        return ApiResponse.ok(account.get().all());
    }

    @Override
    public ApiResponse getAccount(String user, String accountId) {
        var account = bank.find(accountId).filter(a -> a.owner().equals(user));
        return account.<ApiResponse>map(a -> ApiResponse.ok(a.all())).orElse(ApiResponse.error(404, "not found")); // the entity, whole
    }

    @Override
    public ApiResponse listAccounts(String user, int page, int size) {
        List<Account> all = bank.all();
        int from = Math.min(page * size, all.size());
        int to = (int) Math.min((long) from + size, all.size()); // size is whatever the caller says
        return ApiResponse.ok(all.subList(from, to).stream().map(Account::all).toList());
    }
}
