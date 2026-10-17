# Security Logging — Architecture Diagram

Every event passes through the redactor and the JSON writer, is chained to the line before it, and is written locally and shipped elsewhere at once.

![From an event to a trustworthy line](images/architecture-diagram.png)


