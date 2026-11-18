# Session Management: Ids, Rotation, Timeouts and Sign-Out, Explained

## The idea in one sentence

A session id is as good as a password while it lives, so it must be random and stored only as a hash, replaced at every sign-in and step-up, ended by idle and
absolute timeouts, and ended on the server at sign-out, everywhere and after a password change, carried in a Secure, HttpOnly, SameSite cookie.

## The 5 acts

### 1. A session id: random, and never stored as itself

A session id starts as 16 random bytes, 128 bits, from SecureRandom. The demo shows the steps on 16 fixed example bytes: base64url reads the bits as one row and cuts it into groups of 6, and each group, a number from 0 to 63, picks one of 64 characters, so the first 3 bytes, 4f 1a 9c, become Txqc and all 16 become the 22-character id TxqcJ9PosFpsH3LppNA7jg.
The store never keeps the id. It keeps the id's SHA-256, 64 hex characters, as the key of the session. Real ids are random: a thousand are all different, and none contains the customer's name, a time or a counter.
A stolen copy of the store's three keys, sent back as cookies, opens no session, because the bank hashes what arrives and the hash of a hash matches nothing. Ana's real id reads her account; the same id with its last character changed is refused. Hashes are compared in constant time, and ids are never printed or logged.

### 2. A new id at every sign-in and step-up

Before Ana signs in, a stranger asks the bank for a visitor id, called planted here, and gets it into Ana's browser, for example through a link or a shared computer.
After sign-in, a bank that keeps the id leaves Ana with the planted id: the stranger sends it and reads her account. That is session fixation.
After sign-in, a bank that issues a new id gives Ana a new one: the stranger's planted id is refused, and Ana's new id is allowed. Before a payment Ana gives her password again, a step-up, and the id changes once more: the id from before is refused, and the newer one pays.

### 3. How long a session lives

The bank's settings: an idle timeout of 15 minutes without a request, an absolute timeout of 8 hours after sign-in whether busy or not, and the password again for a payment 5 minutes after it was given.
Idle: after 14 minutes Ana is still allowed; after 16 more she is refused, and that id stays dead.
Absolute: with a request every 10 minutes, 8 hours is 480 minutes and 480 / 10 = 48, so requests 1 to 47, from 0:10 to 7:50, are allowed and request 48, at 8:00, is refused.
Payments: 3 minutes after the password a payment is allowed; at 6 minutes the bank asks for the password again, while reading the account still works.

### 4. Signing out, everywhere, and the cookie

On a real HTTP server on this machine's loopback address, the sign-in response has status 200 and sets the cookie __Host-session with Path=/, Secure, HttpOnly and SameSite=Strict: no problems. The account page with that cookie answers 200.
The sign-out response has status 200 and a Set-Cookie with Max-Age=0, which tells the browser to forget the cookie. A copy of the cookie made before sign-out, sent again, gets 401, because the server ended the session. A bank whose sign-out only clears the browser's cookie leaves the copy reading the account.
A weak cookie, SESSIONID with no attributes, has four problems: no Secure, no HttpOnly, no SameSite and no __Host- prefix.
Ana signed in on her phone, laptop and tablet: sign out everywhere ends 3 sessions. A password change on the laptop ends the 2 others and gives the laptop a new id. With a limit of 3 sessions, a fourth sign-in ends the oldest, the phone's.

### 5. The bill

Ana signs in on server A and her next request lands on server B. If each server keeps its own session memory, B refuses her: unknown session. With one store shared by both servers, B allows her; the -with-spring-session-and-redis version keeps that store in Redis.
A customer who stops for 20 minutes to read a statement must sign in again: the price of a short idle timeout.
A stolen id still works, but for at most 15 idle minutes or 8 hours in all, or until it is rotated or ended. And a wrong password never starts a session.

## The verdict

Use the framework's session management where there is one, and check that it rotates the id at sign-in, expires sessions on the server, and sets the cookie attributes.
Choose timeouts by what a session can do, ask for the password again before payments, and give customers a way to sign out everywhere.

## How to recognise this in code you did not write

- `changeSessionId()` or "session fixation protection" settings in web frameworks.
- `server.servlet.session.timeout` and similar settings.
- Cookies with the `__Host-` prefix.
- Pen-test findings titled "session not invalidated on logout", CWE-613.

## Where you have already met this

- Cookies named `JSESSIONID`, `__Host-session` or similar in a browser's developer tools.
- "Your session has expired, please sign in again."
- "Sign out of all other devices" in account settings.
- Banking apps that ask for the password or a fingerprint again before a payment.
