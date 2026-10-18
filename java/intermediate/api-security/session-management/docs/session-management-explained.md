# Session Management: Ids, Rotation, Timeouts and Sign-Out, Explained

## The idea in one sentence

A session id is as good as a password while it lives, so it must be random and stored only as a hash, replaced at every sign-in and step-up, ended by idle and
absolute timeouts, and ended on the server at sign-out, everywhere and after a password change, carried in a Secure, HttpOnly, SameSite cookie.

## The 5 acts

### 1. A session id: random, and never stored as itself

A new session id is 128 bits from SecureRandom, written as 22 base64url characters, and a thousand new ids are all different. Ana's id does not contain her
name, a time or a counter: it is only randomness. The store keeps the SHA-256 of each id, so a stolen copy of its three keys, sent as cookies, opens no session.
Ana's real id reads her account; the same id with its last character changed is refused. The hash is compared in constant time, and ids are never printed or logged.

### 2. A new id at every sign-in and step-up

A stranger opens the bank's site, gets an id for a visitor who has not signed in, and gets it into Ana's browser. A bank that keeps the id at sign-in makes
Ana's signed-in id the planted one, and the stranger's copy reads her account: session fixation. A bank that issues a new id at sign-in refuses the stranger's
copy and lets Ana's new id in. Before a payment Ana gives her password again, and the id changes again: the id from before the step-up is refused, and the new one pays.

### 3. How long a session lives

The bank's settings are an idle timeout of 15 minutes, an absolute timeout of 8 hours, and the password again for a payment after 5 minutes. Ana idle for 14 minutes
is still signed in; idle for 16 more she is refused, and that id stays dead. A busy Ana with a request every 10 minutes gets 47 requests, and then at 8 hours
the session ends anyway. A payment 3 minutes after signing in is allowed; 6 minutes after, the bank asks for the password again, while reading the account still works.

### 4. Signing out, everywhere, and the cookie

On a real local server, signing in sets a __Host-session cookie with Path=/, Secure, HttpOnly and SameSite=Strict, with no problems found. The account page answers 200;
sign-out answers 200 and tells the browser to forget the cookie; and a copy of the cookie made before sign-out, sent again, gets 401, because the server ended the session.
A bank whose sign-out only clears the browser's cookie leaves the copy working. A cookie named SESSIONID with no attributes has four problems. Sign out everywhere ends
Ana's three sessions; a password change on the laptop ends the two others and gives the laptop a new id; and with a limit of three, a fourth sign-in ends the oldest.

### 5. The bill

Two servers, each with its own session memory: Ana signs in on A and her next request lands on B, which refuses it. With one store shared by both servers it is allowed;
the -with-spring-session-and-redis version keeps that store in Redis. A customer who stops for 20 minutes to read a statement must sign in again: the price of a short timeout.
A stolen id works only until it is rotated, times out or is ended, which is what these rules limit. And a session only starts after the right password.

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
