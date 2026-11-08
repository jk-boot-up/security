# TCP/IP Basics with tcpdump and Python, Explained

## The idea in one sentence

A capture file records what really crossed the wire in a standard format, so tools like tcpdump and short scripts can show handshakes, floods and damaged packets, and because it holds everything that was not encrypted, it must be protected like the traffic.

## The 6 acts

### 1. Recording a purchase to a capture file

Before recording, the purchase is five packets: four of 40 bytes (an IP header of 20 bytes and a TCP header of 20 bytes, no data) and the order of 74 bytes, which carries 34 bytes of data: 234 bytes in all.
After recording, the pcap file holds a 24-byte file header, then for each packet a 16-byte record header (its time and length) and its bytes: 24 + 5 x 16 + 234 = 338 bytes. tcpdump, Wireshark and the Python script all read the same file.

### 2. Reading it back, in the style of tcpdump

What tcpdump prints is one line per packet (shown here over two lines): source and destination with ports, the flags, the sequence number, the acknowledgement, the window and the length. S is SYN, a dot is ACK and P is PSH. The first three packets are the handshake and the fourth is the order.
How the numbers follow: the shop's ack 1001 is the customer's seq 1000 plus 1, because a SYN counts as one byte; the customer's ack 7001 is the shop's seq 7000 plus 1; and the shop's last ack 1035 is 1001 plus the 34 bytes of the order. The integration test checks that the real tcpdump prints exactly the same lines.

### 3. Finding a SYN flood in a capture

The capture has 14 packets: the purchase (5), a burst of 8 SYNs, and one more SYN. The finding: for each source that sent a SYN, did it send the final ACK? The customer 198.51.100.7 did. Nine sources never finished: 100.64.0.1 to 100.64.0.8 and 100.64.0.99.
The real customer is not among them. A flood is told by handshakes that never finish, not by where they come from, because its addresses are often forged. An independent Python script finds the same sources.

### 4. How a checksum is made, step by step

The packet is the last SYN, from 100.64.0.99, as it was sent. Step 1: the bytes are split into 16-bit words: a pseudo-header (the two addresses, the protocol number 6 for TCP and the TCP length, 20) and the 20-byte TCP header with its checksum field set to 0000.
Step 2: the sixteen words are added up: 38f2d, one hex digit too long for 16 bits. Step 3: the carry is folded back in: 8f2d + 3 = 8f30. Step 4: every bit is flipped: 70cf, the checksum the packet carries. The receiver does the same sum to check it.

### 5. Finding a damaged packet

Before the damage, the SYN carries the checksum 70cf and working it out again gives 70cf: a match. After the damage, one byte changed on the way, so the packet carries 25cf, while working it out again still gives 70cf: no match.
One packet in the whole capture fails its checksum. Plain tcpdump shows it like any other packet; with the verbose option -v it marks the checksum incorrect. A capture shows what arrived, damaged or forged.

### 6. What a capture reveals

What the customer sent is the order card=4111111111111111&order=A-1001, without TLS. What the capture holds, in packet 4, bytes 40 to 73, is exactly the same text. A capture file is as sensitive as the traffic, so protect it like a password and capture only what the investigation needs.
Live capture needs special rights, usually root, so analyse recorded files, on a copy. Encryption with TLS is what makes a capture useless to a thief.

## The verdict

Learn to read a capture with tcpdump and Wireshark, keep captures short and filtered, store them securely, and delete them when the investigation ends.

## How to recognise this in code you did not write

- `tcpdump -nn -i eth0 port 443` in runbooks.
- `.pcap` files in incident reports.
- Cloud traffic mirroring and VPC flow logs.

## Where you have already met this

- `tcpdump` and Wireshark in every network troubleshooting guide.
- `.pcap` and `.pcapng` files attached to incident reports.
- Intrusion detection systems, which analyse traffic in the same way.
- Cloud "traffic mirroring" and "flow logs", which are captures with less detail.
