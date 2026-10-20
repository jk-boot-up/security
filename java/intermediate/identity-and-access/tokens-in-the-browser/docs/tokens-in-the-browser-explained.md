# Tokens in the Browser: BFF and DPoP, Explained

## The idea in one sentence

A token that a script can read is a token an attacker can take, so keep tokens on a backend behind an HttpOnly cookie, or bind them to a key with DPoP, and still fix the cross-site scripting that put the script there.

## The 4 acts

### 1. A token in local storage

After sign-in the app keeps the access token in localStorage and sends it as a bearer token. The API answers 200 with Ana's balance. It is the simplest design, and every tutorial shows it.

### 2. One injected script takes the token away

An injected script (cross-site scripting, or a compromised third-party library) can read everything in web storage. It copies the access token to its owner. The attacker's own computer presents it as a bearer token and the API answers 200,
because a bearer token is accepted from whoever holds it. Closing the page does not help: the token is already gone.

### 3. A backend for the frontend keeps the token

The BFF signs Ana in, keeps the access token in its own memory and gives the page only an HttpOnly session cookie. The injected script reads nothing: the list of what it can steal is empty. The attacker's computer, with no cookie, gets 401.
A form post from evil.example carries the cookie but not the app's origin or custom header, so the BFF refuses it with 403. The honest limit: a script running in the page can still call the BFF through the browser while the page is open,
so XSS still has to be fixed. After sign-out the same cookie is dead.

### 4. A token that needs the key as well (DPoP)

The app makes an ES256 key pair, and the authorization server binds the token to the key's thumbprint. Each call carries `Authorization: DPoP token` and a proof: a signed statement of the method, address, time, a unique id and the token's hash.
The API checks the signature, the key against the token's binding, the method, the address, the time window and the token hash, and remembers each proof id. The honest call gets 200. The same proof again is "already used". The stolen token as a bearer token
is refused, the token without a proof is refused, a proof from the attacker's own key is "signed with another key", a proof for a transfer cannot be used for a balance, and a proof saved for later is too old. A fresh proof from the real key works.

## The verdict

For a first-party single-page app, prefer a BFF with a `__Host-` HttpOnly SameSite cookie and CSRF protection. When tokens must be in the browser, bind them with DPoP and keep the private key non-extractable. Never store long-lived tokens in
web storage, and keep scripts out of the page with a strict Content-Security-Policy.

## How to recognise this in code you did not write

- `localStorage.setItem` or `sessionStorage.setItem` holding a token.
- A `/bff/` or `/api` proxy path that adds the `Authorization` header on the server.
- `Authorization: DPoP` and a `DPoP` header.
- `Set-Cookie: __Host-...; HttpOnly; Secure; SameSite=Lax`.

## Where you have already met this

- `localStorage.setItem('access_token', ...)` in single-page app tutorials.
- The advice "never store tokens in localStorage" and the argument over cookies versus storage.
- `Authorization: DPoP ...` and a `DPoP:` header in newer OAuth libraries.
- A BFF (backend for the frontend) in the OAuth working group's browser-apps guidance.
