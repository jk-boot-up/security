# infra

`nginx-hardened.conf` is the configuration the demo uses in act two, written out so it can be read without running anything.
The demo generates its configurations from `NginxConfig`, and a test checks that this file is exactly what `NginxConfig` produces
for the five security headers with `always` and `server_tokens off`.

To try it by hand with Docker:

```bash
docker run --rm -p 8080:80 -v "$PWD/nginx-hardened.conf:/etc/nginx/nginx.conf:ro" nginx:1.31.7-alpine
curl -i http://127.0.0.1:8080/
curl -i http://127.0.0.1:8080/missing
```

Both answers carry the five headers. Remove the word `always` from one line and the 404 page loses that header.
