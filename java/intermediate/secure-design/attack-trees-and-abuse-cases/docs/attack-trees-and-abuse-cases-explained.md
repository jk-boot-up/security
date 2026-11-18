# Attack Trees and Abuse Cases, Explained

## The idea in one sentence

An attack tree prices every way to a goal so that you can cut the cheapest path first, recompute, and repeat; abuse cases turn each step into a test that fails until the defence exists.

## The 5 acts

### 1. The tree

The input is the tree. The root, an OR gate, is the goal: move money out of Ana's account. Its first branch, log in as Ana, is an OR of four AND gates: phish her password and one-time code (A1a, 300 EUR, 90%; A1b, 50 EUR, 20%), reuse a leaked password (A2a, 100 EUR, 80%; A2b, 0 EUR, 15%), a SIM swap (A3a, 1,500 EUR, 30%; A3b, 100 EUR, 15%) and steal her session with a script (A4a, 2,000 EUR, 10%; A4b, 0 EUR, 50%).
The other branches: abuse the transfer call (B1, change the account number, 50 EUR, 90%; or forge a request from another site, B2a and B2b), bribe an employee (C, 20,000 EUR, 20%), and break in through the software (D1, an unpatched library, 5,000 EUR, 15%, and D2, move to the database, 1,000 EUR, 60%).
The rules: at an OR gate any child will do, so the cheapest child wins and the chance is 1 - (1 - p1)(1 - p2)...; at an AND gate every child is needed, so the costs add and the chances multiply.

### 2. What the tree says about the attacker

The input is the tree of act one. An AND gate worked out: phish her password and one-time code costs 300 + 50 = 350 EUR and works with 0.9 x 0.2 = 18%.
An OR gate worked out: log in as Ana has four AND gates, at 350 EUR and 18%, 100 EUR and 12%, 1,600 EUR and 5%, 2,000 EUR and 5%. Its cost is min(350, 100, 1,600, 2,000) = 100 EUR and its chance 1 - (1-0.18)(1-0.12)(1-0.045)(1-0.05) = 35%.
The result: the root is an OR of four branches: log in as Ana 100 EUR and 35%, abuse the transfer call 50 EUR and 95%, bribe an employee 20,000 EUR and 20%, break in through the software 6,000 EUR and 9%. The cheapest way in costs 50 EUR via B1, changing the account number, and some way works with 97%.

### 3. Each defence on its own

The input: eight defences with a price: OWN (the account must belong to the user, 2,000 EUR), CSRF (form tokens and SameSite cookies, 3,000 EUR), BREACH (5,000 EUR), OTP (15,000 EUR), PASSKEY (40,000 EUR), CSP (10,000 EUR), PATCH (8,000 EUR) and DUAL (12,000 EUR). Each changes the cost or chance of the steps it names, or removes them.
Each alone, on the tree: OWN gives 100 EUR via A2a and A2b at 75%; every other defence leaves the cheapest way at 50 EUR via B1.
Worked out on the transfer branch: before, the cheapest is 50 EUR via B1 at 95%. OWN removes B1; what is left is the forged request, 100 + 0 = 100 EUR, at 48%.
The result: the best one alone is OWN, and the attacker then pays 100 EUR; PASSKEY alone costs 40,000 EUR and the attacker still pays 50 EUR via B1.

### 4. A plan: cut the cheapest path, then look again

The input: the target, a cheapest attack of at least 5,000 EUR, and the rule: at each step pick the defence that most raises the cheapest path, then look again.
The steps: with nothing, 50 EUR via B1 at 97%, 8 of 8 abuse cases failing. OWN (2,000 EUR spent): 100 EUR via A2a and A2b, 75%, 7 failing. CSRF (5,000 EUR): 100 EUR, 52%, 6 failing. PASSKEY (45,000 EUR): 2,000 EUR via A4a and A4b, 31%, 3 failing. CSP (55,000 EUR): 6,000 EUR via A4a and A4b, 28%, 2 failing.
The result: 55,000 EUR on 4 of the 8 defences; the cheapest way goes from 50 EUR to 6,000 EUR and the chance from 97% to 28%.

### 5. Abuse cases as tests

The input: one abuse case, AC5, the test of leaf B1: As Mallory I change the account number to Ana's. It passes when the bank refuses it, which it does when ownership is on.
Before the plan, nothing is on, and all eight abuse cases fail. After the plan, with csp, csrf, ownership and passkey on, AC1 to AC6 pass; AC7 (an insider approves their own transfer) and AC8 (last month's library flaw) still fail.
The result: 13 of the 14 steps have a test; D2, moving from the web server to the database, has none and stays a judgement; the 2 failing cases are the risks the plan accepts, in writing.

## The verdict

Draw a tree for each goal that matters, estimate the numbers with the people who know the system, rank the defences by what they do to the cheapest path, plan in rounds, and write an abuse case for every step you can test. Revisit the tree when the system or the attackers change.

## How to recognise this in code you did not write

- Tree diagrams in threat modeling reports, with AND and OR nodes.
- "As an attacker I want to..." user stories.
- Security test names such as `shouldNotAllowTransferFromAnotherUsersAccount`.
- A security backlog sorted by attacker cost rather than severity labels.

## Where you have already met this

- Attack trees in threat modeling books and in the automotive and safety-critical industries.
- Misuse cases and evil user stories in agile security practice.
- "As an attacker I want to..." stories in a backlog.
- OWASP SAMM threat assessment and the STRIDE method.
