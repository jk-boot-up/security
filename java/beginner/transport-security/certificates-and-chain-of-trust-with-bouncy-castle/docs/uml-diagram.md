# Certificates and Chain of Trust with Bouncy Castle and OpenSSL — UML Sequence Diagrams

## 1. Validating a chain with revocation

Every certificate needs a current list from its issuer, or the chain is refused.

![Validating a chain with revocation](images/uml-diagram.png)

## 2. A stolen intermediate key

The forged certificate chains correctly until the root revokes the intermediate.

![A stolen intermediate key](images/uml-diagram-2.png)
