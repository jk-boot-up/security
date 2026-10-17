# Password Attacks and Defences with nginx — UML Sequence Diagrams

## 1. Bypassing a limit keyed on a header

Every guess carries a new X-Forwarded-For value, so every guess finds an empty bucket.

![Bypassing a limit keyed on a header](images/uml-diagram.png)

## 2. A limit per username locks the owner out

Dana's bucket was filled by the attacker, and Dana's own login uses the same bucket.

![A limit per username locks the owner out](images/uml-diagram-2.png)
