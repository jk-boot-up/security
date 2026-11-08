# Threats and Risk, Explained

## The idea in one sentence

A risk is a threat using a vulnerability to harm an asset, scored as how likely and how bad, and a risk register ranks those scores so that a small budget goes where it removes the most.

## The 5 acts

### 1. The attack surface: every way in

An attack surface is the list of every way in. Before the release, the store has six entry
points: two public (the catalogue and the login), two for signed-in customers
(checkout and the order page), and two internal (the refund tool and the customer
export). The release ships a reviews feature, and leaves a debug page behind
that shows the store's settings and keys. After the release there are eight entry points, and
three are open to everyone. Nobody decided to open the debug page. It simply
appeared, which is why counting the surface is the first step, and why it is counted again after every release.

### 2. Threat, vulnerability, asset, and risk

Take one risk, R1, and name its parts. The asset is the customer orders. The threat is
that customer order data is stolen. The vulnerability, the weakness the threat
would use, is that the order page does not check the owner, so changing the order
number in the address shows someone else's order. The risk is how likely and how
bad. The score is worked out in four steps: likelihood 4 out of 5, impact 4 out of 5, the two multiplied give 16,
and 16 falls in the band 13 to 25, so it is rated high. The bands are low from 0 to 5, medium from 6 to 12, and high from 13 to 25.

### 3. Ranking the risks

With every risk scored, sort them. Before ranking, the register is in the order the risks were written, R1 to R7.
After ranking, the order page leak scores 16, the debug page
15, the login attack 12, then the export and the card risk tie at 10, fake
reviews 6 and price scraping 4. A tie goes to the higher impact, and then to the
lower id: R2 and R6 both score 10 with the same impact, 5, so R2 comes first, and the order never changes between runs. Rated in words: two are high,
four are medium and one is low. The total score is 73, which gives a single
number to bring down.

### 4. Spending a budget where it removes the most

There are four ways to treat a risk. Mitigate it, which lowers the likelihood or
the impact. Avoid it, by removing its cause. Transfer it, by handing it to
someone else, such as a payment provider. Or accept it, and live with it. Each
treatment has a cost, and the budget is 10. Before the plan, each treatment is valued: the
score it removes, divided by its cost. Deleting the debug page takes R4 from 15 to 0, removing 15 for a cost of 1: value 15.0.
Checking the order owner takes R1 from 16 to 4, removing 12 for a cost of 2: value 6.0. Limiting login attempts removes 6 for 3 (2.0),
a second approval for exports removes 5 for 3 (1.7), and the hosted payment page removes 8 for 5 (1.6). After the plan, the best value is
bought first: 1, then 3, 6 and 9 spent. The hosted payment page would need 9 + 5 = 14, more than 10, so it waits. The total score falls from 73 to 35, and the worst
risk left is the card risk, at 10.

### 5. The bill: a score is a judgment

The scores look like measurements, but they are estimates. In the two reviews, engineering knows a
filter already sits in front of the login, so scores R3 as 2 times 3, 6; finance sees real fraud losses, so scores it 5 times 4, 20.
Finance also scores the price list R7 as 4 times 3, 12, where engineering keeps 4. In the two rankings, engineering's top
three are R1, R4 and R2, and finance's are R3, R1 and R4. The rules are the same and the
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
