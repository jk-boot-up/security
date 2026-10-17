# Authentication vs Authorization with Spring Security, Explained

## The idea in one sentence

Spring Security answers who is asking with a filter chain, a user-details service and a password encoder such as BCrypt, answers what they may do with rules like `@PreAuthorize`, and turns the two kinds of refusal into 401 and 403.

## The 5 acts

### 1. Spring Security present, but every request permitted

Adding Spring Security to a project does not protect it by itself. In version one
the security configuration says `anyRequest().permitAll()`. A request with no
credentials asks to refund order O-1, and the answer is 200 OK. The framework is
there; nothing asked it to check anything.

### 2. Authentication: HTTP Basic, passwords stored as BCrypt

Version two requires a login. Spring Security's Basic authentication filter reads
the username and password from the request. A user-details service holds the
accounts, and a password encoder checks the password against a stored BCrypt
hash. A stored password starts with $2a$04$, which means BCrypt, cost 4. A request
with no credentials gets 401. Ben with a wrong password also gets 401. Ben with his
password gets 200 OK. A 401 answer tells the client to ask for credentials.

### 3. Authenticated is not the same as authorized

The configuration now says `anyRequest().authenticated()`. Every request needs a
valid login, and the team feels safe. But Ben is a customer, and he refunds order
O-1, which is Ana's: 200 OK. He views Ana's order: 200 OK. Spring Security found out
who Ben is, and nothing decided what he may do.

### 4. Authorization: @PreAuthorize rules on the endpoints

Method security is switched on, and each endpoint carries its rule. Viewing needs the
support or admin role, or ownership of the order. Cancelling needs the admin role, or
ownership. Refunding needs support or admin. Spring checks the rule before the method
runs. No credentials: 401. Ben refunding: 403. Ben viewing Ana's order: 403. Ben viewing
his own order O-2: 200. Sam viewing and refunding: 200. Ana cancelling Ben's order: 403.
Alex cancelling it: 200. Spring does the 401 or 403 split for us: the filter chain turns
a missing login into 401, and a refused rule into 403.

### 5. The bill: a forgotten rule, and a password check on every request

Two costs. First, HTTP Basic sends the password on every request, so Spring checks it on
every request. Sam makes 3 requests and 3 BCrypt checks happen, which are slow on purpose.
Second, a developer adds an export endpoint and forgets its `@PreAuthorize`. Ben, a
customer, exports every order: 200 OK. A second rule, written on the address, `/orders/export`
needs the admin role, catches it: Ben gets 403 and Alex gets 200. Two layers cost a little
more configuration and cover for human mistakes.

## The verdict

Use the framework's filters and encoders rather than writing your own, write the rules as short sentences on
the endpoints, and add a second rule on the address for anything that matters. Test the rules as a table of who
may do what, and check the sensitive endpoints with a request that must be refused.

## How to recognise this in code you did not write

- A `SecurityFilterChain` bean with an `authorizeHttpRequests` block.
- `@EnableMethodSecurity` and `@PreAuthorize` on service or controller methods.
- `{bcrypt}` at the start of stored passwords.
- A browser's built-in sign-in box, which is HTTP Basic.

## Where you have already met this

- The `@PreAuthorize`, `@Secured` and `hasRole(...)` annotations in almost every Spring application.
- A `SecurityFilterChain` bean, and the `authorizeHttpRequests` block inside it.
- The `{bcrypt}` prefix in stored passwords of Spring applications.
- The browser's built-in sign-in box, which is HTTP Basic.
