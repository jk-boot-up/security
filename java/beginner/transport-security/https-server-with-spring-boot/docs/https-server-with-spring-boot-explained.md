# An HTTPS Server with Spring Boot, Explained

## The idea in one sentence

A Spring Boot server gets HTTPS from an SSL bundle named in properties, redirects plain HTTP, can renew its certificate without a restart and can require client certificates, but every one of those settings is silent when wrong, so each must be tested from outside.

## The 5 acts

### 1. TLS from an SSL bundle

The server's certificate and key, and TLS 1.3 only, are set in properties: server.ssl.bundle names a bundle, and the bundle names the keystore. A client built from a bundle that trusts only the demo root asks for /products and gets 200 over TLS 1.3.
The same call through Spring's RestClient works. The same server reached by another name, evil.example.test, is refused, because the name on the certificate must match. A client that offers only TLS 1.2 is refused. And a client that uses the JDK's
default trust and not the bundle is refused, because the demo root is not in the JDK's list.

### 2. Redirect, HSTS and secure cookies

A request to the plain HTTP port is answered with 302 pointing to an https address, with an empty body and without the HSTS header, because a browser must ignore HSTS received over plain HTTP. The HTTPS answer carries Strict-Transport-Security with a
year and includeSubDomains. The session cookie is Secure, HttpOnly and SameSite=Strict. The client would send the cookie over HTTPS, and never over plain HTTP.

### 3. Renewing a certificate without a restart

A server with reload-on-update serves the certificate that is on disk. The file is replaced with a renewed certificate, and the server is not restarted. The certificate served has changed, and it matches the new file by fingerprint.
Clients that trust the root still connect without any change, because both certificates are signed by the same root.

### 4. Mutual TLS: the server checks the client too

The server is told server.ssl.client-auth=need and the bundle gets a trust store for client certificates. Ana's client presents a certificate signed by the demo root, and the server sees who she is, from the certificate.
A client with no certificate is refused, and so is a client whose certificate is signed by nobody. This identifies a machine or an app, not a person who types a password, so it suits calls between services.

### 5. What Spring leaves to you

The actuator is not on the class path, so /actuator/health gives 404 and nothing extra is exposed. A failing page answers 500 and its body does not mention the internal detail. There is no Server header naming the software.
But configuration is short, and that is the risk: a wrong property is as silent as a right one, so check the result from outside. And the private key sits in a file the server can read, so protect it, rotate it, and renew before it expires.

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
