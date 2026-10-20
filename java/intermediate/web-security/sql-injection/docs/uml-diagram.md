# SQL Injection and Its Defences — UML Sequence Diagrams

## 1. The parameter keeps it text

The statement and the value are sent separately.

![The parameter keeps it text](images/uml-diagram.png)

## 2. Least privilege stops a mistake

The search user cannot read users, even if the query is vulnerable.

![Least privilege stops a mistake](images/uml-diagram-2.png)
