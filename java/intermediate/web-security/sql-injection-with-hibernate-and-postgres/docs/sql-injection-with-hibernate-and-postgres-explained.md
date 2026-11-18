# SQL Injection with Hibernate and PostgreSQL, Explained

## The idea in one sentence

Hibernate runs the text it is given: parameters keep the customer's words as data, an allow-list covers the parts a parameter cannot, and a database user with few rights is the second wall.

## The 4 acts

### 1. A search that works

The products table holds four rows: the Blue Kettle at 2500 cents, the Red Kettle at 2900, the Copper Pan at 4200, all listed, and the Prototype Toaster at 9900, which is not for sale (listed = false). The users table, mapped to the `Account` entity, holds ana@shop.example and ben@shop.example with their password hashes.
The customer types Kettle in the search box and the sort box holds p.price. The first version pastes both into a JPQL string: `select p.name from Product p where p.name like '%Kettle%' and p.listed = true order by p.price`.
Hibernate translates it into PostgreSQL's SQL, entity names into table names: `select p1_0.name from products p1_0 where p1_0.name like '%Kettle%' escape '' and p1_0.listed=true order by p1_0.price`. The word Kettle is copied into the SQL inside its quotes. The result is the Blue Kettle and the Red Kettle. Nothing looks wrong, which is why the flaw survives testing.

### 2. Hibernate does not stop a quote

The input `zzz' or p.listed = false or p.name like 'zzz` is pasted into the JPQL. Its first quote closes the literal early, `or p.listed = false` becomes a real condition, and its last quote opens a literal that the program's own `%'` closes. The result is the Prototype Toaster, which is not for sale.
Because JPQL can name any entity, the same hole can ask about `Account`: `or exists (from Account a where a.passwordHash like 'pbkdf2$ana%')`. Hibernate translates it into a subquery on the users table: `exists(select a1_0.id from users a1_0 where a1_0.password_hash like 'pbkdf2$ana%' escape '')`. With pbkdf2$ana every product comes back (4 products: yes, a hash starts that way); with pbkdf2$zed none do (no). One yes or no per search, and the hash leaks letter by letter.
In the native SQL query, `zzz' union select email || ' ' || password_hash from users --` appends a second query and comments out the rest: the page lists ana@shop.example pbkdf2$ana$9f2c and ben@shop.example pbkdf2$ben$41d0.

### 3. Parameters keep text as text

The fixed JPQL is constant text: `select p.name from Product p where p.name like :term and p.listed = true order by` a sort key from a list of two. Hibernate sends `select p1_0.name from products p1_0 where p1_0.name like ? escape '' and p1_0.listed=true order by p1_0.name`: the named parameter :term became a `?`, and the customer's words travel separately, as its value.
`%Kettle%` still finds the Blue Kettle and the Red Kettle. The quote input and the account question become values such as `%zzz' or p.listed = false or p.name like 'zzz%`: odd product names that match nothing, so no results.
The native query with `:term` and the Criteria API return no results for the union and the quote inputs too; the Criteria query's SQL also holds a `?` where the value goes. A sort key cannot be a `?`, so `price; drop table products` is checked against the list, is not on it, and gets `order by p.name`, the default: the two kettles again.

### 4. Few rights limit the damage

Two database users: `postgres` owns every table; `shop_search` was granted select on products and nothing else, as PostgreSQL's own grant table reports.
The careless searches run again. The union attack returns 2 e-mails and hashes as postgres, and is refused as shop_search with permission denied for table users. The JPQL question about accounts is refused the same way, because its subquery reads users.
The quote attack as shop_search still shows the Prototype Toaster, because it only reads products. Few rights limit the damage of a mistake; only parameters fix the flaw.

## The verdict

Use derived or annotated queries with bound parameters, the Criteria API where queries are built dynamically, an enum for sort keys, and a least-privilege database user for the application. Treat every string passed to createQuery or createNativeQuery as a code review finding.

## How to recognise this in code you did not write

- `createQuery("... '" + value + "'")` in a review.
- `@Query(nativeQuery = true, value = "... " + ...)` or `@Query` text built with `String.format`.
- `GRANT ... TO app_user` in a database script.
- `javax.persistence`/`jakarta.persistence` imports next to string concatenation.

## Where you have already met this

- `@Query("... where p.name like :term")` in Spring Data repositories.
- `entityManager.createNativeQuery("... " + input)` in code reviews.
- `GRANT SELECT ON ... TO ...` in a database setup script.
- HQL injection write-ups.
