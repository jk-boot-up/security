# Race Conditions with PostgreSQL and Hibernate, Explained

## The idea in one sentence

A rule that must always hold belongs where every writer must pass: in the schema, in one statement, or behind a lock that the database enforces.

## The 5 acts

### 1. Check, then act: two requests in the gap

The input: ANA-1 holds 100, and A and B each withdraw 80 on their own connection: select the balance, check it in Java, update. In the order A reads, B reads, A writes, B writes: both read 100 and both checks 100 >= 80 are true; A writes 20 and commits, B writes 20 and commits. The result: 160 paid out from a balance of 100, and the balance is 20.
After the fix, one: the rule lives in the table, balance int check (balance >= 0), and the update is relative, set balance = balance - 80. A takes 100 - 80 = 20; B would make 20 - 80 = -60, which breaks the check, so PostgreSQL refuses it with 23514 check_violation. Paid out 80.
After the fix, two: the check is inside the update, ... where id = 'ANA-1' and balance >= 80. A changes 1 row; B changes 0 rows, because 20 >= 80 is false, and so knows it was refused.
A single-use coupon SPRING20 read by both as used = false gives 20 + 20 = 40 off. Claimed in one statement, set used = true where ... and used = false, A gets 1 row and B 0 rows.

### 2. Row locks: making the check and the write one step

A runs select ... for update on ANA-1: it locks the row and reads 100. B asks for the same row and waits; pg_stat_activity shows B waiting for a lock (true). A pays out 80 and commits, which releases the lock. B then gets the row, reads 20, and 20 >= 80 is false, so B refuses. The result: paid out 80, balance 20.
When B should not wait while A holds the row: for update nowait refuses at once with 55P03 lock_not_available; lock_timeout of 300 ms waits that long and then gives up with 55P03; skip locked takes the next row that nobody holds, here BEN-2, as workers do on a queue.

### 3. Isolation levels

READ COMMITTED, the default: A reads 100; B takes 30 and commits, leaving 70; A's update, balance - 30, is applied to the new value: 70 - 30 = 40.
REPEATABLE READ, the same steps: A reads 100 and keeps that snapshot; B takes 30 and commits; A's update is refused with 40001 serialization_failure, "could not serialize access due to concurrent update". A retries from the start, in 1 attempt: 70 - 30 = 40. The caller must be able to retry.
One rule over two rows: ANA-1 and ANA-2 hold 100 each and the total must stay at 0 or more. A and B each read the total, 200, and see room for 150; A takes 150 from ANA-1 and B 150 from ANA-2. Under REPEATABLE READ both commit: 200 - 150 - 150 = -100, write skew, because neither update touched the other's row. Under SERIALIZABLE, B is refused with 40001, and the total is 200 - 150 = 50.

### 4. Optimistic locking with Hibernate, and one-time keys

With Hibernate 7 and a @Version column, A and B both load ANA-1: balance 100, version 0. A pays 80 and commits with update hib_account set balance=?,version=? where id=? and version=?: balance 20, version 0 + 1 = 1, where version = 0, which changes 1 row. B pays 80 on what it loaded and commits: its update asks for version 0, but the row is at version 1, so it changes 0 rows and Hibernate throws an OptimisticLockException. B retries: it loads 20 at version 1, and 20 >= 80 is false.
A pessimistic write lock from Hibernate sends select ... for no key update of ha1_0.
One payment key, rent-october, is the primary key of payments. A inserts it (1 row) and moves 70 in one transaction; B inserts the same key at the same time and waits for A; A commits the key and the money together; B's insert then changes 0 rows, so nothing moves. Ana has 100 - 70 = 30.

### 5. The bill

Two transfers, each locking its own sender first: A locks ANA-1 and then asks for BEN-2, B locks BEN-2 and then asks for ANA-1. PostgreSQL finds the cycle and cancels 1 of them with 40P01 deadlock_detected; the other is done, and the cancelled one must be retried. With one lock order for everyone, order by id ... for update, both are done.
Two servers, each with a lock in its own memory, pay out 80 + 80 = 160 from 100: the locks never meet. A row locked while a person confirms makes B give up after 300 ms with 55P03: keep transactions short.
What each error code asks of the application: 23514 check_violation, refuse and do not retry; 55P03 lock_not_available, try again later; 40001 serialization_failure and 40P01 deadlock_detected, retry the transaction.

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
