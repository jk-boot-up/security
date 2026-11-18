# Insecure Deserialization and Its Defences, Explained

## The idea in one sentence

Rebuilding objects from bytes the customer controls runs the customer's choice of code, so allow only the classes you expect before they are built, and prefer data-only, signed formats where prices and rights come from the server.

## The 4 acts

### 1. A cart in a cookie

The cart holds KTL-001 twice at 2500 cents and PAN-002 once at 4200 cents. The first version writes the whole Java object, prices included, into the cookie and encodes the bytes in Base64: 398 characters that start with rO0ABXNy.
Those first six bytes, AC ED 00 05 73 72 in hex, are Java serialization's magic number, its version 5, a new object and its class description. Every serialized Java cookie starts with rO0.
Reading it back, the stream names four classes and the server builds each one: Cart, ArrayList, Object[] and Cart$Line. The result is 2 lines, 2 x 2500 + 1 x 4200 = 9200 cents. It works, and that is why the design survives.

### 2. The cookie decides what the server builds

The customer writes a cart of their own: KTL-001 twice at 1 cent. The server reads it and believes it: 1 line, 2 x 1 = 2 cents.
Then a cookie that is not a cart at all: a TempCleaner, pointing at a marker file on the server. The stream names one class, TempCleaner, and the server builds it. Its readObject runs inside the call that reads the cookie and deletes the file.
Only then does the server see that the object is not a cart, and refuse it: "the cookie is not a cart", and "marker file exists afterwards: false". Checking the type after readObject is too late.

### 3. An allow-list filter names the classes of a cart

An ObjectInputFilter is set on the stream before reading. Its allow-list names the classes a cart is made of (Cart, Cart$Line, ArrayList and a few basic types), limits depth, references, array size and bytes, and ends with !*, which refuses everything else.
The filter is asked about every class before it is built. For the honest cart, Cart, ArrayList, Object[] and Cart$Line are all ALLOWED, and the cart reads as before: 2 lines, 9200 cents.
For the TempCleaner cookie, the answer is REJECTED, so the class is never built: "class refused by the filter", and the marker file still exists. The forged price uses only a cart's classes, so it is all ALLOWED and still accepted at 2 cents: a filter stops unknown classes, not lies.

### 4. Data only: product codes and quantities, signed

The cookie now holds text only, with no classes and no prices: v1;KTL-001x2;PAN-002x1. The text is encoded in Base64 as the body, and the server signs the body with HMAC-SHA-256 and a key only it holds. The cookie is body.signature.
When it comes back, the server recomputes the HMAC of the body and compares it with the cookie's signature in constant time, before parsing anything. Then it reads each line with a strict pattern and looks the prices up itself: 2 x 2500 = 5000, 1 x 4200 = 4200, 9200 cents.
The customer changes x2 to x9. The server's HMAC of the new body is 21AurWKC..., the cookie still carries XJD4Phz3..., so the cookie is refused: bad signature. A serialized object is not a signed cookie at all, and unsigned text fails the signature too.

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
