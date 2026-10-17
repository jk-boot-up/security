# Problem Statement

## The scenario

An online store's customer sends a card number across networks the store does not control. The two sides have never met, and anyone on the path can listen, record and change what passes.

## The naive version

Send the order as it is, or encrypt it with a key that was sent first in the same open connection.

## What this project must deliver

- A comparison of what a sniffer sees without and with TLS.
- A handshake between two real TLS engines, with the wire recorded.
- A client that refuses an untrusted certificate and a wrong name.
- A record that is refused when one bit changes.
- Old protocol versions that are refused, and the TLS 1.2 against TLS 1.3 choice.
- The same facts printed on every run although keys and nonces are random.
