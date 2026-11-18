# Risk Management, Explained

## The idea in one sentence

A risk number without a range and a tail is a promise nobody can keep.

## The 5 acts

### 1. One risk, from ranges to money

The input is an expert's range for how often ransomware hits the core banking system in a year (least 0.05, most likely 0.15, most 0.5) and for what one event costs (least $500k, most likely $3.0M, most $20.0M). The heat map would score it likelihood 2 x impact 5 = 10.
The average of a triangle range is (least + most likely + most) / 3: 0.233 events a year and $7.8M an event, so the expected loss is 0.233 x $7.8M = $1.8M a year.
One simulated year takes three draws between 0 and 1. In year 11, the first draw 0.853 is past the most likely point 0.222, so the rate is 0.5 - sqrt(0.147 x 0.45 x 0.35) = 0.348 events a year. The second draw 0.737 lies between P(none) = e^-0.348 = 0.706 and P(at most 1) = 0.952, so the year has one event. The third draw 0.126 reads the loss range at $500k + sqrt(0.126 x $19.5M x $2.5M) = $3.0M.
The result of 50,000 such years: 79.7% have no ransomware, the expected loss is $1.8M a year, and 1 year in 10 loses more than $8.0M.

### 2. The register: the heat map against money

The input is the register: eight risks of a bank, each with a heat map score (likelihood x impact, 1 to 5 each) and two ranges. Card breach and insider fraud score 12, ransomware only 10, because its likelihood is low.
The result of 50,000 simulated years: ransomware loses $1.8M a year on average, third-party breach $1.0M and card breach $920k, while the outage, scored 10, loses only $444k. The heat map's top three are card-breach, insider-fraud and outage; the money's top three are ransomware, third-party and card-breach. Only card-breach is in both lists.

### 3. The whole register: the bad years

The input is the same register, added up in each of the 50,000 years. The result is a spread: 22.0% of years lose $0 to $2M, 27.1% $2M to $4M, and a long tail, with 2.6% of years losing $20M or more.
The expected loss is $5.8M a year, and the eight averages (mean events x mean loss) add to the same $5.8M, which checks the simulation. The tail is the news: 1 year in 10 loses more than $12.8M, 1 in 20 more than $16.8M, and 41.4% of years lose more than $5.0M. With 1,000 years instead of 50,000 the estimate is 0.9% away.
Which input matters: raising a most likely loss by 50% moves the expected loss by the events a year times the change, divided by three. For ransomware 0.233 x $1.5M / 3 = $117k; the simulation gives +$116k.

### 4. Controls within a budget

The input is seven controls, each with a cost a year and factors on the risks it touches: offline backups keep 0.3 of a ransomware loss, multi-factor login keeps 0.4 of the phishing events and 0.8 of the ransomware events, and so on.
Each control alone avoids some loss: backups avoid $1.3M for $250k, a net benefit of $1.0M; endpoint detection avoids $1.1M for $400k.
There are 2^7 - 1 = 127 sets of controls, and 56 of them cost at most $800k. Each is scored on the same 50,000 years, so the difference between two sets is the controls, not the luck.
The best set is backups, mfa, vendors and approval: cost $610k, expected loss $5.8M -> $3.1M, net benefit $2.7M - $610k = $2.1M. Cheapest first takes approval, mfa, vendors and scrubbing: cost $560k, expected loss $3.8M, net benefit $1.5M. The best set gains $581k a year more.

### 5. The appetite, and what is left

The input is the appetite, the board's number: at most 5.0% of years may lose more than $5.0M.
Before any control, 41.4% of years do: outside. After the best set within the budget, 19.5%: still outside, with a residual expected loss of $3.1M and 1 year in 20 losing more than $8.4M. With all seven controls, costing $1.7M, 4.1%: within.
Trying every set for the cheapest one within the appetite gives all seven controls: no smaller set is enough, and it costs $910k more than the budget. The decision is the board's: spend more, insure the rest, or change the appetite.
What the model cannot know: the ranges are an expert's guesses, the risks are independent here while real incidents cluster, and an incident nobody imagined is not in the register.

## The verdict

Keep the register as ranges, simulate, rank by expected loss and by the tail, price controls against the loss they avoid, state the appetite as a number, and show the model's limits with the result.

## How to recognise this in code you did not write

- A heat map with no money on it.
- A control budget set by last year's budget.
- An appetite statement with no number.
- A risk estimate given as one value.

## Where you have already met this

- A heat map where everything is amber.
- A control bought because a peer bought it.
- A risk register updated once a year.
- An appetite statement with no number in it.
