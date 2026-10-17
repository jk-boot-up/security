# One-Time Codes (TOTP) — Data Flow Diagram

The time becomes a counter, the counter goes through HMAC with the secret, and a few bytes of the result become six digits.

![How a code is made](images/data-flow-diagram.png)


