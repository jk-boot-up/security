# DNS Basics with CoreDNS, Explained

## The idea in one sentence

A DNS server answers exactly what its zone says, over UDP or TCP, with no proof of who wrote it, and caches repeat the answer until its time runs out, so the file and the people who can change it are what protect a name.

## The 5 acts

### 1. A real DNS server, in a container

The zone file for shop.example says www.shop.example is 203.0.113.10 for 300 seconds. Asked over UDP, CoreDNS answers with that address. The answer repeats the question's id and says it is a response. The client trusts it because the id and the question match, and nothing else.
The program then shows both messages byte by byte. The question is 34 bytes: a 12-byte header (id 10 01, flags, counts), the name written as lengths and letters (03 www 04 shop 07 example 00), the type A and the class IN.
The answer is 50 bytes: the same id, response flags, the question again, then the record: a two-byte pointer back to the name at byte 12, type and class, the time to live 00 00 01 2c (1 × 256 + 44 = 300 seconds), the length 4, and the address cb 00 71 0a, one byte per number: 203.0.113.10. Nothing in either message is signed.

### 2. The same question over UDP and over TCP

Asked over TCP, the server gives the same address. UDP sends the 34-byte question alone, in one datagram. TCP is a stream with no edges, so each message starts with its length in two bytes: 00 22, which is 34 in hexadecimal. So the TCP answer is 2 bytes longer. TCP is used when an answer does not fit in a datagram, and to copy a whole zone between servers. Neither one signs or encrypts anything.

### 3. Names that do not exist, names it will not answer, and a rewrite

nope.shop.example does not exist, so the answer is NXDOMAIN with no answers. www.other.example is outside the zone, so the answer is REFUSED, because the server only answers for shop.example. The old name legacy.shop.example is rewritten to www.shop.example, and
the answer is 203.0.113.10 under the name asked for. A rewrite is a rule in the server, invisible to the client.

### 4. The cache and the countdown of the time to live

The cache plugin keeps answers for at most 60 seconds, so the zone's 300 is cut to 60: the smaller of the two. Each answer from the cache has a smaller time to live than the one before, because the cache counts down. A cache makes DNS fast, and keeps a false answer until its time runs out.

### 5. A zone with a wrong record

Someone changes the record for www.shop.example to 192.0.2.66 with a time to live of 86400 seconds: 24 hours × 60 × 60, one day. CoreDNS serves it exactly as it serves the right one: the server cannot tell. The cache setting cuts the 86400 seconds to 60 in what it hands out, but a resolver that does not
cap would keep it for a day. Fixing the file fixes the server, not the caches. The defences are around the file: who may change it, review of changes and short times to live for important names. DNSSEC signs the records so that one forged on the way can be seen, but it signs whatever is in the zone, so a wrong record in the file is signed too; it is described here, not run.

## The verdict

Restrict and review changes to zone files, keep important names on short times to live, sign the zone with DNSSEC where the provider supports it, and watch for changes to records that you did not make.

## How to recognise this in code you did not write

- `dig`, `nslookup` and `host` in every troubleshooting guide.
- CoreDNS configuration in Kubernetes clusters.
- Zone files starting with `$ORIGIN` and an SOA record.
- Incidents where a record was changed and the old answer lingered for hours.

## Where you have already met this

- `dig www.shop.example` and `nslookup`, which send exactly these questions.
- Kubernetes clusters, where CoreDNS is the default name service.
- Corefile and zone-file syntax, shared with many DNS servers.
- Short times to live on names that must be able to change quickly.
