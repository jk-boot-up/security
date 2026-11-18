# Security Misconfiguration, Explained

## The idea in one sentence

Security misconfiguration is a setting left at an insecure value, such as a default password, a debug page or a stack trace, and the defence is secure defaults, a gate that refuses to start production with them, a drift check against a baseline, and a checker that runs on every change.

## The 5 acts

### 1. The store started with its shipped defaults

The shipped settings file says profile=dev, admin.password=admin, debug.endpoint=true, static.listing=true, errors.detailed=true, server.banner=ShopServer/1.4.2 (Java 25), http.trace=true and test.fake-payments=true.
A scanner's first four requests: POST /admin/login with password=admin answers 200 welcome, admin. GET /debug/config answers 200 and shows admin.password=admin. GET /static/ answers 200 with backup-orders.csv, index.html and logo.png. GET /orders?id=abc answers 500 with java.lang.NumberFormatException: For input string: "abc", the first line of a stack trace.
The checker then runs eight checks, from outside and on the settings file: every one fails, so the shipped store scores 0 of 8.

### 2. Secure defaults, and a gate at startup

The hardened settings file says profile=prod, a 44-character production secret, debug.endpoint, static.listing, errors.detailed and http.trace all false, an empty server.banner and no test settings.
The same eight checks now all pass: the login with admin answers 401, the debug page and the listing answer 404, the error page shows no trace, there is no Server header, TRACE answers 405, the file is rw------- and the profile is prod with no test settings. The score is 8 of 8.
The startup gate runs before the store opens its port, only in production. The admin password must have at least 12 characters, must not be empty and must not be admin, password, changeme, 123456, root or secret; the debugging switches must be off and no test.* setting may be present. admin is refused as a known default, an empty password as empty, Summer2026 because it has 10 characters, under 12, and the production secret starts.

### 3. Drift: development settings that reach production

Production is made by copying the development file and changing 5 settings: profile=prod, the production secret, static.listing=false, an empty server.banner and http.trace=false.
The drift check goes setting by setting through the six that must differ from development: admin.password, static.listing, server.banner and http.trace changed, but debug.endpoint and errors.detailed are true in both. Then key by key: test.fake-payments is a development-only key.
Drift found: true, so the deployment fails. If it were deployed anyway, the gate refuses it: a debugging switch is on in production. The hardened profile, checked the same way, shows no drift.

### 4. Banners, methods and file permissions

GET / to both stores: the shipped one answers with Server: ShopServer/1.4.2 (Java 25), which names the version an attacker can look up advisories for; the hardened one sends no Server header.
TRACE / with the customer's cookie, session=c0ffee42: the shipped store answers 200 and echoes Cookie: session=c0ffee42 back in the body; the hardened store answers 405 method not allowed.
The settings file's mode is three groups of three letters, for the owner, the group and others. rw-r--r-- gives the group and others r, read, so other users on the machine can read the admin secret; rw------- gives them nothing.

### 5. The bill: a baseline that must be checked again and again

A new feature adds the setting uploads.max-size=unlimited. It is not in the baseline, so the drift check flags it for review.
Every feature adds settings, each with an insecure value, so the baseline grows with the store. The checks run in CI, on every change, at every startup, and against the live store on a schedule.
Passing means these 8 checks pass, not that the store is safe: anything the checker does not test is still open.

## The verdict

State every setting in the production profile. Turn off debug pages, listings, detailed errors, version banners and unused methods. Refuse to start with a default or empty secret. Compare
production with the baseline before every deployment, run the checker against the live service on a schedule, and keep settings files readable by their owner only.

## How to recognise this in code you did not write

- Scanner reports that list `admin`/`admin`, `/actuator`, `/debug` or directory listings.
- Error pages with class names, file paths and versions.
- `Server` and `X-Powered-By` headers with product versions.
- CIS Benchmark results and hardening guides.

## Where you have already met this

- Routers, cameras and databases that ship with `admin`/`admin` and are found by scanners within minutes.
- Spring Boot's actuator, Tomcat's manager and other admin consoles left reachable.
- Error pages that show a stack trace with class names and versions.
- Benchmarks such as the CIS Benchmarks, which are long lists of exactly these settings.
