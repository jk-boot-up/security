# TCP/IP Basics with tcpdump and Python, Explained

## The idea in one sentence

A capture file records what really crossed the wire in a standard format, so tools like tcpdump and short scripts can show handshakes, floods and damaged packets, and because it holds everything that was not encrypted, it must be protected like the traffic.

## The 5 acts

### 1. Recording traffic to a capture file

A customer's purchase is 5 packets and 234 bytes. In a pcap file it is 24 bytes of file header, then 16 bytes of header and the packet bytes for each packet: 338 bytes. tcpdump, Wireshark and the Python script all read the same file.

### 2. Reading it back, in the style of tcpdump

Each packet is printed as one line: source and destination with ports, the flags, the sequence number, the acknowledgement, the window and the length. S is SYN, a dot is ACK and P is PSH. The first three lines are the handshake and the fourth is the order itself.
The integration test checks that the real tcpdump prints exactly the same lines.

### 3. Finding a SYN flood in a capture

The capture has 14 packets: the purchase, then 8 SYNs from addresses that never answer, then one more. Sources that sent a SYN and never finished the handshake: 9. The real customer is not among them. They are all from the same network, which is what a flood often looks like.
An independent Python script finds the same sources.

### 4. Finding a damaged packet

One packet in the capture has a checksum that does not match. Plain tcpdump shows it like any other packet. With the verbose option it marks the checksum incorrect. A capture shows what arrived, damaged or forged, which is why it is the first thing to look at.

### 5. What a capture reveals

The capture contains the card number. A capture file is as sensitive as the traffic, so protect it like passwords and capture only what the investigation needs. Live capture needs root, so analyse recorded files without it, on a copy.
Encryption with TLS is what makes a capture useless to a thief.

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
