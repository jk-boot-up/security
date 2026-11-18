# Failing Closed: Secure Error Handling, Explained

## The idea in one sentence

Failing closed means that when a check cannot run, times out or throws, the answer is a refusal, half-finished changes are undone, resources are released, and the customer gets a reference instead of the error, with budgets, breakers and alerts to keep the cost in availability bearable.

## The 5 acts

### 1. Failing open

The naive rule starts from yes, and a check can only take it away. A payment of 1500.00 at unknown-electronics is refused while the fraud check works. While the fraud check is down, the
same payment is approved: the exception was swallowed, so nothing took the yes away. A payment of 4500.00 is worked out against the daily limit: 0.00 spent today plus 4500.00 is 4500.00,
more than 2000.00, so it is refused. When the spent-today value is unreadable ("n/a"), parsing throws, the limit check is skipped, and the payment is approved. A check that never ran took nothing away.

### 2. Failing closed

The safe rule starts from no, and only a check that ran and passed turns it into yes. Five payments go in, each with what goes wrong: a coffee for 4.50 with nothing wrong, the risky 1500.00 payment
with nothing wrong, the risky payment while the fraud check is down, a coffee while the fraud check is slow, and the 4500.00 payment with spent-today "n/a". The coffee is approved (REF-0001);
the risky payment is refused as fraudulent; the other three are refused because they could not be checked. The time budget is worked out: the slow check needs 5000 ms, more than the 300 ms
budget, so the call is given up at 300 ms and no answer counts as no. Every refusal is in the security log with its reference: 4 lines.

### 3. Failing halfway

Before: ana has 1000.00, ben has 0.00 and his account is frozen; the bank holds 1000.00. Ana sends 250.00 to ben. Debit then credit: the debit works, 1000.00 - 250.00 = 750.00, and the credit
fails because ben's account is frozen. Now ana has 750.00, ben 0.00, and the total is 750.00: 1000.00 - 750.00 = 250.00 vanished. As one unit of work, the transfer is checked first; ben accepts
no credit, so nothing moves and the total stays 1000.00. A pool of three connections: three failed updates that give a connection back only on success leave 2, 1, then 0 free, and the next
update fails with no connection available. With try-with-resources, 3 stay free after each failure.

### 4. What the customer is told, and what the log keeps

The failure is the fraud service's exception: fraud-db.internal:5432 connection refused for card 4111111111111111. The raw error page shows it as it came: it names an internal server, shows the full
card number and shows lines of code. The customer instead sees: the payment could not be completed, please try again later, quoting reference REF-0001. The log line keeps the reference and the
reason, with the card number masked: 4111111111111111 becomes ****1111, the last four digits. Support finds the detail from the reference, and an attacker learns nothing from the page.

### 5. The bill, and how to pay it

The outage: the fraud check needs 5000 ms and the budget is 300 ms; 10 customers each pay 4.50 for a coffee. Without a breaker, 0 of 10 are approved and the struggling fraud check is called 10
times, waiting 10 x 300 ms = 3000 ms. With a circuit breaker that opens after 3 failures, still 0 of 10 are approved, but the fraud check is called only 3 times, the waiting is 3 x 300 ms = 900 ms,
and the breaker is OPEN. The refusals are counted by reason: breaker open 7, fraud check too slow 3; the total of 10 is at least the alert threshold of 5, so an alert is raised. After the 30000 ms
cool-down, with the fraud check back, one trial call approves a coffee and the breaker is CLOSED. An open breaker still says no: it fails fast, not open.

## The verdict

Start every security decision as no, and approve only on an explicit, successful check. Give every call a time budget, and treat a late answer as no answer. Never write an empty catch, and
map unexpected exceptions to a refusal. Make related changes one unit of work, and use try-with-resources. Show the customer a reference, log the detail without secrets, and alert on refusals.

## How to recognise this in code you did not write

- `catch (Exception e) { }` and `catch (Exception e) { return true; }` in code reviews.
- Timeouts, retries and circuit breakers in service clients, and their dashboards.
- Error pages that say "something went wrong, reference ABC-123".
- Post-incident reports that say a check "failed open" during an outage.

## Where you have already met this

- `catch (Exception e) { }` in code reviews, and logs full of swallowed exceptions.
- Outages where a payment, login or licence check let everything through while a service was down.
- Error pages with a stack trace, a database host name or a SQL fragment.
- Circuit breakers and timeouts in libraries such as Resilience4j, and the alerts that watch them.
