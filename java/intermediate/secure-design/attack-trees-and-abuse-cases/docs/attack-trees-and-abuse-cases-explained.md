# Attack Trees and Abuse Cases, Explained

## The idea in one sentence

An attack tree prices every way to a goal so that you can cut the cheapest path first, recompute, and repeat; abuse cases turn each step into a test that fails until the defence exists.

## The 5 acts

### 1. The tree

The root is "move money out of Ana's account". Logging in as her can be done by phishing her password and one-time code, reusing a leaked password, a SIM swap, or stealing her session with an injected script. The transfer call can be abused by changing the account number or forging a request from another site.
An employee can be bribed. The software can be exploited through an unpatched library and a move to the database. Each leaf carries a cost and a chance.

### 2. What the tree says about the attacker

Costs add along AND gates and the cheapest child wins at OR gates, so the cheapest path to the goal is the single step B1 at 50 euros. The chance that at least one path succeeds is 97%. Branch by branch: logging in as Ana costs 100 euros and works 35% of the time, abusing the transfer call costs 50 and works 95%,
the insider costs 20,000 and works 20%, and the software break-in costs 6,000 and works 9%.

### 3. Each defence on its own

Applying each of eight defences to the baseline tree, one at a time: only the ownership check changes the cheapest path, because only it closes B1. Passkeys at 40,000 euros close three login branches and leave the cheapest way in untouched. A decision based on "which defence is the strongest" would have bought the wrong one first.

### 4. A plan: always cut the cheapest path, then look again

Greedy planning: choose the defence that raises the cheapest path most (ties by lower chance of success, then price), apply it, recompute. The ownership check moves the cheapest path to 100 euros, tied between the leaked-password and CSRF paths. Closing CSRF leaves it at 100 (leaked passwords) but cuts the chance
to 52%. Passkeys then close the leaked password and phishing paths and the cheapest becomes a script injection at 2,000. A strict content security policy triples that to 6,000, where the software break-in becomes the cheapest path, above the target. The abuse cases still failing fall from 8 to 2 along the way (the insider and the unpatched library).

### 5. Abuse cases as tests

Each abuse case is written as a story ("as Mallory I transfer from Ana's account by changing the account number") and as a check that passes only when the abuse is refused. On the weak bank all eight fail, which is the point: the tests describe the defences that do not exist yet. After the planned defences, six turn green and two stay red:
the insider approving their own transfer and the library flaw that was fixed last month. Those are the next items for the backlog, with their cost from the tree. Thirteen of the tree's fourteen steps have a test; the fourteenth (moving from the web server to the database) is a judgement that no test checks.

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
