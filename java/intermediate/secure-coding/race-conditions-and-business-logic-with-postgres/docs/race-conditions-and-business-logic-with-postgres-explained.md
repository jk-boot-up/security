# Race Conditions with PostgreSQL and Hibernate, Explained

## The idea in one sentence

A rule that must always hold belongs where every writer must pass: in the schema, in one statement, or behind a lock that the database enforces.

## The 5 acts

### 1. Check, then act: two requests in the gap

ana has 100 and sends two withdrawals of 80. A and B both read 100, both checks pass, both write the balance they worked out: the bank paid out 160 from a balance of 100, and the balance says 20. With the rule in the table, balance int check (balance >= 0), and the update relative to what is there, the second is refused by the database with 23514. With the check inside the update, where balance >= 80, A changes one row and B none. A single-use coupon checked, applied and marked used by two requests that both saw used = false gives a discount of 40; claimed in one statement, where used = false, A claims 1 and B 0.

### 2. Row locks: making the check and the write one step

A selects the account for update and reads 100. B asks for the same row and waits: pg_stat_activity shows a Lock wait. A pays out 80 and commits; then B reads 20 and is refused. With NOWAIT, B is refused at once with 55P03. With lock_timeout of 300 ms, B waits at least 300 ms and gives up. With SKIP LOCKED, B takes the next row that nobody holds, as workers do on a queue.

### 3. Isolation levels

Under READ COMMITTED, A read 100, B took 30 and committed, and A's update of balance - 30 is applied to the new value: balance 40. Under REPEATABLE READ A keeps its snapshot and the update is refused with 40001, could not serialize access; A starts again and succeeds. Two accounts that must stay at 0 or more in total: under REPEATABLE READ both see 200, each takes 150 from a different account, both commit and the total is -100: write skew. Under SERIALIZABLE B is refused with 40001 and the total is 50. A stronger level makes the database refuse some interleavings, and the application must retry what it refuses.

### 4. Optimistic locking with Hibernate, and one-time keys

A and B both load the account: balance 100 and 100, version 0 and 0. A pays 80 and commits with update hib_account set balance=?,version=? where id=? and version=?. B pays 80 on what it loaded and its commit is refused with an OptimisticLockException. B starts again, loads balance 20 at version 1, and the rule refuses. A pessimistic write lock sends select ... for no key update. The same payment key arriving twice at once: A claims the key and moves 70 in one transaction, B with the same key waits for A and then finds the key taken; ana has 30.

### 5. The bill

Two transfers in opposite directions, each locking its own sender first: PostgreSQL finds the cycle and cancels exactly one with 40P01, and the other finishes. With every lock taken in one order, order by id, both finish. Two servers, each with a lock of its own in its own memory, pay out 160 from 100. A request that locks the account and then waits for a person to confirm keeps everyone else waiting: B gives up with 55P03. So: keep transactions short, lock in one order, retry on 40001 and 40P01, and let the database enforce the rules that must always hold.

## The verdict

Put the invariant in a constraint, make the check and the write one statement, lock in one order when you must lock, choose the isolation level that your rule needs and retry what the database refuses, keep transactions short, and never rely on a lock in the memory of one process.

## How to recognise this in code you did not write

- `@Version` on an entity.
- `SELECT ... FOR UPDATE`.
- `could not serialize access` in a log.
- `deadlock detected` in a log.

## Where you have already met this

- `@Version` on a JPA entity.
- `SELECT ... FOR UPDATE` in a repository query.
- `@Transactional(isolation = ...)`.
- A 409 Conflict or an "optimistic lock" error page.
