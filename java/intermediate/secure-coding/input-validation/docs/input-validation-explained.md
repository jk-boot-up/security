# Input Validation Done Right, Explained

## The idea in one sentence

Input validation decides at the server's trust boundary which values the code accepts, by parsing each value into a type with one allowed shape, after putting text into one canonical form and
after checking its size, while output encoding, query parameters and access checks stay responsible for their own jobs.

## The 5 acts

### 1. Where validation belongs

The input is a checkout request sent without the page: sku=SHO-1042 quantity=-3. The page would have refused it, but a shop that trusts the page takes the catalogue price 89.90 and charges 89.90 x -3 = -269.70: a refund.
The second request keeps quantity 2 and adds a hidden field price=0.01, so the trusting shop charges 0.01 x 2 = 0.02 instead of 179.80.
After the fix, the checkout validates on the server: the quantity is refused (quantity: digits only), the price field is refused (price: not a field of this form), and an honest order costs 89.90 x 2 = 179.80 from the catalogue.
How the server reads a quantity: the length is checked first (at most 3 characters), then the characters (digits only, so '-' is refused), then the range 1 to 99; "2" becomes Quantity[value=2].

### 2. Say what is allowed, and parse into types

The input is one raw string per field; the result is either a typed value or the rule it broke. A quantity is digits only from 1 to 99: "7" becomes Quantity[value=7]; "0", "1e2" and " 5" are refused.
A product code (SKU) is 3 capital letters, a dash and 4 digits; "sho-1042" is refused, and "SHO-1042;" is longer than 8 characters. An e-mail address keeps its local part and lower-cases its domain: "Ben@Shop.EXAMPLE" becomes Ben@shop.example; ben@@shop.example and ben@localhost are refused.
A country is a two-letter ISO code the JDK knows, so ZZ and lower-case de are refused. An amount is exact, with at most two decimals, so 19.505, -5 and NaN are refused.
A form with four bad fields (country=Mars email=x quantity=lots sku=shoes) gets 4 problems reported at once, each naming the field and the rule, never the raw value.

### 3. Canonicalise before validating

The input is three spellings of one name: plain ana (U+0061 U+006E U+0061), full-width (U+FF41 U+FF4E U+FF41) and capitals ANA. Before the fix, a registry that compares raw text gives each one a new account.
How the canonical form is made: NFKC folds the full-width letters into ana, strip removes spaces at the ends, and lower-case (with Locale.ROOT) gives ana. After the fix, the registry compares canonical names: the plain spelling gets the account and the other two are refused as already taken.
A zero-width space (U+200B) or a line break (U+000A) inside a name is refused (no control or invisible characters), and a Cyrillic а (U+0430) in place of a is refused because login names are kept to plain letters.
José typed with one letter é (U+00E9) and with e plus a combining accent (U+0065 U+0301) is different raw text and equal after NFC. A display name may use any script, but "Shоp Suppоrt" with Cyrillic o has the skeleton "shop support" and is refused as reserved for the shop's staff; "José Martín" is accepted.

### 4. Limits are validation too

The input is three oversized values: a request body of 1000000 bytes, a cart of 10000 lines and an e-mail address of 10003 characters. The result: the body is refused after reading 16385 bytes, one more than the limit of 16384; the cart is refused (more than 50 lines) and the address (longer than 254 characters), before any other work.
How (a+)+$ reads aaa!: the inner and outer repetitions can split the a's as (aaa), (aa)(a), (a)(aa) or (a)(a)(a); each split fails at the !, so the engine tries all 4 = 2^(3-1), in 11 steps.
Before the fix, (a+)+$ on 10, 15 and 20 a's and a ! takes 2036, 65519 and 2097130 steps: five more a's cost about 2^5 = 32 times the work (ReDoS). After the fix, a+$ checked in one pass takes 21 steps for 20 a's and a !, and a length limit runs before any pattern.

### 5. The bill: what validation does not do

The input is two display names, <b>great</b> and O'Brien. The result: both are accepted, because both are allowed names, yet the first is still markup, so it is encoded on output, and the second has a quote, so the database layer binds it as a parameter.
A bad e-mail address with a line break and markup gets the message "email: not a valid address", which does not repeat the input (repeats it? false).
A wholesale order of 150 is refused (from 1 to 99): a limit stricter than the business turns a customer away, so each limit is a written business decision. Validation narrows what reaches the code; encoding, parameters and access checks still do their jobs.

## The verdict

Validate on the server, at every trust boundary. Parse into types with allow-lists, and report every problem at once. Normalise text before comparing it, and refuse invisible characters in names.
Limit every size before doing work, and keep patterns free of nested repetition. Never repeat raw input in an error, and write each rule down as a business decision.

## How to recognise this in code you did not write

- `@Valid`, `@Size` and `@Pattern` in Spring and Jakarta applications.
- Value objects such as `Money`, `EmailAddress` or `Sku` in well-kept domain models.
- `Normalizer.normalize` and confusable checks in account sign-up code.
- Request size limits in web servers and gateways.

## Where you have already met this

- Forms with `required`, `min`, `max` and `pattern` attributes, and the same requests sent with curl.
- Jakarta Validation annotations such as `@Size`, `@Pattern` and `@Email` in Java applications.
- Look-alike domain names and account names used in phishing.
- Outages caused by one regular expression on one long input.
