# Password Hashing, Explained

## The idea in one sentence

A shop should store only a slow, salted hash of each password: the hash hides the password, the salt makes identical passwords look different and defeats tables made in advance, and the slowness makes every guess cost real work.

## The 5 acts

### 1. Passwords stored as they are

The first version stores each password exactly as the customer typed it. The table is
stolen, and the thief reads 5 of 5 passwords at once. Ana's row says welcome1. There is
nothing to crack and no work to do. And because many people reuse passwords, the damage
spreads to their other accounts.

### 2. A fast hash, with no salt

The second version stores a SHA-256 hash of each password. A hash is a one-way scramble:
the thief cannot read the password from it. But the scramble is the same every time. Ana and
Ben both chose welcome1, and both rows begin fcc3a23f, so anyone can see they match. The attacker hashes
eight common passwords once and looks each stolen value up in that table. Four accounts fall at
once: Ana, Ben, Carol and Erin. Only Dave's long random password is safe.

### 3. A fast hash, with a salt for each account

The third version adds a salt: a few extra bytes, different for every account, mixed into
the password before it is hashed and stored beside the hash. Ana's value now begins 82ae9429
and Ben's begins 51cd9c7f, even though their passwords are the same, so nobody can see that they
match. The table made in advance no longer fits any account, because each account has its own
salt. The attacker must guess for each account separately. But the hash is still fast. The
same four accounts fall after 24 guesses, which is 24 steps of work.

### 4. A slow hash, with a salt for each account

The fourth version uses PBKDF2, which repeats the scramble many thousands of times. Here, 100,000
rounds. Each stored text has four parts: the algorithm, the number of rounds, the salt and the
hash. A right password logs in and a wrong one does not. The same attack, the same 24 guesses, now costs
2,400,000 steps, because each guess costs 100,000 times more than a fast hash. That is the point of a
slow hash. But it cannot rescue a password that is on the attacker's list. The same four accounts still
fall. Dave's survives, as before. A slow hash makes guessing expensive, not impossible.

### 5. The bill: every login pays, and old hashes must be upgraded

The slow hash costs the shop too. One login costs 100,000 steps, and 1,000 wrong logins cost 100,000,000 steps, so
the login itself can become a target; rate limits are needed. The second cost is time. Erin's account was
made years ago with only 10,000 rounds, while Ana's uses 100,000. At Erin's next correct login, the shop
quietly stores a new hash with 100,000 rounds. The next login reports no upgrade needed. The shop can
only upgrade a hash when it holds the password, which is at login, so accounts that never log in stay weak.

## The verdict

Never store a password and never store a plain fast hash. Use a slow, salted password hash from a
trusted library, store its settings with each hash so they can be raised later, upgrade old hashes at
login, and limit login attempts. Then add what hashing cannot give: a policy against weak passwords,
and a second factor.

## How to recognise this in code you did not write

- Stored passwords that begin with a name or version marker, such as `$2a$`.
- A user table with one column for the whole stored text, not separate columns for password and salt.
- A `PasswordEncoder` or a `hash` and `verify` pair of functions.
- Login code that does a comparison of equal length and time.

## Where you have already met this

- The long `$2a$...` text stored in a user table of many applications: a bcrypt hash with its cost and salt included.
- The notice "we store passwords as salted hashes" in a company's security page.
- A data-breach story where passwords were "hashed with MD5", and cracked within days.
- A framework's `PasswordEncoder`, whose stored values begin with a name between curly braces.
