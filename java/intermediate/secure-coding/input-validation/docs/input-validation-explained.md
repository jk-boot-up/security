# Input Validation Done Right, Explained

## The idea in one sentence

Input validation decides at the server's trust boundary which values the code accepts, by parsing each value into a type with one allowed shape, after putting text into one canonical form and
after checking its size, while output encoding, query parameters and access checks stay responsible for their own jobs.

## The 5 acts

### 1. Where validation belongs

The checkout page refuses a quantity of -3 before sending. The same request, sent without the page to a shop that trusts the page, gives a total of -269.70: a negative total is a refund.
A hidden price field changed to 0.01 gives a total of 0.02 instead of 179.80: the customer set the price. The checkout that validates on the server refuses the quantity (digits only)
and the price field (not a field of this form), and an honest order is accepted with a total of 179.80 from the catalogue.

### 2. Say what is allowed, and parse into types

Each field has one allowed shape, and a value that passes becomes a type that cannot hold anything else. A quantity is 1 to 99 in digits, so 0, 1e2 and " 5" are refused. A product code is three
capital letters, a dash and four digits. An e-mail address keeps its local part and lower-cases its domain; ben@@shop.example and ben@localhost are refused. A country is an ISO code, so ZZ
and lower-case de are refused. Money is an exact decimal with at most two places, so 19.505, -5 and NaN are refused. A form with four bad fields reports all four at once.

### 3. Canonicalise before validating

With ana registered, a registry that compares raw text also accepts the full-width ａｎａ and ANA in capitals: two accounts for one name. The canonical registry normalises with NFKC, trims and
lower-cases without the machine's locale, so the full-width spelling is ana, and is refused as already taken. A zero-width space or a line break inside a name is refused, and so is a Cyrillic
letter in place of a Latin one, because login names are kept to plain letters. José typed with one accented letter and with e plus a combining accent are different raw text and equal after NFC.
A display name may use any script, but "Shоp Suppоrt" written with Cyrillic o is refused as a look-alike of the staff name.

### 4. Limits are validation too

A request body of 1000000 bytes is refused after reading only 16385 of them, one more than the limit. A cart of 10000 lines and an e-mail address of 10000 characters are refused before any other
work. The pattern (a+)+$ on 10 a's and a ! takes 2036 steps to fail, on 15 a's 65519 steps and on 20 a's 2097130 steps: each extra character roughly doubles the work, so a few dozen characters can
stop a server (ReDoS). The same rule written without nesting, checked in one pass, takes 21 steps. So: no nested repetition in patterns that read input, and a length limit before any pattern runs.

### 5. The bill: what validation does not do

A review "<b>great</b>" is valid text and still markup, so it is encoded on output. A surname O'Brien is valid and contains a quote, so the database layer binds values as parameters and never
pastes them into query text. A refused e-mail address with a line break and markup gives the message "email: not a valid address", without repeating the raw input. A wholesale order of 150 is
refused by a limit of 99: a limit stricter than the business turns customers away, so each rule is a written business decision. Validation narrows what reaches the code; encoding,
parameters and access checks still do their own jobs.

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
