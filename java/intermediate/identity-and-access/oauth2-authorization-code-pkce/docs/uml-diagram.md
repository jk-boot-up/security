# OAuth 2.0 Authorization Code Flow with PKCE — UML Sequence Diagrams

## 1. A leaked code without the verifier

The code alone is not enough: the token endpoint hashes the verifier and compares.

![A leaked code without the verifier](images/uml-diagram.png)

## 2. A stolen refresh token is reused

Rotation makes the old token single-use; its reuse revokes the family.

![A stolen refresh token is reused](images/uml-diagram-2.png)
