# TCP/IP Basics for Security, Explained

## The idea in one sentence

TCP/IP carries bytes between addresses reliably, but a source address can be forged, a checksum has no key, the handshake costs the server memory, and the stream is readable by anyone on the path, so defences must be added at other layers.

## The 5 acts

### 1. Layers: a packet inside a packet

A customer's browser sends a SYN to the store's port 443. The TCP segment is 20 bytes, and inside an IP packet it is 40 bytes. Decoded again, it says from 198.51.100.7 to 203.0.113.10, ports 51000 to 443, with the SYN flag set.
Nothing in either header says who the sender really is: the source address is just a field.

### 2. A checksum catches accidents, not attackers

One byte changed in transit, the time to live, is refused because the header checksum does not match. The same change made by someone who recomputes the checksum is accepted. The checksum has no key, so anyone can make it match,
and it is no protection against an attacker.

### 3. The three-way handshake

The customer sends SYN with sequence number 1000. The store answers SYN-ACK acknowledging 1001, and now holds a half-open entry. The customer answers ACK and the connection is established, with no half-open entries left.
An ACK with no SYN before it does not establish anything.

### 4. A SYN flood

Eight SYNs from addresses that never answer fill the store's table of eight half-open entries. A real customer's SYN is then not answered: the store is denying service to its customers. With SYN cookies the store keeps no entry,
because the state is encoded in its own sequence number, and the real customer connects. The cost is that cookies carry no options and take work for every SYN.

### 5. Guessable numbers, and a stream anyone can read

If the store picks its sequence numbers with a counter that adds 64000 each time, an attacker who sends a SYN pretending to be the admin machine guesses the next number from its own connection and is accepted as the admin: a forged connection.
With a keyed hash the same guess fails. And on a real connection on this machine, a sniffer on the wire sees the card number: TCP delivers bytes reliably and in order, and hides nothing. Secrecy and identity come from TLS.

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
