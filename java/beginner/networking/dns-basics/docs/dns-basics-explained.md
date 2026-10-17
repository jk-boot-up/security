# DNS Basics for Security, Explained

## The idea in one sentence

DNS maps names to addresses in small unsigned datagrams that a resolver believes when they repeat the question, the id and the port, and caches for their lifetime, so unguessable ids, capped lifetimes, signatures and TLS are all needed to limit a false answer.

## The 5 acts

### 1. A DNS message, byte by byte

The question for shop.example.com is 34 bytes: 12 of header, then the name as labels, then type and class. Its first 12 bytes are abcd01000001000000000000. The answer repeats the name as a two-byte pointer, so it is 50 bytes and not 66.
Decoded again, shop.example.com is 203.0.113.10 for 300 seconds. Nothing in the message says who made the answer: only the id and the question have to match.

### 2. The cache and the time to live

At second 0 the answer 203.0.113.10 arrives with a time to live of 300 seconds. At second 100 the resolver answers from its cache, with one question sent upstream so far. At second 300 the entry has expired and the resolver asks again.
Caching makes DNS fast, and it also means one false answer is repeated to every customer until it expires.

### 3. A real exchange over UDP

On this machine, a client sends one datagram of 34 bytes to a tiny server and gets one of 50 bytes back. The answer arrived, repeats the id, and says 203.0.113.10. The name can be read in the query as plain text,
so anyone on the path sees which sites you ask for. There is no connection and no signature: a datagram is believed if it looks right.

### 4. Cache poisoning by guessing

A resolver that counts its ids up and sends every query from port 53 is poisoned by the first forged answer: after 1000 forged answers the cache says 192.0.2.66, the attacker's address. A resolver whose ids and ports come from a keyed hash
is not: after 1000 forged answers the cache says nothing. The attacker must match one of 4,227,858,432 pairs, and 1000 guesses are about one in 4,227,858.

### 5. How long a false answer lives, and what stops it

A false answer with a time to live of 86400 seconds is still believed after 2 hours when the resolver allows it, and not when the resolver caps lifetimes at 3600 seconds. DNSSEC signs the answers, so a forged one fails a check that needs no trust in the path.
DNS over TLS or HTTPS hides the questions and protects the path to the resolver, but not the data upstream. The browser's check that the certificate names shop.example.com is the last line of defence.

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
