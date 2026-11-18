# Insecure Deserialization with Jackson, Explained

## The idea in one sentence

Jackson builds the class that the data names whenever a field is typed by name or class, so the type must come from a list kept in the code, and values such as prices must never come from the client.

## The 5 acts

### 1. A cart in a cookie

The cart holds KTL-001 twice at 2500 cents, PAN-002 once at 4200 cents, and a Gift note. Jackson writes it as JSON: each line with its price, and the note with "@class":"com.jk.explore.security.jacksondeser.Note$Gift", because the note field is typed by class name, so a new kind of note needs no code change.
The cookie is that JSON in Base64, 244 characters. Every JSON cookie starts with eyJ, the Base64 of an opening brace and a quote.
Read back with Jackson 3's defaults, the cart has 2 lines: 2 x 2500 + 1 x 4200 = 9200 cents.

### 2. The cookie decides which class is built

The customer writes "price":1 into the JSON and the cart totals 2 x 1 = 2 cents.
Then a note whose @class is com.jk.explore.security.jacksondeser.TempCleaner, with a path. Jackson loads the class; it implements Note, so the default validator allows it; Jackson sets the path, and setPath deletes the file. The cookie is accepted as a note, and the marker file no longer exists.
Against an Object field, Jackson 3's default validator refuses class names: Object is too general a base type. That is a real protection, and it protects nothing else. With global default typing switched on and a validator that allows any class, as many copied examples do, the same TempCleaner is built from an Object root, and the file is gone again.

### 3. A list of kinds, or a list of classes

The first fix types the note by a short name from a list kept on the field: gift means Note.Gift, coupon means Note.Coupon. The honest note's JSON says "type":"gift" and reads back as 2 lines, 9200 cents. A forged note whose type is TempCleaner's class name is an unknown type id: refused, and the marker file still exists.
The second fix keeps class names but gives the mapper a validator that allows only Note.Gift and Note.Coupon. The honest cart still reads; TempCleaner is refused as a class not allowed, and the file survives.
The forged price passes both, because the price is in the cookie: a list of types stops unknown classes, not lies.

### 4. Data only, priced by the shop

The request's JSON is {"items":[{"sku":"KTL-001","qty":2},{"sku":"PAN-002","qty":1}]}: no prices and no class names. The shop looks up each price in its own catalogue: KTL-001 x2 x 2500 = 5000, PAN-002 x1 x 4200 = 4200, 2 items, 9200 cents.
The mapper refuses unknown properties, so "price":1 is refused as an unknown property, and so is "@class". A quantity of -5 is refused by the shop's range check, 1 to 99, and XXX-999 is an unknown product.

### 5. Limits on the stream

Jackson 3's default limits are 500 levels of nesting, 100,000,000 characters of text and 1,000 digits in a number. A cookie of 5000 nested arrays is refused by the default: nesting deeper than 500.
The request mapper tightens them to what a cart needs: 10 levels, 64 characters, 10 digits. Eleven levels, a 65 character product code and the 11 digit quantity 12345678901 are each refused. These limits protect memory and the stack, not only types.

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
