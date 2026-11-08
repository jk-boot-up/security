# SQL Injection and Its Defences, Explained

## The idea in one sentence

A statement that includes the customer's text can be rewritten by that text, so statements are fixed, values are parameters, names come from a list, and the database user can do very little.

## The 4 acts

### 1. A search that works

The products table holds four rows: the Blue Kettle at 2500 cents, the Red Kettle at 2900, the Copper Pan at 4200, all listed, and the Prototype Toaster at 9900, which is not for sale (listed = 0).
The customer types kettle in the search box and chooses price in the sort box. The first version pastes both into the statement: SELECT name, price FROM products WHERE name LIKE '%kettle%' AND listed = 1 ORDER BY price.
The result is the Blue Kettle and the Red Kettle. Nothing looks wrong, which is why the flaw survives testing.

### 2. A quote turns text into code

The input zzz' OR listed = 0 -- is pasted in. Its quote closes the string early, OR listed = 0 adds a condition of the attacker's choosing, and -- turns the rest of the statement, %' AND listed = 1 ORDER BY name, into a comment. The result is the Prototype Toaster, which is not for sale.
The input zzz' UNION SELECT email, password_hash FROM users -- closes the string the same way and appends a second query on the users table. The page now lists ana@shop.example and ben@shop.example.
The sort box value price; DROP TABLE products ends the statement and starts another. It is refused with unexpected ';' (one statement only), but only by luck: this engine, like most drivers, runs one statement per call.

### 3. Parameters keep text as text

The fixed statement is constant text: SELECT name, price FROM products WHERE name LIKE ? AND listed = 1 ORDER BY a column taken from a list of two, name or price. The customer's words travel separately, as the value of the ?.
kettle still finds the Blue Kettle and the Red Kettle. zzz' OR listed = 0 -- and the UNION input are now just words that no product name contains: no results.
A column name cannot be a ?, so the sort box is checked against the list. price; DROP TABLE products is not on it, so the statement uses ORDER BY name, the default, and the search still answers with the two kettles.

### 4. Few rights limit the damage

Two database users: admin may read products, users and orders; search may read only products, which is all the search page needs.
The union attack, still pasted in: as admin it lists ana@shop.example and ben@shop.example; as search it is refused with permission denied for table users.
The quote attack, as search, still shows the Prototype Toaster, because it only reads the products table. Few rights limit the damage of a mistake; only parameters fix the flaw.

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
