# An HTTPS Server with Spring Boot, Explained

## The idea in one sentence

A Spring Boot server gets HTTPS from an SSL bundle named in properties, redirects plain HTTP, can renew its certificate without a restart and can require client certificates, but every one of those settings is silent when wrong, so each must be tested from outside.

## The 5 acts

### 1. TLS from an SSL bundle

TLS is configured with properties and no TLS code: server.ssl.bundle=shop names an SSL bundle, spring.ssl.bundle.jks.shop.keystore.location points to shop.p12, and options.enabled-protocols=TLSv1.3 allows TLS 1.3 only.
The server presents a certificate with subject CN=shop.example.test, issued by CN=Demo Root CA, the root the client's trust bundle holds. Four attempts: (a) the trust bundle, TLS 1.3, shop.example.test: 200 over TLSv1.3, and Spring's RestClient reads kettle, toaster, lamp;
(b) the same, reached as evil.example.test: refused, the name on the certificate must match; (c) a client that offers only TLS 1.2: refused; (d) the JDK's default trust instead of the bundle: refused, because the demo root is not in the JDK's list.

### 2. Redirect, HSTS and secure cookies

A request to the plain HTTP port, GET http://shop.example.test/products, is answered 302 with Location: https://shop.example.test:<https port>/products, an empty body and no Strict-Transport-Security. The HTTPS answer to GET /login carries Strict-Transport-Security: max-age=31536000 ; includeSubDomains,
and 31536000 / 86400 seconds a day = 365 days. It also sets the session cookie JSESSIONID with a random 32-character id and Path=/; Secure; HttpOnly; SameSite=Strict. The client attaches it over https and not over http, because of Secure.

### 3. Renewing a certificate without a restart

The shop bundle is set to reload: spring.ssl.bundle.jks.shop.reload-on-update=true. The SHA-256 fingerprints are shown as letters, because the keys are made on each machine. Before: shop.p12 on disk is A, and the certificate served is A.
Then shop.p12 is replaced by the renewed file and the server is not restarted. After: the file is B and the certificate served is B. The renewed certificate is CN=shop.example.test, issued by CN=Demo Root CA, so a client that trusts the root still connects, ok (200), with no change.

### 4. Mutual TLS: the server checks the client too

Two settings turn on mutual TLS: server.ssl.client-auth=need, and a trust store in the shop bundle (trust.p12) that the server uses to check client certificates. Three clients connect. Ana presents CN=Ana the customer, issued by CN=Demo Root CA: 200, and the server saw CN=Ana the customer.
A client with no certificate is refused. Mallory presents CN=shop.example.test, issued by itself: refused. This identifies a machine or an app, not a person who types a password, so it suits service-to-service calls.

### 5. What Spring leaves to you

Three requests probe what Spring Boot might give away. GET /actuator/health answers 404: the actuator is not on the class path, so nothing extra is exposed. GET /boom throws an error that holds a password (database password is hunter2): it answers 500, and the password is not in the body.
GET /products answers 200 with no Server header. The bill: a wrong property fails silently, so check the result from outside; and the private key is a file the server can read, so protect it, rotate it and renew it before it expires.

## The verdict

Use an SSL bundle with TLS 1.3 only, a redirect and HSTS, secure cookies, and reload with a renewal process that someone owns. Require client certificates between services. Put TLS in a proxy when you can, test from outside whatever you configure, and protect the key file.

## How to recognise this in code you did not write

- `server.ssl.bundle` and `spring.ssl.bundle.*` in application properties.
- `server.ssl.client-auth=need` in internal service configuration.
- A certificate-expiry alert in a monitoring system.
- `http.redirectToHttps(...)` in a security configuration.

## Where you have already met this

- `server.ssl.*` properties in `application.properties` files.
- `.p12` and `.jks` files next to an application, and a password in its configuration.
- The "certificate has expired" outage that wakes people up.
- Service meshes and internal APIs that require client certificates.
