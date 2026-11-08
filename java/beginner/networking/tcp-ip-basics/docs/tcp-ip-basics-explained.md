# TCP/IP Basics for Security, Explained

## The idea in one sentence

TCP/IP carries bytes between addresses reliably, but a source address can be forged, a checksum has no key, the handshake costs the server memory, and the stream is readable by anyone on the path, so defences must be added at other layers.

## The 5 acts

### 1. Layers: a packet inside a packet

A customer's browser sends a SYN to the store's port 443. The packet sent: a TCP segment of 20 bytes (ports 51000 to 443, the SYN flag, sequence number 1000) inside an IP packet of 40 bytes (from 198.51.100.7 to 203.0.113.10, time to live 64).
The packet received: the store decodes the 40 bytes and gets the same values back. Nothing in either header says who the sender really is: the source address is four bytes the sender writes itself.

### 2. A checksum catches accidents, not attackers

The checksum is made from the header with its checksum field set to 0000: add the ten 16-bit words (2eb72), fold the overflow back in (eb72 + 2 = eb74), and flip every bit (ffff - eb74 = 148b).
Before the change the packet is accepted. After the time to live is changed from 64 to 1, the packet with the old checksum is refused, but an attacker who redoes the three steps writes 538b and the packet is accepted.
The checksum has no key, so it is no protection against an attacker.

### 3. The three-way handshake

The request: the customer sends SYN with sequence number 1000. The response: the store answers SYN-ACK with its own sequence number, 3120261316, and acknowledges 1001, the customer's number plus one; it now holds one half-open entry.
The request: the customer answers ACK with 3120261317, the store's number plus one, which proves it saw the SYN-ACK. The result: the connection is established and no half-open entries are left. An ACK with no SYN before it establishes nothing.

### 4. A SYN flood

Before the fix: eight SYNs from addresses that never answer fill the store's table of eight half-open entries, and a real customer's SYN is not answered: the store is denying service to its customers.
After the fix, SYN cookies: the store keeps no entry. Its sequence number, the cookie, is an HMAC of a secret, the customer's address, port and sequence number: 1053972776. The ACK brings 1053972777, the cookie plus one, the store recomputes the HMAC, and the customer connects.
The cost: a cookie carries only a few TCP options, such as the maximum segment size, and every SYN costs a hash, so cookies are switched on when the table is full.

### 5. Guessable numbers, and a stream anyone can read

Before the fix, the store picks its sequence numbers with a counter that adds 64000. The attacker connects as itself and sees 1000, guesses 65000 for the SYN-ACK sent to the admin machine 10.0.0.5, which it cannot see, and its forged ACK is accepted: a forged connection.
After the fix, a keyed hash (RFC 6528): the attacker sees 692804923 and guesses 692868923, but the store sent 1773183168, and the forgery fails.
On a real connection on this machine, the stream sent and what a sniffer on the wire sees are the same text, card number included: TCP delivers bytes reliably and in order, and hides nothing. Secrecy and identity come from TLS.

## The verdict

Let the operating system's defences work: SYN cookies, keyed sequence numbers, ingress filtering. Rate-limit and filter at the edge. Never treat a source address as proof of identity, and never treat a TCP connection as private: use TLS.

## How to recognise this in code you did not write

- `ss -tan` showing many connections in SYN_RECV.
- `net.ipv4.tcp_syncookies` in kernel settings.
- Firewall and cloud rules written in terms of source addresses.
- Packet captures where a password or card number is readable.

## Where you have already met this

- `netstat` and `ss`, which list connections and their states, such as SYN_RECV and ESTABLISHED.
- `tcpdump` and Wireshark, which show packets with their flags.
- Firewall rules that say "drop packets from this source".
- News stories about SYN floods and denial-of-service attacks.
