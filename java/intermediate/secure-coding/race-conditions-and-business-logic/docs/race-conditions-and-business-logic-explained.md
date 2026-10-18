# Race Conditions and Business Logic, Explained

## The idea in one sentence

A race condition lets two requests both pass a check before either acts, so the check and the action must be one step; retries need idempotency keys so they run once; and the server must enforce its own business rules
and order states, because clients send requests together, again and out of order.

## The 5 acts

### 1. Check, then act

Ana has 100 and sends two withdrawals of 80 at the same moment. A reads the balance, then B reads it: both see 100. Both check that 100 covers 80, and both say yes. Then A writes 20 and pays out 80,
and B writes 20 and pays out 80. The bank paid out 160 from a balance of 100, and the balance says 20. Sent one after the other, the second is refused, so a test that sends them in turn never
sees the bug. A single-use coupon that is checked, applied and then marked used gives a discount of 40 instead of 20 the same way.

### 2. Making the check and the write one step

With a lock on the account around read, check and write, A takes the lock, and B waits while A reads 100, checks, writes 20 and releases the lock. Then B takes the lock, reads 20, and refuses.
Paid out 80, balance 20. Without a lock, a compare-and-set writes only if the balance is still what was read: A sets 100 to 20, B tries to set 100 to 20 and fails because the balance changed,
reads again, sees 20 and refuses. A coupon claimed in one step before the discount is used once. The rule that must always hold, the balance never below zero and each coupon used once, now holds
in every order of steps.

### 3. Sending the same request twice

Ana pays 300 rent, the reply is lost, and her app sends the request again. Without a key, ana has 400 and ben 600: paid twice. With an idempotency key chosen by the app for this payment, the first send moves 300,
and the same key again returns "already done: moved 300", so ana has 700 and ben 300. The same key with a different amount is refused, because a key names one request. A new key is a new payment.
The key is claimed in one step before the money moves, so two copies racing each other cannot both run.

### 4. Rules the server must keep

A careless bank given a transfer of -50 from ana to ben takes 50 from ben. Ten transfers of 1 from ana to herself earn 10 reward points for nothing. The careful bank refuses a negative amount,
a transfer to the same account, and more money than the account holds. A cart with boots at 180, three coupons and a total of 1 written by the client is charged -59 by a careless checkout,
and 160 by the careful one, which ignores the client's total and allows one coupon per order. An order kept as flags is shipped before payment and refunded 360 on an order of 180. As a state machine,
shipping first is refused, paying, shipping and refunding work, and a second refund is refused: 180 refunded in all.

### 5. The bill

A lock makes requests to one account take turns: B spent 3 steps waiting while A held the lock. Two transfers in opposite directions, each locking its own sender first, deadlock: A locks ANA-1, B locks BEN-2,
and each waits for the other. With every lock taken in one order, the smaller account first, both finish and ana has 520 and ben 480. Two servers, each with its own lock in its own memory, sharing one database,
both pay out 80 from 100: a lock in one process does not stop another. With more than one server, the guarantee must come from the database, which is the -with-postgres version of this project.

## The verdict

Find every check-then-act on money, stock, coupons and counts, and make it one step: a lock, a compare-and-set, or better a single conditional database update. Give every request that may be retried an idempotency key.
Write the business rules down and enforce them on the server, with explicit states for workflows. Take locks in one fixed order, and with more than one server, let the database provide the guarantee.

## How to recognise this in code you did not write

- `if (balance >= amount) { ... balance = balance - amount; }` in a request handler.
- An `Idempotency-Key` header in a payment API.
- `UPDATE account SET balance = balance - ? WHERE id = ? AND balance >= ?` as a one-step check and write.
- Order tables with a state column and allowed transitions.

## Where you have already met this

- Gift cards and coupons redeemed many times by sending the same request in parallel.
- `Idempotency-Key` headers in payment provider APIs.
- `SELECT ... FOR UPDATE`, optimistic locking with a version column, and isolation levels in database code.
- Order and refund workflows with states such as created, paid, shipped and refunded.
