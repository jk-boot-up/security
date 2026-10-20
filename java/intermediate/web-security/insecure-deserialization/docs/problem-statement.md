# Problem Statement

## The scenario

An online store keeps each customer's shopping cart in a cookie so that no server has to remember it.

## The naive version

Serialize the cart object into the cookie and read it back with `readObject`: it is the same object we wrote.

## What this project must deliver

- A cart cookie written and read with real Java serialization.
- A gadget class that runs code while it is being read, and a forged price.
- An `ObjectInputFilter` allow-list with limits.
- A data-only, HMAC-signed cookie with server-side prices.
