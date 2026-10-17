# Threats and Risk, Explained

## The idea in one sentence

A risk is a threat using a vulnerability to harm an asset, scored as how likely and how bad, and a risk register ranks those scores so that a small budget goes where it removes the most.

## The 5 acts

### 1. The attack surface: every way in

An attack surface is the list of every way in. The store starts with six entry
points: two public (the catalogue and the login), two for signed-in customers
(checkout and the order page), and two internal (the refund tool and the customer
export). The team then ships a reviews feature, and leaves a debug page behind
that shows the store's settings and keys. Now there are eight entry points, and
three are open to everyone. Nobody decided to open the debug page. It simply
appeared, which is why counting the surface is the first step.

### 2. Threat, vulnerability, asset, and risk

Take one risk and name its parts. The asset is the customer orders. The threat is
that customer order data is stolen. The vulnerability, the weakness the threat
would use, is that the order page does not check the owner, so changing the order
number in the address shows someone else's order. The risk is how likely and how
bad. Likelihood is scored 4 out of 5, impact 4 out of 5, and the risk score is
the two multiplied: 16, which is rated high.

### 3. Ranking the risks

With every risk scored, sort them. The order page leak scores 16, the debug page
15, the login attack 12, then the export and the card risk tie at 10, fake
reviews 6 and price scraping 4. A tie goes to the higher impact, and then to the
lower id, so the order never changes between runs. Rated in words: two are high,
four are medium and one is low. The total score is 73, which gives a single
number to bring down.

### 4. Spending a budget where it removes the most

There are four ways to treat a risk. Mitigate it, which lowers the likelihood or
the impact. Avoid it, by removing its cause. Transfer it, by handing it to
someone else, such as a payment provider. Or accept it, and live with it. Each
treatment has a cost, and the budget is 10. The planner values each one by the
score it removes per unit of cost, and buys the best first. Deleting the debug
page costs 1 and removes 15. Checking the order owner costs 2 and removes 12.
Limiting login attempts costs 3 and removes 6, and a second approval for exports
costs 3 and removes 5. That is 9 spent. The hosted payment page costs 5 and
does not fit, so it waits. The total score falls from 73 to 35, and the worst
risk left is the card risk, at 10.

### 5. The bill: a score is a judgment

The scores look like measurements, but they are estimates. Engineering knows a
filter already sits in front of the login, so scores that risk low, and its top
three are R1, R4 and R2. Finance sees real fraud losses, so scores the login
risk 20, and its top three are R3, R1 and R4. The rules are the same and the
ranking is different. That is not a flaw to remove. It is why the assumptions
behind each score must be written down and argued about. And every risk that is
accepted, here R5, R6 and R7, needs an owner and a review date, or it is just a
risk nobody is watching.

## The verdict

Count the ways in, write each risk as an asset, a threat and a vulnerability,
score them, rank them, and spend the budget on the best value first. Then treat
the result as a conversation and not a verdict: write down who scored what and
why, give every accepted risk an owner and a review date, and revisit the list
whenever a feature ships.

## How to recognise this in code you did not write

- A list of entry points, such as the routes of a web application.
- A grid of likelihood against impact with colored cells.
- Findings in a penetration test report, each rated high, medium or low.
- A backlog where security tickets carry a priority and an owner.

## Where you have already met this

- A penetration test report, which is a ranked list of findings.
- The "threat model" step in a design review, which starts by drawing the entry points.
- A risk matrix in a company policy, a grid of likelihood against impact with red, amber and green cells.
- A security ticket labelled high, medium or low priority.
