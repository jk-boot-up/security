# Session Management: Ids, Rotation, Timeouts and Sign-Out — UML Sequence Diagrams

## 1. Session fixation, and what stops it

The planted id is worthless once sign-in issues a new one.

![Session fixation, and what stops it](images/uml-diagram.png)

## 2. Signing out on the server

A copy of the cookie is worthless once the server has forgotten the session.

![Signing out on the server](images/uml-diagram-2.png)
