# Problem Statement

## The scenario

An online store has a bug between a client and a server, and someone wants to read the traffic; or an attacker has a recording and wants the passwords in it.

## The naive version

Turn off TLS in test, or assume that a recording of TLS traffic is useless to everyone.

## What this project must deliver

- A real capture of a real TLS 1.3 login.
- What it shows with and without secrets.
- Why a key log is limited to one connection.
- Why forward secrecy protects old recordings.
