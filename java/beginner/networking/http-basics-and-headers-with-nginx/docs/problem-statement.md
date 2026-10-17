# Problem Statement

## The scenario

An online store runs its application behind nginx. The headers, the limits and the treatment of odd requests are decided in nginx's configuration, which few people test.

## The naive version

Copy an add_header snippet into the server block, check the home page in a browser, and assume every page has the headers.

## What this project must deliver

- A generator for nginx configurations in eight variants, and a reference file written out for reading.
- A raw-socket client that can send malformed and ambiguous requests.
- The same five-header auditor as the plain project, applied to real answers.
- Tests that start real nginx containers and prove every claim, with no container left behind.
