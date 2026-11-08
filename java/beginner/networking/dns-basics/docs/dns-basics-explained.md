# DNS Basics for Security, Explained

## The idea in one sentence

DNS maps names to addresses in small unsigned datagrams that a resolver believes when they repeat the question, the id and the port, and caches for their lifetime, so unguessable ids, capped lifetimes, signatures and TLS are all needed to limit a false answer.

## The 5 acts

### 1. A DNS message, byte by byte

The question is 34 bytes: a 12-byte header (id abcd, flags 0100, one question), the name written as labels (04 shop 07 example 03 com 00, 18 bytes), then type 0001 (A, an IPv4 address) and class 0001 (IN).
The answer copies the header with the reply flags 8180, copies the question, and adds one 16-byte record: the name as the pointer c00c (c0 marks a pointer, 0c is byte 12, where the name already is), type and class,
the time to live 0000012c (1 x 256 + 2 x 16 + 12 = 300 seconds), the length 0004 and the address cb00710a, which is 203.0.113.10. Without the pointer it would be 66 bytes, not 50. Nothing in the message says who sent it: only the id and the question have to match.

### 2. The cache and the time to live

At second 0 the answer 203.0.113.10 arrives with a time to live of 300 seconds. The resolver keeps it until now + min(TTL, its cap) = 0 + min(300, 3600) = second 300.
Questions at seconds 100 and 299 are answered from the cache, with still only one question sent upstream. At second 300 the entry has expired and the resolver asks again. Caching makes DNS fast, and it also means one false answer is repeated to every customer until it expires.

### 3. A real exchange over UDP

On this machine, a client sends one UDP datagram of 34 bytes, with id 1234, to a tiny server and gets one of 50 bytes back, which repeats the id and says 203.0.113.10.
Shown the way someone on the path sees it, with every byte that is not a printable character as a dot, the query reads .4...........shop.example.com.....: the name is plain text, so anyone on the path sees which sites you ask for. There is no connection and no signature: a reply that looks right is believed.

### 4. Cache poisoning by guessing

An attacker sends up to 1000 forged answers that say 192.0.2.66. Before the fix, the resolver counts its ids up and sends every query from port 53: the last id seen was 4000, so the guess 4001 on port 53 is believed after 1 forged answer, and the cache is poisoned.
After the fix, the id and port of query 2 come from HMAC-SHA-256(secret, 2) = c56a 8cedc967 ...: the id is the first 2 bytes, c56a = 50538, and the port is 1024 + (8cedc967 mod 64512) = 30055. Without the secret nobody can predict them,
and after 1000 forged answers the cache says nothing. The attacker must match 1 of 65536 x 64512 = 4,227,858,432 pairs, and 1000 guesses hit about 1 time in 4,227,858.

### 5. How long a false answer lives, and what stops it

A false answer, 192.0.2.66, asks to be kept for 86400 seconds, a day. Before the fix, the resolver caps lifetimes at a day: kept until min(86400, 86400) = second 86400, and still believed after 2 hours. After the fix, the cap is 3600 seconds:
kept until min(86400, 3600) = second 3600, and gone after 2 hours. A cap does not stop the first lie, but it makes it short. DNSSEC signs the answers, so a forged one fails a check that needs no trust in the path; DNS over TLS or HTTPS hides the questions on the path to the resolver;
and the browser's check that the certificate names shop.example.com is the last line of defence. These three are described here, not run.

## The verdict

Use resolvers that randomize ids and source ports and cap TTLs, turn on DNSSEC validation where available, use encrypted DNS when privacy matters, and never treat a name's address as proof of identity: check the certificate.

## How to recognise this in code you did not write

- `dig +dnssec` showing signatures and the ad flag.
- Resolver settings for DNS over TLS or HTTPS.
- TTL fields in a domain's control panel.
- Incident reports about hijacked domains.

## Where you have already met this

- `dig` and `nslookup`, which show questions, answers and TTLs.
- "DNS_PROBE_FINISHED" and "server not found" errors in a browser.
- TTL settings in a domain's control panel, and "wait for DNS to propagate".
- News stories about hijacked domains and poisoned caches.
