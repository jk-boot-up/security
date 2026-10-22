# Race Conditions with PostgreSQL and Hibernate — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `race-conditions-and-business-logic-with-postgres-explained.mp4` | the video, 1920×1080 |
| `race-conditions-and-business-logic-with-postgres-explained.m4a` | audio only |
| `race-conditions-and-business-logic-with-postgres-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> A real PostgreSQL 18 in Docker: two withdrawals that read before either writes and pay out 160 from 100, a check constraint and a conditional update, SELECT FOR UPDATE with another session waiting in pg_stat_activity, NOWAIT, lock_timeout and SKIP LOCKED, READ COMMITTED against REPEATABLE READ and a retry, write skew that only SERIALIZABLE stops, Hibernate 7 optimistic locking with a version column, a pessimistic lock, a one-time payment key, a deadlock that PostgreSQL cancels, and a lock in the memory of one server.
