# Password Attacks and Defences, Explained

## The idea in one sentence

Attackers can guess passwords through the login page itself, with a dictionary against one account, a few common passwords against every account, or leaked pairs from another site, and each defence, lockout, rate limit, second factor or breach check, stops some of these and misses others.

## The 5 acts

### 1. No defences

First, one guess on its own. The request comes from the attacker, for the name dana, with the password 123456. The bank keeps no password, only a hash:
SHA-256 of "demo-salt:" + name + ":" + password. The guess hashes to 403f2bc8..., the stored hash (of midnight7) is 61340acc..., they differ, and the response is
WRONG_PASSWORD. Then the dictionary attack tries twenty common passwords on Dana, one per tick: guesses 1 to 13 are wrong, guess 14 is right, at tick 13.
Spraying tries three common passwords on all ten accounts, skipping accounts already taken: welcome1 takes Emil, Farah and Hana in 10 tries, hunter2 takes Ivan in 7,
and 123456 takes nobody in 6. Four accounts in 23 guesses, finishing at tick 23, and no account saw more than three wrong guesses.

### 2. Lock an account after 5 wrong passwords, for 10 ticks

A lockout blocks an account for 10 ticks after 5 wrong passwords. Tick by tick: guesses 1 to 5 are wrong at ticks 0 to 4, so ticks 5 to 14 are LOCKED and use up no guess.
Guesses 6 to 10 are wrong at ticks 15 to 19, so ticks 20 to 29 are locked. Guesses 11 to 13 take ticks 30 to 32, and guess 14 gets in at tick 33 instead of tick 13: the same
14 guesses plus 20 locked ticks. Slowed, not stopped. The spray is not affected at all: at most 3 wrong guesses on one account, and the lock needs 5, so it still takes
four accounts by tick 23. A lockout counts failures per account, and a spray spreads its failures thinly across all of them.

### 3. Allow each address 5 attempts in every 10 ticks

A rate limit counts attempts per address, not per account. Each address may make 5 attempts in every window of 10 ticks, and the attacker has one address.
In the window of ticks 0 to 9, guesses 1 to 5 take ticks 0 to 4 and ticks 5 to 9 are RATE_LIMITED. The same happens in the window of ticks 10 to 19. In the third window,
guesses 11 to 14 take ticks 20 to 23, and the attacker is in at tick 23 instead of 13. The spray still takes four accounts, but by tick 43 instead of 23. The attack runs at
about half the speed: a rate limit buys time and does not close the door.

### 4. Credential stuffing: passwords leaked from another site

Credential stuffing uses pairs of usernames and passwords leaked from another site. Ten leaked pairs are each tried once, each from its own address. Dana, Farah,
Joel and Liam used the same password at the bank. The lockout and the rate limit never trigger, because every account sees one attempt and every address makes one:
those four are taken. If Dana and Joel have a second factor, their right password only gets a request for a code, and Farah and Liam are taken. With a check against known breaches,
the right passwords are recognised as public: nobody is taken, and Dana, Farah, Joel and Liam must choose a new password.

### 5. The bill: what the defences cost

The defences have costs. First, a lockout per account can be turned against the customer: an attacker fails 5 times on Dana's account, and then Dana, logging in from her
own phone with the right password, gets the response LOCKED. That is a denial of service. Locking each account-and-address pair instead lets Dana in, but an attacker with
10 addresses then gets 10 times 5, which is 50, guesses instead of 5. Second, messages. If an unknown name gets "no account with that name" and a wrong password gets "wrong password",
the attacker can test which usernames exist. One message for both, "wrong username or password", hides it, at the price of a vaguer message for honest typing mistakes. The work
behind the message must match too: an unknown name costs the same 1 password hash as a known one, so the time a reply takes does not tell them apart either.

## The verdict

Layer the defences, because none of them covers every attack. Use a rate limit per address and a gentle limit per account, a second factor for
anything that matters, and a check against known breaches at sign-up and login. Give one message for every failure, never lock an account in a way an
outsider can trigger for long, and watch the logs, because the first sign of spraying or stuffing is many accounts failing once each.

## How to recognise this in code you did not write

- A login that waits longer after each wrong try.
- "We sent a code to your phone" after a correct password.
- A sign-up form that refuses a password because it appeared in a breach.
- A single message such as "wrong username or password".

## Where you have already met this

- "Your account has been locked after too many failed attempts" emails.
- A login page that asks you to wait, or to prove you are not a robot, after a few wrong tries.
- A prompt to enter a code from an app after you type your password.
- "This password has appeared in a data breach, please choose another" at sign-up.
