# Dependency Confusion, Explained

## The idea in one sentence

A build must decide by name which repository may supply a package, fail closed when it is missing there, and check the exact bytes against a lockfile, because the highest version or the first answer may come from an attacker.

## The 5 acts

### 1. A build that works

json-lib 2.0.1 comes from the public registry, shop-pricing 1.4.2 and shop-audit 3.1.0 from the private repository. A lockfile records the three versions with their SHA-256 digests. Everything is as intended, and nothing yet shows the danger.

### 2. Somebody registers the online store's package name in the public registry

The attacker registers shop-pricing 99.0.0 (and shop-newlib 1.0.0, a name the online store has not published yet) in the public registry, each with an install script. A build that asks every repository and takes the highest version in range, for a requirement written as "at least 1.0", installs
the attacker's package: "INSTALL SCRIPT RAN" on the build machine, which holds the CI secrets, signing keys and deployment credentials.

### 3. Repository order helps only until a name is missing

Asking the repositories in order and taking the first that has the package fixes the version race: shop-pricing 1.4.2 comes from the private repository. But shop-newlib, an internal library that is not in the private repository yet, falls through to the public registry and the attacker's script runs.
And in a configuration with the public registry first, the attacker copies the exact version number the online store pinned (shop-audit 3.1.0), and the pin does not help: the attacker's copy comes first.

### 4. Names decide where a package may come from

Each repository declares which names it may supply: names that start with shop- come only from the private repository, everything else only from the public one. The build installs the right versions from the right places. The unpublished internal library is refused with a clear message
("an internal name; the private repository has no ..."): the build fails closed, and the attacker's package is never even looked at.

### 5. Locked digests, and watching the names

A resolver that wraps another and compares every result with the lockfile refuses the attacker's shop-pricing 99.0.0 (the lock says 1.4.2 with a given digest) even when the underlying tool would have picked it. If someone tampers with the private repository so that shop-pricing 1.4.2
has different contents, the digest no longer matches and the build stops. Last, a name watch asks the public registry for each internal name and reports shop-newlib and shop-pricing: somebody has registered them, and the security team should know the same day.

## The verdict

Route internal names to the private repository only (content filtering or a proxying repository manager with rules), keep CI from reaching the public registry directly, lock and verify digests, disable install scripts in CI where you can, reserve your names publicly, and alert when a public registry
has one of your internal names.

## How to recognise this in code you did not write

- `--extra-index-url` or several `repositories {}` blocks without content filters.
- Requirements written as ">=1.0" for internal packages.
- `exclusiveContent`, scoped packages and `--require-hashes` in build files.
- A finding titled "Dependency Confusion" or CWE-427.

## Where you have already met this

- `--extra-index-url` in a `pip` command or `requirements.txt`.
- `repositories { mavenCentral(); maven { url ... } }` in a Gradle build.
- Scoped npm packages such as `@company/pricing`.
- The 2021 write-up by Alex Birsan, which popularised the name "dependency confusion".
