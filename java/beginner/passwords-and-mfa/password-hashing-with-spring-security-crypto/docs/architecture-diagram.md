# Password Hashing with Spring Security Crypto — Architecture Diagram

The shop calls one encoder; the delegating encoder chooses the algorithm, tags the text with its name, and checks old texts with the algorithm they name.

![From a password to a stored text, with a library](images/architecture-diagram.png)


