# Authentication vs Authorization, Explained

## The idea in one sentence

Authentication finds out who is asking, usually by checking a password and handing out a session token; authorization decides what that person may do, using rules about roles and ownership; and a gate must do both, in that order, on every request.

## The 7 acts

### 1. The first version, with no checks

The first version of the shop asks nobody anything. Two requests arrive with no login and no token: refund order O-1, Ana's laptop
of 1200 dollars, and view order O-1. The responses are 200 OK and 200 OK. There is no idea of a person anywhere in the shop, so there is nothing to protect.

### 2. Authentication: finding out who is asking

Authentication finds out who is asking. Ben logs in with the password guess, and is rejected. He logs in with ben-pass, and receives
a session token, tok-1, which records ben, his role CUSTOMER, and the tick it was issued, 0. From now on he shows the token instead of the
password. Three requests then view order O-2: with no token, 401 UNAUTHENTICATED; with a made-up token tok-99, which the shop never issued,
401 again; with tok-1, 200 OK.

### 3. How a password is checked, step by step

The shop never stores Ben's password. When he signed up, his password ben-pass and a salt, demo-salt:ben, were hashed 10000 times with PBKDF2 and
HMAC-SHA256, giving 32 bytes that start 0f455f22 5cf85332. That hash is all the shop keeps. At login the typed password is hashed the same way,
with the same salt. guess gives 6be4814d de2bcf7c, which is different, so it is rejected. ben-pass gives 0f455f22 5cf85332 again, the same, so it is accepted.
The salt makes the same password give a different hash for every account, and the 10000 rounds make every guess slow for an attacker.

### 4. Authenticated is not the same as authorized

The team believes the problem is solved, because every request now needs a login. But the gate asks only one question: is there a live session?
Ben is a customer, logged in with tok-1. He asks to refund order O-1, which is Ana's, and to view it. Both responses are 200 OK. Being logged in proved
who he was, and nothing decided what he was allowed to do.

### 5. Authorization: what may this person do?

Authorization is a set of rules about roles and ownership, applied after authentication. The gate now asks two questions, in order. First, who is this?
No live token gets 401 UNAUTHENTICATED. Then, may this person do this to this order? If not, 403 FORBIDDEN. Ben refunding O-1 is 403, and Ben viewing
Ana's O-1 is 403, but Ben viewing his own O-2 is 200. Sam, from support, may view and refund O-1: 200 and 200. Ana cancelling Ben's O-2 is 403.
Alex, an admin, cancelling it is 200.

### 6. How the gate decides, step by step

The rules are one small table, printed from PolicyGate.allowed: a CUSTOMER may view and cancel only their own orders and may not refund; SUPPORT may view
and refund any order but not cancel; an ADMIN may do anything. Take one request: token tok-1 asks to view O-1. Step 1, authentication: tok-1 is a session
for ben, a CUSTOMER, issued at tick 0; its age is 0 ticks, under the lifetime of 3, so it is live, and the person is ben. Step 2, authorization: the rule for
CUSTOMER and VIEW is own only, and the owner of O-1 is ana, not ben. So the response is 403 FORBIDDEN.

### 7. The bill: sessions expire, and roles go stale

The checks have costs. A session lasts 3 ticks. Sam logs in at tick 0 with tok-1; at tick 4 the age is 4, not under 3, so it has expired and his refund gets 401:
safer if a token is stolen, and a new login for him. Then a subtler cost. Before the demotion, Sam's session tok-1 holds the role SUPPORT. After Alex makes him a
CUSTOMER, a gate that uses the role copied at login still lets him refund: 200 OK, a stale role, with 0 directory lookups. A gate that looks the role up gives 403
FORBIDDEN, at the price of 1 lookup on every request.

## The verdict

Do both, in order, in one place. Turn away requests with no known person with
401. Then apply the rules to the known person, and answer 403 when they may not.
Keep the rules in one policy and not scattered across endpoints, put the gate in
front of every endpoint, and choose on purpose whether a role is copied into the
session or looked up each time.

## How to recognise this in code you did not write

- A 401 page asking you to sign in, and a 403 page saying you may not.
- A session cookie that expires, followed by a login page.
- Role names such as viewer, editor and admin in a settings screen.
- A "remember me" box, which trades a longer session for convenience.

## Where you have already met this

- The 401 and 403 pages a browser shows, and the difference between them.
- A login form, a "remember me" box, and a "your session has expired" message.
- A "you do not have permission to view this page" message after you have signed in.
- Role names such as viewer, editor and admin in a cloud or office tool.
