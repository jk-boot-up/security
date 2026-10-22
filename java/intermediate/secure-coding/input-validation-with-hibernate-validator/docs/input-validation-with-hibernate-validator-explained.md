# Input Validation with Hibernate Validator, Explained

## The idea in one sentence

A rule that is written once, on the type, and checked in order from the cheapest to the dearest, is a rule that gets used.

## The 5 acts

### 1. Where validation belongs

A shop that trusts the page totals a quantity of -3 for the sku at 89.90 as -269.70: a refund. The service whose parameter is @NotNull @Valid Checkout refuses it with the path place.checkout.lines[0].quantity, and refuses a missing form. An honest order is accepted and totalled from the catalogue. The form has no price field, so there is nothing for a hidden field to set.

### 2. Say what is allowed, in annotations

Quantity 7 is accepted; 0 and 100 are refused with the messages must be greater than or equal to 1 and must be less than or equal to 99. The sku must match [A-Z]{3}-\d{4}. The e-mail uses @Email, which takes ben@localhost: a built-in check is wider than the rule, so add a @Pattern for what you require. A country code is checked by a custom constraint against the JDK's list. A form with several bad fields reports all of them at once, with their paths.

### 3. Your own constraints, across fields, and in an order

A custom constraint refuses a zero-width space, a line break and text that is not in normal form C, and accepts capitals and Cyrillic, which the login pattern refuses: no single rule catches everything. A class-level rule checks that the postal code fits the country and reports the violation on the postal code. A group sequence runs limits, then shapes, then the expensive checks: a sku in the wrong shape never reaches the catalogue (0 lookups); a sku in the right shape that nobody sells costs one lookup.

### 4. Limits are validation too

A cart of 10000 lines is refused by the size limit, and the catalogue was asked 0 times. An e-mail address of 10013 characters is refused before the e-mail pattern runs. The pattern ((a+)+)+b on 8, 12 and 16 a's followed by ! takes 2264, 36808 and 589752 steps: four more characters multiply the work by sixteen. The JDK remembers its way through the simpler (a+)+$ and needs only quadratic time; one more level of nesting is exponential again. With @Size(max = 20) in the first group, 5000 characters cost 0 steps.

### 5. The bill: what validation does not do

A review title <b>great</b> is refused for its length and the message repeats the visitor's text, markup and all. A surname O'Brien and a title <b>ok</b> are valid text, and encoding on output and parameters in the query are still the code's job. A coupon written into a constraint's own message template: Hibernate Validator 9 leaves ${1+1} as text; a validator set to allow method calls, as old versions did, evaluates it to 2 and ${'a'.concat('b')} to ab, which is the visitor's text run as code. Passed as a message parameter, the same text stays text even there.

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
