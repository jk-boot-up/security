# Privacy Engineering, Explained

## The idea in one sentence

Removing the name does not make data anonymous, and deleting a row does not erase a person.

## The 5 acts

### 1. What is collected, and why

The customers table holds name, email, phone, postcode, birth date, gender, national id and two marketing columns. Five purposes (payments, statements, marketing, fraud analytics and identity checks required by law) need 7 of the 9. Phone and gender are collected, stored and protected for no purpose, so the bank can stop collecting them.

### 2. Who can be picked out

Postcode, birth date and gender are not names, and together they pick out nearly everyone. An outside list with names and those three values names 1,398 customers from the published rows. With the postcode cut to 3 digits and the birth date to its decade the smallest group has 4 customers and the outside list names none.

### 3. Pseudonyms for analytics

The analytics extract has one row for each transaction with an HMAC-SHA256 pseudonym of the customer. 22,346 rows carry 2,000 pseudonyms. The same customer gets the same pseudonym under the same key, which keeps joins working, and another one under a new key. The rows still carry the postcode, birth date and gender.

### 4. Retention

Transactions older than 7 years are deleted unless the customer is under a legal hold: 9,657 expired, 14 kept. 494 marketing consents older than 2 years are withdrawn and 505 notes older than a year are deleted. The analytics table is a copy that the job does not know about: 8,054 of its 22,346 rows are older than the limit.

### 5. A customer's request

The data map is found from the foreign keys of the schema, not from a list: customers, accounts, transactions and support notes. For customer 17 the export counts 1 customer, 1 account, 4 transactions and 11 analytics rows. After the erasure, which keeps the 4 transactions by law, a scan of every text column finds 6 rows that mention the customer's name or email, in notes of other customers and in payments to them. Scrubbing leaves 0.

## The verdict

Collect only what a purpose needs, measure how many people are alone in the data you share and make it coarser, treat pseudonyms as personal data, let the retention job reach every copy, build the data map from the schema, and search free text after an erasure.

## How to recognise this in code you did not write

- An analytics extract with the name column dropped.
- A column nobody can explain.
- A retention rule in a policy and no job.
- An erasure that deletes one row.

## Where you have already met this

- A column collected because it might be useful.
- A data extract with the name removed.
- A retention policy in a document and no job.
- An erasure request answered by deleting one row.
