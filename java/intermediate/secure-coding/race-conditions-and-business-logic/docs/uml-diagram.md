# Race Conditions and Business Logic — UML Sequence Diagrams

## 1. A retried payment with an idempotency key

The key is claimed before the money moves; the second copy gets the first answer.

![A retried payment with an idempotency key](images/uml-diagram.png)

## 2. A deadlock

Each transfer holds one lock and waits for the other.

![A deadlock](images/uml-diagram-2.png)
