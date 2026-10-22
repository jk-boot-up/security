# Session Guide — SQL Injection with Hibernate and PostgreSQL

## Learning Objectives

By the end of the session you can:

- explain why JPQL and native queries built from strings are injectable
- show how JPQL can read entities that the query does not name
- fix a query with named parameters, Criteria or an allow-list
- explain what a least-privilege database user adds

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A search that works | 8 min |
| 0:18 | Act 2: Hibernate does not stop a quote | 8 min |
| 0:26 | Act 3: Parameters keep text as text | 8 min |
| 0:34 | Act 4: Few rights limit the damage | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act two and ask what the page was supposed to be able to see. Ask why the account question needs no table name in the query. In act four, ask who should own the grant statement.

## Exercises

1. Add a Spring Data style repository method and check that it binds the parameter.
2. Make `shop_search` read a view instead of the products table and decide what the view should hide.
3. Add a test that fails if any class in the project calls `createQuery` or `createNativeQuery` with a concatenated string.
4. Write a native query with an `in` list as a parameter.
