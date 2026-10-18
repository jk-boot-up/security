# Race Conditions and Business Logic — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `race-conditions-and-business-logic-explained.mp4` | the video, 1920×1080 |
| `race-conditions-and-business-logic-explained.m4a` | audio only |
| `race-conditions-and-business-logic-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> Race conditions and business logic flaws in Java, OWASP Top 10 A06, insecure design. Explained with an online bank and store, like two cashiers who each check the same nearly empty account and both hand over the money. We watch two withdrawals of 80 from a balance of 100 both succeed, and a single-use coupon used twice, by running the steps of two requests in a written order; fix them with a lock and with a compare-and-set, so the balance can never go below zero; stop a retried payment from paying twice with an idempotency key; keep the server's own rules about negative amounts, transfers to yourself, stacked coupons, totals sent by the client and orders refunded twice; and finish with the bill: locks make requests take turns, locks taken in the wrong order deadlock, and a lock in one server does not stop another.
