# Race Conditions and Business Logic — Architecture Diagram

Check-then-act leaves a gap between reading and writing; a lock, a compare-and-set or a single conditional update closes it.

![Where the gap is, and how it is closed](images/architecture-diagram.png)


