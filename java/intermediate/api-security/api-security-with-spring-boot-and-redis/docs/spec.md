# Spec — API Security with Spring Boot and Redis

## Purpose

Teach three API limits in Spring Boot: what a caller may set, what they may see, and how much they may ask for.

## Not in scope

- A full API gateway.
- Object-level authorization across accounts, which has its own projects.
- Distributed rate limiting across data centres.

## The problem

Teams bind bodies to entities, return entities, and rate-limit in one server's memory, and each of the three gives an attacker something.

## The pieces

| Role | Class |
| --- | --- |
| Controllers | `NaiveAccountsController`, `SafeAccountsController` |
| Models | `Account`, `UpdateProfileRequest`, `AccountView`, `DirectoryEntry` |
| Errors | `ErrorHandling` |
| The budget | `RateLimiter`, `RedisRateLimiter`, `LocalRateLimiter`, `RateLimitFilter`, `ManualClock` |
| Test support | `RedisLab`, `Client`, `Running` |
| Entry point | `ApiSpringDemo` |

## Requirements

1. The first version applies any field a body names and returns the whole record.
2. The second version refuses unknown fields and invalid values with a short fixed answer.
3. The second version returns only the fields of a view and masks the card.
4. A page is capped, and a size below one is refused.
5. Each signed-in caller has a bucket that refills with the clock.
6. Servers sharing a Redis share one budget, and taking a token is atomic.
7. The API refuses with 503 when the limiter cannot answer.
8. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 21, all passing |
| Demo output | 35 lines, identical on every run |
| Acts | 5 |
| Video | 07:02, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
