# Problem Statement

## The scenario

A bank's services trust each other through certificates, and one that expires or is stolen can stop or expose a payment.

## The naive version

Issue certificates for a year, trust a revocation check that quietly gives up, and find expiries by the first customer complaint.

## What this project must deliver

- A private CA with a root and an intermediate.
- CRL, OCSP and stapling measured with hard and soft fail.
- Short-lived certificates renewed by step-ca, and the outage when a renewal is missed.
- An expiry monitor over a fleet of certificates.
