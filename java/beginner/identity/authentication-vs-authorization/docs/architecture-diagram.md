# Authentication vs Authorization — Architecture Diagram

A request meets the gate: first authentication (who is this, 401 if nobody), then authorization (may this person do this, 403 if not), and only then the shop acts.

![Two questions, in order, at the gate](images/architecture-diagram.png)


