# Password Hashing — Architecture Diagram

At registration the hasher adds a salt and a slow hash; at login it repeats the work with the stored salt and rounds and compares.

![From a password to a stored hash](images/architecture-diagram.png)


