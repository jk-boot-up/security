# SQL Injection and Its Defences, Explained

## The idea in one sentence

A statement that includes the customer's text can be rewritten by that text, so statements are fixed, values are parameters, names come from a list, and the database user can do very little.

## The 4 acts

### 1. A search that works

The first version of the search pastes the customer's words into the statement. A search for kettle finds the Blue Kettle and the Red Kettle, sorted by price.
Nothing looks wrong, which is why the flaw survives testing.

### 2. A quote turns text into code

A search for `zzz' OR listed = 0 --` closes the string, adds a condition, and comments out the rest, so the unlisted Prototype Toaster appears. A search for
`zzz' UNION SELECT email, password_hash FROM users --` appends the users table to the answer: the page now lists ana@shop.example and ben@shop.example.
A sort value with `; DROP TABLE products` is refused, but only because this engine, like most drivers, accepts one statement per call.

### 3. Parameters keep text as text

The fixed version has a constant statement with a `?` for the words, and the sort column comes from a list of two names. The honest search is unchanged.
The same quote and the same union are now just words that no product contains, so the answer is empty. The bad sort value falls back to the default.

### 4. Few rights limit the damage

Defence in depth: the search page connects as a database user that may read only the products table. If somebody later writes a vulnerable query,
the union with the users table is refused with permission denied. This does not replace parameters; it limits what one mistake can reach.

## The verdict

Use parameters for every value, always, with the driver or the framework's own binding. Choose names (columns, tables, sort directions) from an allow-list.
Run each application with its own database user and the fewest rights. Keep error details out of the response.

## How to recognise this in code you did not write

- String concatenation or `String.format` inside a `Statement.executeQuery` call.
- `PreparedStatement` with `?` placeholders.
- `ORDER BY` built from a request parameter.
- A finding titled "SQL Injection" with CWE-89.

## Where you have already met this

- `PreparedStatement` and `?` placeholders in JDBC, and named parameters in `JdbcTemplate`.
- `' OR '1'='1` in every list of attack strings.
- Breach reports that begin with a search box or a login form.
- A finding titled "SQL Injection" with CWE-89.
