package com.jk.explore.security.apisec;

import java.util.List;
import java.util.Map;

/**
 * The fixed version: an explicit request model, an explicit response model, a hard cap on page size, and budgets. A list costs one token per
 * row returned, so asking for more is allowed but is not free.
 */
public final class SafeApi implements Api {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 50;

    private final Bank bank;
    private final RateLimiter writes;
    private final RateLimiter reads;

    public SafeApi(Bank bank, SimClock clock) {
        this.bank = bank;
        this.writes = new RateLimiter(clock, 5, 5.0 / 60);        // 5 changes, then 5 a minute
        this.reads = new RateLimiter(clock, 100, 100.0 / 60);     // 100 rows, then 100 a minute
    }

    @Override
    public ApiResponse patchProfile(String user, String accountId, Map<String, Object> body) {
        int wait = writes.tryTake(user, 1);
        if (wait > 0) {
            return new ApiResponse(429, "too many requests", wait);
        }
        var account = bank.find(accountId).filter(a -> a.owner().equals(user));
        if (account.isEmpty()) {
            return ApiResponse.error(404, "not found");
        }
        try {
            UpdateProfileRequest req = UpdateProfileRequest.parse(body);
            if (req.displayName() != null) {
                account.get().set("displayName", req.displayName());
            }
            if (req.email() != null) {
                account.get().set("email", req.email());
            }
        } catch (UpdateProfileRequest.BadRequest e) {
            return ApiResponse.error(400, e.getMessage());
        }
        return ApiResponse.ok(AccountView.of(account.get()));
    }

    @Override
    public ApiResponse getAccount(String user, String accountId) {
        int wait = reads.tryTake(user, 1);
        if (wait > 0) {
            return new ApiResponse(429, "too many requests", wait);
        }
        return bank.find(accountId).filter(a -> a.owner().equals(user))
                .<ApiResponse>map(a -> ApiResponse.ok(AccountView.of(a))).orElse(ApiResponse.error(404, "not found"));
    }

    @Override
    public ApiResponse listAccounts(String user, int page, int size) {
        if (page < 0 || size < 0) {
            return ApiResponse.error(400, "bad paging");
        }
        int effective = size == 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        List<Account> all = bank.all();
        int from = (int) Math.min((long) page * effective, all.size());
        int to = Math.min(from + effective, all.size());
        int wait = reads.tryTake(user, Math.max(1, to - from));
        if (wait > 0) {
            return new ApiResponse(429, "too many requests", wait);
        }
        return ApiResponse.ok(all.subList(from, to).stream().map(a -> new DirectoryEntry(a.id(), String.valueOf(a.get("displayName")))).toList());
    }
}
