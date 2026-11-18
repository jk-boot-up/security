# Bank payments

The online bank: a mobile app, an API gateway, a payments service, two stores and a card processor

## Findings

* INP02 Mobile app: Overflow Buffers
* AA01 Mobile app: Authentication Abuse/ByPass
* DE02 Mobile app: Double Encoding
* AC01 Mobile app: Privilege Abuse
* INP08 Mobile app: Format String Injection
* INP12 Mobile app: Client-side Injection-induced Buffer Overflow
* INP13 Mobile app: Command Delimiters
* INP14 Mobile app: Input Data Manipulation
* CR03 Mobile app: Dictionary-based Password Attack
* AA02 Mobile app: Principal Spoof
* INP20 Mobile app: iFrame Overlay
* INP23 Mobile app: File Content Injection
* AC12 Mobile app: Privilege Escalation
* AC13 Mobile app: Hijacking a privileged process
* AC14 Mobile app: Catching exception throw/signal from privileged block
* INP24 Mobile app: Filter Failure through Buffer Overflow
* INP25 Mobile app: Resource Injection
* INP26 Mobile app: Code Injection
* INP27 Mobile app: XSS Targeting HTML Attributes
* INP28 Mobile app: XSS Targeting URI Placeholders
* INP29 Mobile app: XSS Using Doubled Characters
* INP30 Mobile app: XSS Using Invalid Characters
* INP31 Mobile app: Command Injection
* INP32 Mobile app: XML Injection
* INP33 Mobile app: Remote Code Inclusion
* INP35 Mobile app: Leverage Alternate Encoding
* AC15 Mobile app: Schema Poisoning
* INP41 Mobile app: Argument Injection
* AC21 Mobile app: Cross Site Request Forgery
* AA01 API gateway: Authentication Abuse/ByPass
* AC01 API gateway: Privilege Abuse
* AA02 API gateway: Principal Spoof
* CR05 API gateway: Encryption Brute Forcing
* SC05 API gateway: Removing Important Client Functionality
* INP17 API gateway: XSS Using MIME Type Mismatch
* AA03 API gateway: Exploitation of Trusted Credentials
* AC09 API gateway: Functionality Misuse
* INP19 API gateway: XML External Entities Blowup
* INP21 API gateway: DTD Injection
* INP22 API gateway: XML Attribute Blowup
* AA01 Payments service: Authentication Abuse/ByPass
* AC01 Payments service: Privilege Abuse
* AA02 Payments service: Principal Spoof
* CR05 Payments service: Encryption Brute Forcing
* SC05 Payments service: Removing Important Client Functionality
* INP17 Payments service: XSS Using MIME Type Mismatch
* AA03 Payments service: Exploitation of Trusted Credentials
* AC09 Payments service: Functionality Misuse
* INP19 Payments service: XML External Entities Blowup
* INP21 Payments service: DTD Injection
* INP22 Payments service: XML Attribute Blowup
* AC01 Accounts database: Privilege Abuse
* CR05 Accounts database: Encryption Brute Forcing
* DE04 Accounts database: Audit Log Manipulation
* AC01 Audit log: Privilege Abuse
* CR05 Audit log: Encryption Brute Forcing
* DE04 Audit log: Audit Log Manipulation
* DE01 Login and payment request: Interception
* AC05 Login and payment request: Content Spoofing
* DE03 Login and payment request: Sniffing Attacks
* CR06 Login and payment request: Communication Channel Manipulation
* CR08 Login and payment request: Client-Server Protocol Manipulation
* DE01 Payment request with token: Interception
* AC05 Payment request with token: Content Spoofing
* DE03 Payment request with token: Sniffing Attacks
* CR06 Payment request with token: Communication Channel Manipulation
* CR08 Payment request with token: Client-Server Protocol Manipulation
* DE01 Authorized payment: Interception
* AC05 Authorized payment: Content Spoofing
* DE03 Authorized payment: Sniffing Attacks
* CR06 Authorized payment: Communication Channel Manipulation
* CR08 Authorized payment: Client-Server Protocol Manipulation
* DE01 Balance update: Interception
* AC05 Balance update: Content Spoofing
* DE03 Balance update: Sniffing Attacks
* CR06 Balance update: Communication Channel Manipulation
* DE01 Payment record: Interception
* AC05 Payment record: Content Spoofing
* DE03 Payment record: Sniffing Attacks
* CR06 Payment record: Communication Channel Manipulation
* DE01 Card charge: Interception
* AC05 Card charge: Content Spoofing
* DE03 Card charge: Sniffing Attacks
* CR06 Card charge: Communication Channel Manipulation
* CR08 Card charge: Client-Server Protocol Manipulation
* DE01 Charge result: Interception
* AC05 Charge result: Content Spoofing
* DE03 Charge result: Sniffing Attacks
* CR06 Charge result: Communication Channel Manipulation
* CR08 Charge result: Client-Server Protocol Manipulation
* DE01 Request record: Interception
* AC05 Request record: Content Spoofing
* DE03 Request record: Sniffing Attacks
* CR06 Request record: Communication Channel Manipulation


