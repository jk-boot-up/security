# Networking

What travels between machines, and what none of it promises: packets, names and the web. Each project shows what the protocol does, how it is attacked, and which defence goes where.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [tcp-ip-basics](tcp-ip-basics) | Packets, checksums, the three-way handshake, SYN floods and guessable sequence numbers |
| 2 | [tcp-ip-basics-with-tcpdump](tcp-ip-basics-with-tcpdump) | The same traffic written as a pcap file, read by the real tcpdump and an independent Python flood detector |
| 3 | [dns-basics](dns-basics) | DNS messages with name compression, TTL caching, a real UDP exchange, and cache poisoning against guessable ids and ports |
| 4 | [dns-basics-with-coredns](dns-basics-with-coredns) | A real CoreDNS in Docker queried over UDP and TCP: NXDOMAIN, rewrite, caching, and a zone serving a wrong record |
| 5 | [http-basics-and-headers](http-basics-and-headers) | An HTTP/1.1 parser, ambiguous lengths refused, header injection, cookie attributes and a security-header audit |
| 6 | [http-basics-and-headers-with-spring-boot](http-basics-and-headers-with-spring-boot) | The same audit against real Spring Boot and Spring Security responses |
| 7 | [http-basics-and-headers-with-nginx](http-basics-and-headers-with-nginx) | nginx headers, the add_header inheritance trap, size limits and smuggling refusals, in Docker |

A project with `-with-` in its name is the same lesson with a framework, a library or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it first.** Each project shows the naive version failing before any control appears.
- **Every claim is a number.** Each failure and each fix prints an exact result that a test checks, against a golden output file.
- **The bill is part of the lesson.** The last act of every project shows what the control costs, or what it still leaves open.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [learning path](../learning-path.md) for the order of all projects.
