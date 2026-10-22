# Insecure Deserialization with Jackson, Explained

## The idea in one sentence

Jackson builds the class that the data names whenever a field is typed by name or class, so the type must come from a list kept in the code, and values such as prices must never come from the client.

## The 5 acts

### 1. A cart in a cookie

The cart is a JSON cookie: two lines with their prices and a note that names its own class, so new kinds of note need no code change. The honest cart reads back with two lines and a total of 9200 cents.

### 2. The cookie decides which class is built

Against an Object field, Jackson 3's default validator refuses class names. That is a real protection, and it protects nothing else. The note field has the shop's own interface as its type, and another team's TempCleaner class implements it, so the cookie that names TempCleaner is accepted as a
note and the file named in the cookie is deleted. The global default typing switch, with the permissive validator many examples use, builds the same class from an Object root. A forged price is accepted too.

### 3. A list of kinds, or a list of classes

Typing the note by a short name from a list on the field (gift, coupon) makes TempCleaner an unknown type id: refused, and the marker file is still there. Keeping class names but giving the mapper a validator that allows only the Gift and Coupon classes refuses it as a class not allowed.
Honest carts round-trip in both. The forged price is still accepted, because the price is in the cookie: a type fix is not an integrity fix.

### 4. Data only, priced by the shop

The request has only product codes and quantities, and the mapper refuses an unknown property. A forged price is an unknown property, and so is a class name. A quantity of -5 and an unknown product are refused by the shop's own checks, and the total, 9200 cents, comes from the shop's catalogue.

### 5. Limits on the stream

A cookie of 5000 nested arrays is refused by Jackson's default limit of 500 levels. The request mapper tightens the limits to what a cart needs: 10 levels, 64 characters of text and 10 digits in a number, and each is refused with a fixed reason. These limits protect memory and stack, not only types.

## The verdict

Do not use class names in client data. Use short names from a list, or a validator that lists classes, or no types at all; bind client requests strictly; price on the server; and keep stream limits tight.

## How to recognise this in code you did not write

- `@JsonTypeInfo(use = Id.CLASS)` on a field.
- `activateDefaultTyping` in a configuration.
- `FAIL_ON_UNKNOWN_PROPERTIES` turned off.
- A Jackson gadget CVE in a security feed.

## Where you have already met this

- `@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)` in older code.
- `activateDefaultTyping(...)` in a copied configuration.
- A CVE for a gadget class in a Jackson-using library.
- `FAIL_ON_UNKNOWN_PROPERTIES` in configuration.
