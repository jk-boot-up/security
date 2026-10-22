# Problem Statement

## The scenario

An online store runs several services behind TLS, and the settings come from many hands and many blog posts.

## The naive version

Open the site in a browser; if the padlock is there, it is fine.

## What this project must deliver

- A real scanner against five real servers.
- A policy of rules over the scanner's report, with exceptions that carry a reason.
- The findings no browser shows.
- The ways a scan can be wrong.
