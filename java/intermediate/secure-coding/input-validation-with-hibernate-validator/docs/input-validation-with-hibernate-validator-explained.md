# Input Validation with Hibernate Validator, Explained

## The idea in one sentence

A rule that is written once, on the type, and checked in order from the cheapest to the dearest, is a rule that gets used.

## The 5 acts

### 1. Where validation belongs

The input is a checkout request sent without the page: the cart is SHO-1042 x -3, and the form's fields are email, country, postalCode, login and lines, with no price field, because the catalogue has the price.
Before the fix, a shop that trusts the page totals 89.90 x -3 = -269.70: a refund. After the fix, the service's parameter is @NotNull @Valid Checkout, so the same request is refused at place.checkout.lines[0].quantity with "must be greater than or equal to 1", a missing form is refused with "must not be null", and an honest order is accepted and totalled 89.90 x 2 = 179.80.
How the path is built: place is the method, checkout its parameter, lines a property, and quantity a property of element 0 of the list; the message comes from @Min(1) on Line.quantity.

### 2. Say what is allowed, in annotations

The rules are written once, on the records, and the demo reads them back from the annotations: quantity @Min(1) @Max(99); sku @NotNull @Pattern([A-Z]{3}-\d{4}) @InStock; email @NotBlank @Size(max=254) @Email; country @NotNull @Country.
The input is one value per field; the result is accepted, or refused with the constraint's message. 7 is accepted; 0 and 100 are refused. "sho-1042" and "SHO-1042;" do not match the pattern. "ben@@shop.example" is refused, but the built-in @Email accepts "ben@localhost": wider than the shop needs, so add a @Pattern for what you require. "ZZ" and "de" are not country codes.
A form with several bad fields (ben@@shop.example, ZZ, Ben K, sho-1042 x 0) gets 5 violations at once, each with its path.

### 3. Your own constraints, across fields, and in an order

The input is five logins: one with a zero-width space (U+200B), one with a line break (U+000A), BenK in capitals, one with a Cyrillic е (U+0435) in place of e, and é typed as two characters (e and U+0301). The custom @NoInvisible rule refuses 3 of 5 (the hidden characters and the text not in normal form C); all the login's rules, with @Pattern, refuse 5 of 5.
A rule across two fields, @PostalCodeMatchesCountry, refuses DE with 1011 (DE codes match \d{5}) and reports it on postalCode, where the person can fix it; IN with 110001 is accepted.
How the groups order the checks: 1 Limits (sizes), 2 Default (shapes), 3 Expensive (@InStock, a catalogue lookup), each only if all before passed. So sho-1042 is refused by its pattern with 0 lookups, and SHO-9999, the right shape but not sold, costs 1 lookup.

### 4. Limits are validation too

The input is two oversized values: a cart of 10000 lines and an e-mail address of 10013 characters. The result: both stop in the Limits group, the cart with "size must be between 0 and 50" and 0 catalogue lookups, the address with "size must be between 0 and 254".
Before the fix, the coupon pattern ((a+)+)+b on 8, 12 and 16 a's and a ! takes 2264, 36808 and 589752 steps (every character the engine reads is a step): x 16.3 and x 16.0, so four more a's cost about 2^4 = 16 times the work (ReDoS).
After the fix, @Size(max = 20) in the Limits group refuses 5000 a's with 0 steps: the pattern never runs. Why two levels of nesting: the JDK remembers its way through the simpler (a+)+$, which takes 109, 349 and 1213 steps for 8, 16 and 32 a's, 3 to 4 times the work for twice the a's.

### 5. The bill: what validation does not do

The input is a review title <b>great</b> (12 characters). The result is the message "title: '<b>great</b>' is not 3 to 10 characters": it repeats the visitor's text, markup and all, so encode it or leave it out.
The input is a coupon put into a constraint's own message template: ${1+1} and ${'a'.concat('b')}. Before the fix, on a validator set to allow method calls in templates as old versions did, the messages are "the coupon 2 does not exist" and "the coupon ab does not exist": the visitor's text was run as code (expression injection).
After the fix, Hibernate Validator 9's default keeps "the coupon ${1+1} does not exist", and passing the text as a message parameter keeps it as text even on the risky validator. Valid text still needs encoding on output and query parameters, and each rule is a business decision, written down.

## The verdict

Put constraints on the types that cross the boundary, validate at the service as well as the page, check sizes before shapes before lookups, write a pattern for what you require instead of trusting a wide built-in, keep messages free of raw input and templates free of visitor text, and remember that validation is not encoding.

## How to recognise this in code you did not write

- `@Valid` and `@NotNull` on a parameter.
- A `@GroupSequence`.
- A custom `@Constraint` annotation.
- A 400 response that lists field errors.

## Where you have already met this

- `@Valid` on a controller parameter.
- `@NotNull` and `@Size` on an entity.
- A 400 response with a list of field errors.
- `javax.validation` imports from before the move to `jakarta`.
