# Insecure Deserialization and Its Defences, Explained

## The idea in one sentence

Rebuilding objects from bytes the customer controls runs the customer's choice of code, so allow only the classes you expect before they are built, and prefer data-only, signed formats where prices and rights come from the server.

## The 4 acts

### 1. A cart in a cookie

The first version writes the Java object into the cookie, with each line's price, and reads it back with `readObject`. A cart of a kettle (twice) and a pan comes back as
2 lines, 9200 cents. It works, and that is why the design survives.

### 2. The cookie decides what the server builds

The customer edits the price and the cart totals 2 cents. A cookie that holds a `TempCleaner` object is worse: its `readObject` runs inside the call that reads the cookie and removes the marker
file. Afterwards the server notices that the object is not a cart and refuses it, but the file is already gone: "marker file exists afterwards: false". Checking the type after
the read is too late.

### 3. An allow-list filter names the classes a cart is made of

An `ObjectInputFilter` with an allow-list (Cart, its lines, ArrayList, a few basic types, and `!*` for everything else) plus limits on depth, objects, array size and bytes is set on the stream.
The cleaner is rejected before it is built: "filter status: REJECTED", and the marker file still exists. Honest carts are read as before. The forged price is still accepted, because the classes are all allowed:
a filter protects against code, not against lies.

### 4. Data only: names and quantities, signed

The cookie now holds `v1;KTL-001x2;PAN-002x1` in Base64 and an HMAC-SHA-256 signature. The server checks the signature in constant time first, then parses each line with a strict pattern,
and looks the prices up itself, so there is nothing to forge. A serialized object is not a signed cookie. A changed quantity fails the signature. Unsigned text fails it too.

## The verdict

Do not deserialize Java objects from untrusted sources. If you must, set an allow-list `ObjectInputFilter` with limits (and a process-wide filter as a safety net). Prefer a data-only format such as JSON with a strict schema,
sign anything that a customer carries, and keep prices and permissions on the server.

## How to recognise this in code you did not write

- `ObjectInputStream.readObject()` on request data, cookies or message queues.
- Base64 starting with `rO0`.
- `ObjectInputFilter` and `jdk.serialFilter`.
- A finding titled "Deserialization of Untrusted Data" with CWE-502.

## Where you have already met this

- `new ObjectInputStream(request.getInputStream()).readObject()` in older code and tutorials.
- Base64 text in cookies or hidden fields that starts with `rO0` (the Base64 of Java's serialization header).
- Tools that generate gadget chains for common libraries.
- A finding titled "Deserialization of Untrusted Data" with CWE-502.
