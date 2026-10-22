# Network Isolation, Explained

## The idea in one sentence

Isolation is a property of the network, so it is measured, and the design is a table that the measurement is compared with.

## The 5 acts

### 1. One flat network

Six containers on one Docker network, each listening on its port. The front is taken over; what it reaches by name: app, db, cache, admin and ci. Pairs that can talk, in either direction: 15 of 15; the design wanted 6.

### 2. Three networks: front, back, management

Web and app on front; app, db, cache and admin on back, an internal network with no route out; admin and ci on management. The front is taken over: it reaches the application only, by name and by address after scanning every address it can learn. Pairs that can talk: 8 of 15, the design wanted 6, extra: 2 (admin-cache and cache-db), missing: 0. Everything on a network can talk to everything else on it.

### 3. The design as code, and a layout built from it

The intent is six pairs: admin-app, admin-ci, admin-db, app-cache, app-db and app-web. One internal network for each pair, each service on the networks of its pairs: measured 6 pairs, extra 0, missing 0. The taken-over front reaches the application only, by name and by address. Someone with the Docker socket joins web and db to one more network; measuring against the intent finds db-web at once.

### 4. The edge: what is published, and what can leave

Published to the host: web yes, app no, db no, and the host can open web's port. With an attacker's server on the internet side, the services that can reach it: web only. Internal networks have no way out, so a taken-over database cannot send anything out; a taken-over front still can, and its way out is the next thing to close.

### 5. The bill

Names are scoped to networks: a service on two networks, and app is on four, is a pivot. Reachable is not authorised. Whoever can use the Docker socket can join any container to any network. One network for each pair does not scale to a hundred services, which is why clouds have security groups and clusters have network policies, and why those need testing as this does. Measure against the intent in the build and on a schedule.

## The verdict

Write down which services may talk, build the network from that table, measure from inside on every change and on a schedule, close egress as carefully as ingress, treat a service on two networks as a bridge, and remember that reachable is not authorised.

## How to recognise this in code you did not write

- A compose file with one network.
- `internal: true` on a network.
- A security group that names another security group.
- A diagram of the network that nobody has checked.

## Where you have already met this

- A compose file with a single default network.
- A security group with 0.0.0.0/0 on every port.
- An internal flag on a Docker network.
- A firewall change that was made in the console and never written down.
