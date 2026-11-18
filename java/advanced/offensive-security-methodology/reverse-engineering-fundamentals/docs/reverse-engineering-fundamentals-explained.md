# Reverse Engineering Fundamentals, Explained

## The idea in one sentence

Whatever runs on someone else's machine is theirs to read and change.

## The 5 acts

### 1. The program

The input: the client, compiled by the demo from samples/plain into 3 class files (bank/Main starts it, bank/Licence checks the key, bank/Statement formats a line), and three keys a customer could type: the right key bankapp-demo-key-0001, a wrong key, and none.
The result: the right key prints a statement line for Corner Bakery, 12.50; a wrong key and no key print denied. This is the behaviour every later change is compared with.

### 2. Reading it without running it

The input: the 3 class files, never run. The constant pool of a class file holds the strings its code loads. Of the 7 strings in the three files, 3 name a key, a licence or an address, and one of them is the licence key itself.
The names: 13 classes, methods and fields, and 10 of them are longer than one letter (the other 3 are constructors, called <init>). A reader learns from bank/Licence.check and VENDOR_KEY where to look.
The bytecode of check, from javap -c: offsets 0 to 5 return false for no key; 6 loads the key from the pool with ldc; 9 calls String.equals; 12, ifeq, jumps to 17 (return 0, false) when they differ; 15 and 16 return 1, true.

### 3. Decompiling it

The input: the check method as the bank wrote it, nine lines with two if statements, and its compiled class file.
The result: CFR 0.152, given only Licence.class, prints the method back in five lines: the same null check, then return VENDOR_KEY.equals(string). The field VENDOR_KEY = "bankapp-demo-key-0001" comes back too. Lost: the parameter name, key, which the class file does not keep, so CFR calls it string. The whole file is 19 non-blank lines against 20 in the original.

### 4. Changing it

Before the patch: check is 12 instructions in 19 bytes; the right key is licensed, a wrong key and no key are denied.
The patch: Patcher looks for a static method that takes a String, returns a boolean, calls String.equals and branches with ifeq, and finds bank/Licence.check. With the JDK class file API it writes a copy whose body is two instructions, iconst_1 and ireturn: push 1, true, and return it.
After the patch: check is 2 instructions in 2 bytes, and all three keys, wrong or none, are licensed. A check that runs on the customer's computer decides nothing.

### 5. Obfuscation

Before obfuscation: 10 of 13 names are readable and 3 strings name a key, a licence or an address.
After obfuscation (the same behaviour for every key): bank/Licence.check is a/a.a, VENDOR_KEY is k, 0 of 13 names are readable and 0 strings name a key. The pool holds hex instead, 383b3431... for the key.
How one string is decoded: the decoder a/c.d reads two hex digits as a byte and XORs it with 0x5a. 38 XOR 5a is 62, the letter b; 3b gives 61, a; 34 gives 6e, n; 31 gives 6b, k. All 21 bytes give bankapp-demo-key-0001. XorSteps redoes this by hand, and a test proves it matches the program's decoder.
What is still there: the decompiled check reads return k.equals(string); calling the program's own decoder on its 6 encoded strings gives back the 3 that name a key, a licence or an address; and the key check is found by its shape although it is called a, patched, and a wrong key is licensed.

## The verdict

Keep secrets and decisions on the server, assume every client is read and patched, treat obfuscation as a delay and not as a lock, and read unknown code in the order of cost: strings, names, bytecode, then a decompiler.

## How to recognise this in code you did not write

- A key in a mobile app or a jar.
- A licence check that returns a boolean on the client.
- Obfuscation as the only protection.
- A check that the client does on itself.

## Where you have already met this

- An API key in a mobile app.
- A licence check that returns a boolean.
- A password in a jar file.
- Obfuscation sold as security.
