# SQL Injection with Hibernate and PostgreSQL, Explained

## The idea in one sentence

Hibernate runs the text it is given: parameters keep the customer's words as data, an allow-list covers the parts a parameter cannot, and a database user with few rights is the second wall.

## The 4 acts

### 1. A search that works

The first version of the search builds a JPQL string from the customer's text. For the word Kettle it returns the Blue Kettle and the Red Kettle, the unlisted Prototype Toaster stays hidden, and nothing looks wrong. This is why the hole survives testing.

### 2. Hibernate does not stop a quote

A quote in the search box ends the string literal and the rest is JPQL: the unlisted Prototype Toaster appears. Because JPQL can name any entity, the same hole asks the account table whether any hash starts with `pbkdf2$ana` (every product comes back, so yes) and `pbkdf2$zed`
(nothing comes back, so no): data is read one yes or no at a time from a page that only searches products. In the native query, `union select` returns the emails and password hashes of both accounts.

### 3. Parameters keep text as text

The fixed searches have constant query text and send the customer's words as a named parameter, so the payloads are only odd product names that match nothing. The Criteria API binds the values it is given in the same way. The sort key is the one part a parameter cannot carry, so it is
chosen from an enum, and a trick such as `price; drop table products` just gets the default order.

### 4. Few rights limit the damage

The careless searches run again, this time connected as `shop_search`, who has been granted `select` on the product table and nothing else. The union and the JPQL question about accounts are both refused by PostgreSQL itself with permission denied for table users. The mistake is still
there, but it can read less. This is a second wall, not a replacement for parameters.

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
