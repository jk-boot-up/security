# Network Isolation, Explained

## The idea in one sentence

Isolation is a property of the network, so it is measured, and the design is a table that the measurement is compared with.

## The 5 acts

### 1. One flat network

The input: six services of the store, each a container listening on its port (web 8000, app 8080, db 5432, cache 6379, admin 9000, ci 2222), all on one network called flat.
The attack: web, the public front, is taken over. Measured from inside it with nc, it reaches app, db, cache, admin and ci by name.
How many pairs can talk: n services on one network make n x (n - 1) / 2 pairs, so 6 x 5 / 2 = 15, and 15 of 15 are measured. The design wants 6, so 9 pairs are extra.

### 2. Three networks: front, back, management

The input: three networks. front holds web and app and has a way out; back holds app, db, cache and admin and is internal; mgmt holds admin and ci and is internal. web is published on front.
The result, measured: the taken-over front reaches only app, by name and also by every address it can learn.
How many pairs can talk: front 2 x 1 / 2 = 1, back 4 x 3 / 2 = 6, mgmt 2 x 1 / 2 = 1; together 8, and 8 are measured. Against the design's 6, 2 are extra: admin-cache and cache-db, because everything on back reaches everything else on back.

### 3. The design as code, and a layout built from it

The input is the design as code: the six pairs that may talk, admin-app, admin-ci, admin-db, app-cache, app-db and app-web. The layout built from it has one internal network for each pair, link1 to link6, and an edge network with a way out that only web is on.
The measurement: 6 pairs, 0 extra, 0 missing; the front reaches only app, by name and by address.
The drift: someone with the Docker socket creates the network oops and connects web and db to it. Measured again: 7 pairs, and the extra one is db-web, found by comparing the measurement with the intent.

### 4. The edge: what is published, and what can leave

The way in: of the six services only web is published to the host, on 127.0.0.1, and the host can connect to it; app, db, cache, admin and ci are not published.
The way out: a server evil, listening on port 4444, is put on the edge network, as a stand-in for the internet. Measured from each service, only web reaches it.
How it is worked out: only edge has a way out, and only web is on edge, so the layout predicts web, and web is what is measured. A taken-over database cannot send anything out; a taken-over front still can, and that way out is the next thing to close.

### 5. The bill

The pivots, worked out from the layout of act three: web is on 2 networks, app on 4, db on 2, admin on 3. A taken-over pivot reaches every network it is on, so the pivots are where an attacker goes next.
What the network does not decide: reachable is not authorised, so the database still checks who the application is; and whoever can use the Docker socket can join anything to any network.
What it costs: one network per pair is 7 networks for 6 services, which does not scale to a hundred; at scale clouds use security groups and clusters use network policies, and they need testing the same way, against the intent, on a schedule.

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
