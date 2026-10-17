# Password Attacks and Defences with nginx — Architecture Diagram

The attacker's guesses meet nginx first. The limit zone counts them by a key, and only those within the limit reach the login stub.

![A login behind nginx](images/architecture-diagram.png)


