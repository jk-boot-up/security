# Server-Side Request Forgery (SSRF) and Its Defences — UML Sequence Diagrams

## 1. DNS rebinding against check-then-connect

The check sees the first answer, the connection uses the second.

![DNS rebinding against check-then-connect](images/uml-diagram.png)

## 2. Checking and connecting to the same address

The name is looked up once; the connection goes to that answer.

![Checking and connecting to the same address](images/uml-diagram-2.png)
