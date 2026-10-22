# Session Management with Spring Session and Redis, Explained

## The idea in one sentence

Spring Session puts the session where every server can reach it, and the rules about its id, its age and its end are still the bank's to set, test and protect.

## The 5 acts

### 1. The session lives in Redis

A client with no password is refused by Redis. Ana signs in and the cookie `__Host-session` has Path=/, Secure, HttpOnly and SameSite=Strict, with 0 problems. Redis holds 1 session record and an index entry for ana. The cookie is the id in the Redis key, encoded. A stolen copy of Redis's 1 session key, turned into a cookie, opens 1 session: the id is stored as itself. The plain project kept only a hash of each id; here the protection is Redis's password, its loopback address and short timeouts.

### 2. A new id at every sign-in

A stranger gets an id from the bank's welcome page and puts it into ana's browser. With a bank that keeps the id at sign-in, ana's id after signing in is the planted one, and the stranger's copy reads her account: session fixation. With Spring Security's default, changeSessionId, the ids differ, the stranger's copy is refused with 401, ana's new id is allowed, and the planted id is no longer a live record in Redis.

### 3. How long a session lives

With an idle timeout of 2 seconds for the demo, Redis keeps the record for the idle timeout plus five minutes. Ana idle for 1 second is allowed. Idle for 2.5 seconds she is refused, and the dead record stays in Redis for five more minutes while the bank still refuses its id. An absolute timeout of 8 hours: 7 hours 59 minutes after sign-in she is allowed; two minutes later she is refused, though she was busy. Spring Session has no absolute timeout, so this is a 20-line filter of our own.

### 4. Signing out, everywhere, and the limit

Sign-out returns 200, tells the browser to forget the cookie, and the record is no longer live in Redis. A copy of the cookie made before sign-out is refused with 401: the server ended the session, not only the browser. Ana is signed in on her phone, laptop and tablet: 3 live records and 3 in her index. Sign out everywhere ends 3 sessions. After a password change on the laptop, 2 other sessions are ended, the laptop goes on with a new id, and the old laptop id and the phone are refused. With a limit of 3 sessions, a fourth sign-in ends the oldest, the phone's.

### 5. The bill

Two servers with separate session stores: ana signs in on A and her next request lands on B and is refused. Both servers using the same Redis allow it, and back on A too. When Redis stops answering, a request with a valid cookie is refused with 500 and so is a sign-in: the bank fails closed. When Redis answers again the same cookie is allowed. With Spring Security's default request cache, 20 requests from strangers with no cookie leave 20 session records in Redis; with it off, 0.

## The verdict

Protect Redis like the cookie jar, keep Spring Security's new id at sign-in, add an absolute timeout, end sessions on the server, turn the request cache off for an API, and make the bank refuse when Redis does not answer.

## How to recognise this in code you did not write

- `spring:session:sessions:` keys in a Redis browser.
- A customer signed out on every device after a password change.
- A Redis with no password on a shared network.
- A record for every request from a stranger.

## Where you have already met this

- `@EnableRedisIndexedHttpSession` or `spring-session-data-redis` in a pom.
- Keys such as `spring:session:sessions:<uuid>` in a Redis browser.
- A customer signed out on every device after a password change.
- Load balancers with sticky sessions that Redis makes unnecessary.
