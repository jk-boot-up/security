# The CIA Triad, Explained

## The idea in one sentence

The CIA triad says that information must stay private (confidentiality), stay correct (integrity), and stay reachable (availability), and that every security control protects at least one of the three.

## The 7 acts

### 1. The first version, with no controls

The first version of the order system trusts everyone. Three requests go in: Raj, from support, reads order O-101; Mallory,
also from support, changes its amount to $12; and in a flash sale 100 checkouts from a bot arrive, then 1 from Maya.
Nothing is checked, so the results are three failures. Raj gets the shipping address, 12 Palm Street, which he never needed:
a leak. Ana sees amount=$12 and nobody knows it was changed: a silent change. The server can do 10 requests per tick and
serves them in order, so all 10 go to the bot and Maya's checkout is refused: an outage.

### 2. Confidentiality: each role sees only what it needs

The fix for the leak is to decide what each role may see. The same three requests go in: Raj (support) and Ana (finance)
read O-101, and Mallory (support) tries to change its amount. The responses are filtered by role. Raj gets customer=Maya Rao
and item=Laptop, with the address and the amount hidden. Ana gets everything: 12 Palm Street and $1200. Mallory is denied,
because only finance may change an amount. The principle is called least privilege: give each person only what the job needs.

### 3. Integrity: every order is saved with a seal

Role checks guard the front door, but someone may reach the stored data another way. So every order is saved with a seal.
Two inputs go in: the order as one line of text, O-101|Maya Rao|Laptop|12 Palm Street|1200, and a secret key. HMAC-SHA256
mixes them into the seal, 701017e4 0f4b5c72 ... a10f3d55: 32 bytes, written as 64 hex characters. Storage keeps the order
and its seal side by side. Change one character of the text and the seal comes out completely different; without the key,
nobody can work out the new one.

### 4. How the seal is made, step by step

HMAC-SHA256 makes the seal in three steps, and `SealSteps` redoes them by hand. First, the 33-byte key is padded with 31 zero
bytes to 64 bytes, the block size of SHA-256. Second, every byte of the padded key is XORed with 36, and SHA-256 hashes that
followed by the order text: the inner hash 3854d249 8b389b27 .... Third, every byte of the padded key is XORed with 5c, and
SHA-256 hashes that followed by the inner hash: 701017e4 0f4b5c72 ... a10f3d55, exactly the seal Java's Mac gave in act three.
SHA-256 on its own needs no key, so anyone could recompute a plain hash; mixing the key in twice means only someone with the
key can make a matching seal.

### 5. A change behind the application is caught

Before the edit, storage holds O-101|Maya Rao|Laptop|12 Palm Street|1200 with the seal 701017e4 0f4b5c72 .... Mallory edits
the amount directly in storage, to 12, and leaves the seal as it was, because without the key a new one cannot be made. When
Ana reads O-101, the system works the seal out again from the text it finds, 3513c4ae 562fdd03 ..., which does not match the
stored seal, so the order is refused instead of showing a wrong amount. O-102 is untouched and still reads normally. The seal
does not stop the edit; it makes sure the edit cannot go unnoticed.

### 6. Availability: one client cannot use up the whole server

The same flash-sale flood arrives again: 100 checkouts from the bot, then 1 from Maya, at a server that can do 10 per tick.
This time each request first goes to a rate limiter, which counts the client's requests in this tick and allows at most 3.
The bot's requests 1, 2 and 3 get counts 1, 2 and 3 and are allowed; request 4 gets count 4, over 3, and is refused, and so
are requests 5 to 100. Maya's request is her first, count 1, and is allowed. Without the limit the bot got all 10 and Maya
was refused; with it the bot gets 3, is refused 97 times, and Maya is served. Availability is protected by sharing the
capacity fairly.

### 7. The bill: strict controls get in the way of emergencies

Strict confidentiality has a cost. A courier is at the wrong door, the customer is on the phone, finance is not available,
and Raj, from support, gets address=[hidden]. The system offers an emergency exception, often called break-glass access.
Asked with no reason, it is denied. Asked with a reason, it shows the address, 12 Palm Street, and writes an entry to the
audit log: raj used emergency access on O-101, with the reason. The three promises pull against each other, so the
exception must exist, and it must be watched.

## The verdict

The triad is a checklist for thinking, not a product. Whenever a system holds
something that matters, ask three questions: who must not see it, who must not
change it, and who must always be able to reach it. Then ask which control
answers each question, and what that control costs the other two promises.

## How to recognise this in code you did not write

- A permission check before returning data (confidentiality).
- A checksum, signature or seal stored beside data (integrity).
- A rate limit, a backup or a second server (availability).
- An emergency exception with a logged reason: the triad's promises being traded off on purpose.

## Where you have already met this

- A support screen that shows an order's item but not the card or the address (confidentiality).
- A download page that shows a checksum beside the file, so you can tell if it was altered (integrity).
- A shop that goes down on a sale day, and the protection services that absorb the flood of traffic (availability).
- The phrase "break glass in case of emergency" on a fire alarm, and its twin in business software.
