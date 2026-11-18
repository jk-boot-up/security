# Session Management with Spring Session and Redis, Explained

## The idea in one sentence

Spring Session puts the session where every server can reach it, and the rules about its id, its age and its end are still the bank's to set, test and protect.

## The 5 acts

### 1. The session lives in Redis

A redis-cli PING with no password gets NOAUTH: refused. Ana signs in on a real Spring Boot server: status 200, and the cookie __Host-session with Path=/, Secure, HttpOnly and SameSite=Strict, with no problems. The account page answers 200.
Redis now holds four keys: the session record spring:session:sessions:<id>, its expiry marker, a time bucket of expirations, and an index entry for ana, which lets the bank find all of her sessions. The record's fields are creationTime, lastAccessedTime, maxInactiveInterval and the security context.
The cookie is the Base64 of the id. The example id 3f2a7c1e-9b4d-4e8a-a6f0-5c1d2e3b4a90 has 36 characters; its first 3, 3f2, are the bytes 33 66 32, whose 24 bits cut into 6-bit groups give M2Yy; all 36 give 48 characters. Ana's real cookie is the Base64 of her real Redis id.
So a stolen copy of Redis's session keys, turned into cookies, opens the sessions: Redis holds live ids. The plain project kept only a hash; here Redis needs a password, the loopback address only, and short timeouts.

### 2. A new id at every sign-in

Before Ana signs in, a stranger opens the bank's /welcome page, gets a session cookie, called planted here, and puts it into Ana's browser.
After sign-in, a bank configured with session fixation protection off (fixation none) keeps the id: Ana's cookie is still the planted one, and the stranger's copy reads her account. That is session fixation.
After sign-in with Spring Security's default, changeSessionId, Ana gets a new cookie: the stranger's planted one is refused with 401, Ana's new one is allowed, and the planted id is no longer a live record in Redis.

### 3. How long a session lives

The settings: spring.session.timeout is 2 seconds, short so the demo can wait for it, and the absolute timeout is 8 hours, enforced by a filter of our own.
Redis gives the record a time to live of the idle timeout plus five minutes, at most 2 + 300 = 302 seconds.
Idle: 1 second after Ana's last request she is allowed; 2.5 seconds after, she is refused with 401. The dead record is still in Redis, but no live record is left.
Absolute: Ana stays busy and the bank's clock is moved forward. At 7:59 after sign-in she is allowed; at 8:01 she is refused, because the session's creation time plus 8:00 is before now, and the filter ends it.

### 4. Signing out, everywhere, and the limit

The sign-out response has status 200, tells the browser to forget the cookie with Max-Age=0, and the record is no longer live in Redis. A copy of the cookie made before sign-out, sent again, gets 401: the server ended the session.
Ana signs in on her phone, laptop and tablet: 3 live records in Redis, and 3 in her index, the principal name index that Spring Session keeps. Sign out everywhere from the laptop ends 3 sessions; the phone and the tablet are refused.
A password change on the laptop ends the 2 other sessions and gives the laptop a new id: the new id is allowed, the old one and the phone are refused.
With a limit of 3 sessions, a fourth sign-in on her watch succeeds and ends the oldest, the phone's; the laptop and the watch are allowed.

### 5. The bill

Ana signs in on server A and her next request lands on server B. With a separate store for each server, B refuses: 401. With both using the same Redis, B allows her, and so does A again.
While Redis stops answering, a request with a valid cookie and a sign-in are both refused with 500: the bank fails closed. When Redis answers again, the same cookie is allowed.
Twenty requests from strangers with no cookie: with Spring Security's request cache on, the default, they create 20 session records in Redis, so a stranger can fill it; with the cache off, 0.
The price: a customer who stops to read a statement signs in again, and every request now needs Redis to answer.

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
