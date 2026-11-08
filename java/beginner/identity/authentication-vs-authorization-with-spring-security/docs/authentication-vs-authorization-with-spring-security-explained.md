# Authentication vs Authorization with Spring Security, Explained

## The idea in one sentence

Spring Security answers who is asking with a filter chain, a user-details service and a password encoder such as BCrypt, answers what they may do with rules like `@PreAuthorize`, and turns the two kinds of refusal into 401 and 403.

## The 7 acts

### 1. Spring Security present, everything permitted

Adding Spring Security to a project does not protect it by itself. In version one the security configuration says
`anyRequest().permitAll()`. The requests: nobody, with no Authorization header, asks to refund order O-1 and to view it.
The responses: 200 OK and 200 OK. The framework is there; nothing asked it to check anything.

### 2. Authentication: HTTP Basic and BCrypt

Version two requires a login. Spring Security's Basic authentication filter reads the username and password from the
Authorization header, a user-details service holds the accounts, and a BCrypt password encoder checks the password against
the stored line. The requests all ask for order O-2: with no login, as ben with the password guess, and as ben with ben-pass.
The responses: 401 with no password check, 401 after one failed check, and 200 OK after one successful check.

### 3. The login on the wire: the HTTP Basic header

HTTP Basic joins the username and password with a colon: ben:ben-pass, 12 bytes. Base64 then writes every 3 bytes as 4 letters:
ben becomes YmVu, :be becomes OmJl, n-p becomes bi1w and ass becomes YXNz. Every request carries the header
`Authorization: Basic YmVuOmJlbi1wYXNz`. Base64 is an encoding, not encryption: anyone who sees the header decodes it back to
ben:ben-pass, which is why Basic authentication belongs only on HTTPS.

### 4. The password check, step by step: BCrypt

When ben signs up, BCrypt takes his password ben-pass, a salt and a cost of 4 (2^4 = 16 rounds, slow on purpose). The demo fixes
the salt to ShopSaltForBenAccount. so the output repeats; real use takes a random salt every time. The stored line has four parts:
version $2a$, cost 04, the salt, and a 31-letter hash, Wz1TULMouKL9lQ/.NmDs1YmiNyGmOb6. The password itself is never stored.
To check a login, the typed password is hashed again with the stored salt and cost: ben-pass gives the same hash, guess gives
t56XmVfz7dgNW5z2vW3Vm4JaetzqNUe, which is different. Spring's own BCrypt encoder agrees: true for ben-pass, false for guess.

### 5. Authenticated is not the same as authorized

The configuration now says `anyRequest().authenticated()`. Every request needs a valid login, and the team feels safe.
The requests: ben, a customer, logged in correctly, refunds order O-1, which is ana's, and views it. The responses: 200 OK and
200 OK. Spring Security found out who ben is, and nothing decided what he may do.

### 6. Authorization: @PreAuthorize rules on the endpoints

Method security is switched on, and each endpoint carries its rule. Viewing needs the SUPPORT or ADMIN role, or ownership of
the order. Cancelling needs ADMIN, or ownership. Refunding needs SUPPORT or ADMIN. Spring checks the rule before the method runs.
The eight requests and their responses: nobody refunds, 401; ben refunds, 403; ben views ana's order, 403; ben views his own
order O-2, 200; sam views and refunds, 200 and 200; ana cancels ben's order, 403; alex cancels it, 200. The filter chain turns a
missing login into 401 and a refused rule into 403.

### 7. The bill: a forgotten rule, a check every time

Two costs. First, HTTP Basic sends the password on every request, so Spring checks it on every request: sam makes 3 requests
and 3 BCrypt checks happen, each slow on purpose. Second, before the fix: a developer adds an export endpoint and forgets its
`@PreAuthorize`, and ben, a customer, exports every order: 200 OK. After the fix: a second rule, on the address,
`/orders/export` needs the ADMIN role. Ben gets 403 and alex gets 200. Two layers cost a little more configuration and cover
for human mistakes.

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
