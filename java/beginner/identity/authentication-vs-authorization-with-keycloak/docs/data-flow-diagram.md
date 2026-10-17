# Authentication vs Authorization with Keycloak — Data Flow Diagram

Any failed check is a 401. Only a token that passes all of them reaches the shop's own rules, which can answer 403.

![What the shop checks, in order](images/data-flow-diagram.png)


