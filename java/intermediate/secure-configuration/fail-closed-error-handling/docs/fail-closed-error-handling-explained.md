# Failing Closed: Secure Error Handling, Explained

## The idea in one sentence

Failing closed means that when a check cannot run, times out or throws, the answer is a refusal, half-finished changes are undone, resources are released, and the customer gets a reference instead of the error, with budgets, breakers and alerts to keep the cost in availability bearable.

## The 5 acts

### 1. Failing open

With the fraud check working, a payment of 1500.00 at an unknown electronics shop is refused. While the fraud check is down, the same payment is approved: an error became a yes. A payment of
4500.00 against a daily limit of 2000.00 is refused, but when the account's spent-today value cannot be read, it is approved: an exception skipped the check. The decision started as yes,
and each check could only take it away, so a check that never ran took nothing away.

### 2. Failing closed

The decision starts as no, and only a check that ran and passed can turn it into yes. A coffee for 4.50 is approved. The risky payment is refused as fraudulent. While the fraud check is down,
it is refused because it could not be checked, with reference REF-0003. When the fraud check takes five seconds against a budget of 300 ms, a coffee is refused. The big payment with an unreadable
spent-today value is refused. Every refusal is in the log with its reference: four lines.

### 3. Failing halfway

Ana sends 250.00 to Ben, whose account is frozen. The transfer fails after the debit, so Ana has 750.00, Ben has nothing, and the bank's total falls from 1000.00 to 750.00: 250.00 vanished. As
one unit of work, checked first and undone if the second half fails, the transfer is refused and nothing changes. Three failed updates that give a connection back only on success leave no free
connections, and the next update fails. The same three failures with try-with-resources leave all three connections free.

### 4. What the customer is told, and what the log keeps

The raw error page names an internal server, shows the full card number and shows lines of code. The customer instead sees: the payment could not be completed, please try again later, quoting
reference REF-0001. The log line has the reference and the reason, but not the full card number, only its last four digits. Support finds the detail from the reference, and an attacker learns nothing from the page.

### 5. The bill, and how to pay it

Failing closed costs availability: ten coffees while the fraud check is slow are all refused, after 50 seconds spent waiting. With a circuit breaker that opens after three failures, all ten are
still refused, but the fraud check is called only three times and the waiting stops at 15 seconds; the breaker is open. The refusals are counted by reason, and an alert is raised. After the
cool-down, with the fraud check back, a coffee is approved and the breaker closes. An open breaker still says no: it fails fast, not open, and the refusals are watched so a long outage is noticed.

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
