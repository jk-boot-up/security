# Symmetric Encryption with Google Tink — UML Sequence Diagrams

## 1. Writing the keyset to disk

The master key encrypts the keyset, so the file holds no key bytes in the clear.

![Writing the keyset to disk](images/uml-diagram.png)

## 2. A message that will not open

Wrong key, changed bit or different associated data all end in the same refusal.

![A message that will not open](images/uml-diagram-2.png)
