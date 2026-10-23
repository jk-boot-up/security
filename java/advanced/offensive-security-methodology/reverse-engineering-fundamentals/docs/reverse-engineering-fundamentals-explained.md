# Reverse Engineering Fundamentals, Explained

## The idea in one sentence

Whatever runs on someone else's machine is theirs to read and change.

## The 5 acts

### 1. The program

The client has a Licence class that compares the typed key with a key held in the program, a Statement class that formats lines and a Main class that starts it. With the right key it shows a statement line, with a wrong key it prints that it is denied.

### 2. Reading it without running it

The constant pool of the class files holds the strings that the code loads. Three of them contain key, licence or an address, and one is the licence key itself. javap -c shows invokevirtual String.equals followed by ifeq in a method called check. Ten of the 13 names in the files are longer than one letter.

### 3. Decompiling it

A decompiler turns the bytecode back into Java. CFR's output of the Licence class has the key and the equals call in it, in 19 non-blank lines against the 20 of the original source.

### 4. Changing it

The class file API replaces the body of the check method with two instructions, iconst_1 and ireturn. The patched program says licensed to a wrong key and to no key. A check that runs on the customer's computer decides nothing.

### 5. Obfuscation

The obfuscated copy behaves exactly like the original and shows 0 of the 3 strings; none of its names is longer than one letter. Its decompiled check is still readable as a comparison with a decoded string. Calling the program's own decoder gives back all 3 strings, and the check is found by its shape, a static method that takes a String and returns a boolean using equals and a branch, and patched the same way.

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
