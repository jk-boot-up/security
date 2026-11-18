# Race Conditions and Business Logic, Explained

## The idea in one sentence

A race condition lets two requests both pass a check before either acts, so the check and the action must be one step; retries need idempotency keys so they run once; and the server must enforce its own business rules
and order states, because clients send requests together, again and out of order.

## The 5 acts

### 1. Check, then act

The input: ana's account ANA-1 holds 100, and two withdrawals of 80, A from her phone and B from her laptop, arrive at the same moment. Each one reads the balance, checks that it covers 80, and writes the new balance.
In the order ABABAB, A reads 100, B reads 100, both check that 100 covers 80, A writes 20 and pays out 80, and B writes 20 and pays out 80. The result: 80 + 80 = 160 paid out from a balance of 100, and the balance says 20, because B wrote 100 - 80 over A's 20.
One after the other (AAABBB), B reads 20 and is refused. Of all 20 orders of A's three steps and B's three steps, 18 pay out 160; only AAABBB and BBBAAA are safe, so a test that sends one request at a time never sees the bug.
A single-use coupon SUMMER20 that is checked, applied, then marked used gives 20 + 20 = 40 off instead of 20: the same gap.

### 2. Making the check and the write one step

With a lock on the account around read, check and write, in the order ABABABAB: A takes the lock ANA-1, and B waits three times while A reads 100, checks, writes 20 and releases the lock. Then B takes the lock, reads 20, checks that 20 covers 80: no, and refuses. Paid out 80, balance 100 - 80 = 20.
Without a lock, a compare-and-set writes only if the balance is still the value that was read. In the order ABABAB both read 100 and pass the check; A sets 100 to 20; B tries to set 100 to 20, but the balance is now 20, and 100 = 20 is false, so nothing is written and B reads again, sees 20 and is refused.
A coupon claimed in one step (check and mark together) before the discount gives 20 once. Run in every order: check, then act pays out more than 100 in 18 of 20 orders, the lock in 0 of 70, the compare-and-set in 0 of 20.

### 3. Sending the same request twice

The input: ana pays ben 300 rent, a transfer from ANA-1 to BEN-2, with balances ana 1000 and ben 0. The reply is lost, so her app sends the same request again.
Without a key, send 1 moves 300 (ana 700, ben 300) and send 2 moves 300 again (ana 400, ben 600): paid twice. With the idempotency key rent-october, chosen by the app for this one payment, send 1 moves 300 and send 2 returns "already done: moved 300", so ana still has 700. The same key with an amount of 900 is refused, because a key names one request; a new key, rent-november, is a new payment and ana has 400.
How the bank reads a key: a new key is claimed in one step, the money moves and the answer is kept; a repeated request gets the kept answer and nothing moves; a changed request is refused; and when two copies arrive at once, only one claim succeeds.

### 4. Rules the server must keep

The input: ana and ben have 1000 each, and every transfer earns its sender 1 reward point. Ana asks for a transfer of -50 to ben, ten transfers of 1 to herself, and a transfer of 5000.
Before the fix, a careless bank leaves ana with 1050 and ben with 950: money taken from ben. The ten self-transfers leave ana at 1050 with 10 points for nothing. After the fix, the careful bank refuses -50 (the amount must be above zero), the transfer to herself, and 5000 (not enough money).
The cart as the client sent it: boots-42 x 1, which the catalogue prices at 180, three coupons of 20 each, and a total of 1 written by the client. The careless checkout charges 1 - 20 - 20 - 20 = -59: the store pays. The careful one prices from the catalogue and allows one coupon: 180 - 20 = 160, the client's 1 ignored, WELCOME20 and SPRING20 refused.
An order of 180 kept as flags can be shipped while unpaid and refunded twice, 360. As a state machine (created, paid, shipped, refunded), shipping first is refused, pay, ship and refund work, a second refund is refused, and 180 is refunded in all.

### 5. The bill

A lock makes requests to one account take turns: in act two's order, B waited 3 steps while A held the lock.
Two transfers in opposite directions, A from ANA-1 to BEN-2 for 50 and B from BEN-2 to ANA-1 for 70, each lock their own sender first. In the order AB, A locks ANA-1, B locks BEN-2, A waits for BEN-2 and B waits for ANA-1: each waits for a lock the other holds, a deadlock.
With one lock order for everyone, the smaller account first, both lock ANA-1, then BEN-2. B waits while A moves 50 and unlocks both, then B moves 70. No deadlock: ana has 500 - 50 + 70 = 520 and ben 500 + 50 - 70 = 480.
Two servers sharing one database, each with a lock in its own memory, take the locks server-1:ANA-1 and server-2:ANA-1, which never meet: both pay out 80, 160 from 100. With more than one server, the database must give the guarantee, with row locks, isolation levels or a version check, which is the -with-postgres version of this project.

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
