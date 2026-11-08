# Session Guide — Digital Signatures (Ed25519)

## Learning Objectives

By the end of the session you can:

- explain how a digital signature differs from an HMAC tag
- say who can make and who can check a signature
- explain why a signature should carry a purpose, a version and an expiry
- explain why a signature is only as good as the public key it is checked against

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A tag made with a shared key | 5 min |
| 0:15 | Act 2: A signature: the private key signs, the public key checks | 5 min |
| 0:20 | Act 3: How the signature is made, step by step | 5 min |
| 0:25 | Act 4: What exactly is signed | 5 min |
| 0:30 | Act 5: A good signature on an old message | 5 min |
| 0:35 | Act 6: The bill: whose key is it, and what if it is stolen? | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Start with the notary analogy. Run the demo one act at a time. After act one, ask the group who can forge, and why that cannot be fixed with a better tag. After act three, ask which step needs the secret a, and why the check does not. After act four, ask what could go wrong if the
refund order and the price list used the same label. After act five, ask where the version should be stored, and what happens after a restart. In act six, ask how a checkout should come to hold the right public key in the first place.

## Exercises

1. Add a third purpose label, shop.discount.v1, and test that signatures for one purpose never verify for another.
2. Make the checkout store the newest version to a file so it survives a restart.
3. Sign the price list with a validity of 30 days, and write the test for day 31 first.
4. Compare the size of an Ed25519 signature with an RSA-2048 signature made with `Signature.getInstance("SHA256withRSA")`.
