# Dependency Confusion, Explained

## The idea in one sentence

A build must decide by name which repository may supply a package, fail closed when it is missing there, and check the exact bytes against a lockfile, because the highest version or the first answer may come from an attacker.

## The 5 acts

### 1. A build that works

The input: the dependencies (a name and the versions allowed, written [from, below)), and what each repository holds: company-private has shop-pricing 1.4.2 and shop-audit 3.1.0, public-registry has json-lib 2.0.1.
The result: json-lib 2.0.1 from public-registry, shop-pricing 1.4.2 and shop-audit 3.1.0 from company-private. The lockfile records the three versions and digests.
How a digest is made: three lines, "shop-pricing@1.4.2", "internal pricing rules" and an empty line for the missing install script, joined by newlines, are 42 bytes of UTF-8; their SHA-256 is 64 hex digits, and one changed byte changes all of them.

### 2. Somebody registers the store's name in the public registry

The input: the attacker, who knows only the names, publishes shop-pricing 99.0.0 and shop-newlib 1.0.0 to public-registry, each with an install script ("send CI secrets to attacker.example", only text here).
How the highest version is chosen for shop-pricing >= 1.0.0: company-private has 1.4.2 and public-registry has 99.0.0; both are in range. Versions compare number by number from the left: major 99 > 1, so 99.0.0 is higher.
The result: shop-pricing 99.0.0 from public-registry is installed and its install script runs on the build machine, which holds the CI secrets, signing keys and deployment credentials. shop-audit is safe here only because it is pinned to 3.1.0 and the attacker has not published that number.

### 3. Repository order helps only until a name is missing

The setup: the repositories are asked in order, and the first one that has the name answers (Maven and Gradle by default). With company-private first, shop-pricing 1.4.2 comes from company-private.
A new internal library, shop-newlib, is not in company-private yet: company-private has no shop-newlib, public-registry has the attacker's 1.0.0, so the name falls through and the script runs.
With public-registry first (a common misconfiguration), the store pins shop-audit [3.1.0, 3.1.1), and the attacker publishes the same number, 3.1.0: the first repository has it, so the pin does not help.

### 4. Names decide where a package may come from

The input, the rule: a name that starts with shop- is internal. json-lib is public, so public-registry only; shop-pricing, shop-audit and shop-newlib are internal, so company-private only. (Gradle calls this exclusive content; npm uses scopes; repository managers use routing rules.)
The result for the dependencies as before: json-lib 2.0.1 from public-registry, shop-pricing 1.4.2 and shop-audit 3.1.0 from company-private.
The result with the unpublished shop-newlib: refused, "shop-newlib is an internal name; the private repository has no >= 0.0.0". The build fails closed; the public registry is never asked for a shop- name.

### 5. Locked digests, and watching the names

How the lockfile check is made, for shop-pricing: the lockfile says 1.4.2 and a digest. The public-registry package is 99.0.0: the version differs. A changed package in company-private is 1.4.2 with other bytes: the digest differs.
The result with the highest-version rule and the lockfile: json-lib and shop-audit are installed, shop-pricing 99.0.0 from public-registry is refused. With names deciding but a changed 1.4.2 in company-private: shop-pricing 1.4.2 is refused too.
The name watch asks the public registry for each internal name (shop-audit, shop-newlib, shop-pricing) and finds shop-newlib and shop-pricing: somebody has registered them, and the security team should know the same day.

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
