# Security Misconfiguration, Explained

## The idea in one sentence

Security misconfiguration is a setting left at an insecure value, such as a default password, a debug page or a stack trace, and the defence is secure defaults, a gate that refuses to start production with them, a drift check against a baseline, and a checker that runs on every change.

## The 5 acts

### 1. The store started with its shipped defaults

The store is started with its shipped settings. An admin login with the default password admin succeeds. GET /debug/config answers and the page shows the admin password. GET /static/ lists three files,
including backup-orders.csv. GET /orders?id=abc fails with a stack trace in the page. The checker scores the shipped store 0 of 8: every default is a finding.

### 2. Secure defaults, and a gate at startup

The same store with the hardened profile passes all eight checks: the default password fails, there is no debug page and no listing, errors show only a reference, the Server header names nothing,
TRACE is refused, the settings file is owner-only, and the profile is production with no test settings. The startup gate refuses to start a production store whose admin password is set back to admin, or left empty.

### 3. Drift: development settings that reach production

Production is made by copying the development file and changing five settings. The drift check finds two settings that still have their development value, debug.endpoint and errors.detailed, and one
development-only setting, test.fake-payments, so it fails the deployment. If it were deployed anyway, the store would refuse to start because a debugging switch is on. The hardened profile, compared the same way, shows no drift.

### 4. Banners, methods and file permissions

The shipped store sends Server: ShopServer/1.4.2 (Java 25), which tells an attacker which advisories to look up; the hardened store sends no Server header. TRACE on the shipped store answers 200 and
echoes the request headers back; the hardened store refuses it with 405. A settings file written with rw-r--r-- is readable by other users on the machine, and it holds the admin secret; written with rw------- it is not.

### 5. The bill: a baseline that must be checked again and again

A new feature adds a setting the baseline has never seen, uploads.max-size, and the drift check flags it for review. Every feature adds settings, and every setting has an insecure value, so the baseline
grows with the store. The same checks belong in CI on every change, at every startup, and against the live store on a schedule. A passing score means these eight checks pass, not that the store is secure.

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
