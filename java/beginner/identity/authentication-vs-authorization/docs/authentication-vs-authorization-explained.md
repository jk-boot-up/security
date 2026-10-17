# Authentication vs Authorization, Explained

## The idea in one sentence

Authentication finds out who is asking, usually by checking a password and handing out a session token; authorization decides what that person may do, using rules about roles and ownership; and a gate must do both, in that order, on every request.

## The 5 acts

### 1. The first version, with no checks

The first version of the shop asks nobody anything. A request with no token at all
asks to refund order O-1, and the answer is 200 OK. The same request can view the
order. There is no concept of a person, so there is nothing to protect.

### 2. Authentication: finding out who is asking

Authentication finds out who is asking. Ben tries to log in with a wrong password
and is rejected. He logs in with his password and receives a session token,
tok-1: from now on he shows the token instead of the password. A request with no
token gets 401 UNAUTHENTICATED. A request with a made-up token, tok-99, gets the
same, because the shop never issued it. A request with Ben's token gets 200 OK.
The password is stored only as a slow, salted hash, so even the shop cannot read
it back.

### 3. Authenticated is not the same as authorized

The team believes the problem is solved, because every request now needs a login.
But the gate only asks whether the person is logged in. Ben is a customer. He
refunds order O-1, which is Ana's, and the answer is 200 OK. He views Ana's
order, and the answer is 200 OK again. Being logged in proved who he was, and
nothing decided what he was allowed to do.

### 4. Authorization: what may this person do?

Authorization is a set of rules about roles and ownership, applied after
authentication. The gate now answers in two steps. First, who is this? No live
token gets 401. Then, may this person do this to this order? If not, 403
FORBIDDEN. Ben refunding is 403, and Ben viewing Ana's order is 403, but Ben
viewing his own order O-2 is 200. Sam, from support, may view any order and
refund, so both are 200. Ana cancelling Ben's order is 403. Alex, an admin,
cancelling it is 200. The rules are written once, in one place.

### 5. The bill: sessions expire, and roles go stale

The checks have costs. A session lasts only 3 ticks. Sam logs in at tick 0, and at
tick 4 his refund gets 401, because the session has expired: safer if a token is
stolen, and a new login for him. Then a subtler cost. Sam logs in, and then Alex
demotes him to a customer. If the role was copied into the session at login, Sam
can still refund: 200 OK, a stale role. If the gate looks up the role every
time, the same refund is 403 FORBIDDEN, at the price of one lookup per request.
Either choice is a trade between speed and being current.

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
