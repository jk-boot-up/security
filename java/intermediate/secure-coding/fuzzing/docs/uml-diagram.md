# Fuzzing: Finding the Inputs Nobody Tested — UML Sequence Diagrams

## 1. Shrinking a crash

Delta debugging removes pieces while the crash stays the same.

![Shrinking a crash](images/uml-diagram.png)

## 2. Fixing and fuzzing again

The smallest inputs become tests; the fixed parser refuses them, and the fuzzer finds nothing new.

![Fixing and fuzzing again](images/uml-diagram-2.png)
