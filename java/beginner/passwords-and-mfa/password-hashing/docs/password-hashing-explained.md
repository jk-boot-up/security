# Password Hashing, Explained

## The idea in one sentence

A shop should store only a slow, salted hash of each password: the hash hides the password, the salt makes identical passwords look different and defeats tables made in advance, and the slowness makes every guess cost real work.

## The 5 acts

### 1. Passwords stored as they are

The first version stores each password exactly as the customer typed it. Before storing, Ana typed welcome1; after storing,
her row says welcome1. The table is stolen, and the thief reads 5 of 5 passwords at once. There is nothing to crack and no
work to do. And because many people reuse passwords, the damage spreads to their other accounts.

### 2. A fast hash, with no salt

The second version stores a SHA-256 hash of each password: 32 bytes that cannot be turned back into the password. Before
hashing, Ana and Ben both typed welcome1. After hashing, both rows hold the same 32 bytes, beginning fcc3a23f, so anyone can see
they match. The attacker hashes eight common passwords once and looks each stolen value up in that table. Four accounts fall:
Ana, Ben, Carol and Erin. Only Dave's long random password is safe, and there was no guessing for each account.

### 3. A fast hash, with a salt for each account

The third version adds a salt: 16 bytes, different for every account. Before hashing, Ana's salt a5a5a5a5 a5a5a5a5 a5a5a5a5 00000001
is joined with her password welcome1 (77656c63 6f6d6531) into 24 bytes. After hashing, SHA-256 gives 82ae9429 63612d26 ...,
and the row stores the salt, a $, and the hash. The salt is not a secret. Ben has the same password but his salt ends 00000002,
so his hash begins 51cd9c7f: nobody can see they match, and a table made in advance fits no account. But the hash is still fast.
The same four accounts fall after 24 guesses, which is 24 steps of work.

### 4. A slow hash, with a salt for each account

The fourth version uses PBKDF2 with 100,000 rounds. Before hashing: the password, the salt and the number of rounds. After
hashing, the stored text has four parts: pbkdf2-sha256, 100000, the salt and the 32-byte hash ce26f863 557480dd .... The program
then makes the hash by hand. Round 1 is an HMAC-SHA-256, keyed with the password, of the salt followed by 00000001: 9c08e776 ....
Every next round is an HMAC of the round before, so no round can be skipped. The hash is all 100,000 rounds XORed together, and
it is exactly the hash PBKDF2 gave. The same attack, the same 24 guesses, now costs 2,400,000 steps. But the four weak passwords
are on the attacker's list, so they still fall. A slow hash makes guessing expensive, not impossible.

### 5. The bill: login costs, and upgrades

The slow hash costs the shop too. One login costs 100,000 steps, and 1,000 wrong logins cost 100,000,000 steps, so the login
itself can become a target and attempts must be limited. Before the upgrade, Erin's account, made years ago, uses 10,000 rounds
while Ana's uses 100,000. Erin logs in with the right password, so the shop knows it for a moment and stores a new hash: after the
upgrade, Erin's uses 100,000 rounds, and her next login is simply OK. Accounts that never log in stay weak.

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
