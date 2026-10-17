# infra

`nginx-limit-by-address.conf` is the configuration the demo uses in acts two and four, written out so it can be read
without running anything. The demo generates its configurations from `NginxConfig`, and a test checks that this file
is exactly what `NginxConfig` produces for a limit on the client address.

To try it by hand with Docker:

```bash
docker run --rm -p 8080:80 -v "$PWD/nginx-limit-by-address.conf:/etc/nginx/nginx.conf:ro" nginx:1.31.7-alpine
curl -i -X POST "http://127.0.0.1:8080/login?user=dana"
```

The first six requests answer 401, and the seventh answers 429.
