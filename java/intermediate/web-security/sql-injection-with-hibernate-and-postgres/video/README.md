# SQL Injection with Hibernate and PostgreSQL — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `sql-injection-with-hibernate-and-postgres-explained.mp4` | the video, 1920×1080 |
| `sql-injection-with-hibernate-and-postgres-explained.m4a` | audio only |
| `sql-injection-with-hibernate-and-postgres-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> SQL injection in Hibernate 7 against a real PostgreSQL 18, in Java. Hibernate runs the text it is given: a JPQL or native query built by pasting a customer's words into it is injectable just like plain SQL. A quote reveals a product that is not for sale, the same hole asks yes or no questions about the account table because JPQL can name any entity, and a native UNION returns every e-mail address and password hash. Explained with an online store's product search, like a waiter who lets the customer dictate the kitchen's order slip word for word. We fix it three ways with bound parameters (named parameters in JPQL, in native SQL, and the Criteria API), choose the sort key from a fixed list, and connect as a database user who can only read products, so PostgreSQL itself refuses the attack. We finish with the cost: Docker for the demo, more than one database user, and a review of every string that reaches createQuery.
