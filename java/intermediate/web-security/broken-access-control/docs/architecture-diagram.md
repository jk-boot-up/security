# Broken Access Control — Architecture Diagram

The naive store checks inside each handler; the guarded store checks once, before any handler, and refuses what it has no rule for.

![Where the checks live](images/architecture-diagram.png)


