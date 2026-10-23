#!/usr/bin/env bash
# Makes the demo's certificates and keys once, with OpenSSL 3 and the JDK's keytool. Every secret here is a demo value and protects nothing.
# A demo root CA signs the server certificate for shop.example.test and a client certificate for Ana. An impostor has the same name and
# is signed by nobody anyone trusts. The root's private key stays in a temporary folder and is thrown away.
# The build runs it once and writes into build/ (PKI_OUT); nothing it makes is committed. Output: root.pem, shop.crt, shop.key, impostor.crt, impostor.key, client.p12, trust.p12 (password: demo-password).
set -euo pipefail
OPENSSL="${OPENSSL:-/opt/homebrew/opt/openssl@3/bin/openssl}"
[ -x "$OPENSSL" ] || OPENSSL=openssl
PW=demo-password
OUT="${PKI_OUT:-$(cd "$(dirname "$0")/.." && pwd)/build/generated/demo-keys/pki}"
mkdir -p "$OUT"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
cd "$WORK"
EC="-newkey ec -pkeyopt ec_paramgen_curve:prime256v1 -nodes"

$OPENSSL req -x509 $EC -keyout root.key -out root.pem -subj "/CN=Demo Root CA" -days 36500 \
  -addext "basicConstraints=critical,CA:TRUE" -addext "keyUsage=critical,keyCertSign,cRLSign"

issue() { # name subject extensions
  $OPENSSL req -new $EC -keyout "$1.key" -out "$1.csr" -subj "$2"
  printf '%s\n' "$3" > "$1.ext"
  $OPENSSL x509 -req -in "$1.csr" -CA root.pem -CAkey root.key -CAcreateserial -out "$1.crt" -days 36500 -extfile "$1.ext"
}
issue shop "/CN=shop.example.test" $'subjectAltName=DNS:shop.example.test\nextendedKeyUsage=serverAuth\nbasicConstraints=CA:FALSE'
issue client "/CN=Ana the customer" $'extendedKeyUsage=clientAuth\nbasicConstraints=CA:FALSE'

$OPENSSL req -x509 $EC -keyout impostor.key -out impostor.crt -subj "/CN=shop.example.test" -days 36500 \
  -addext "subjectAltName=DNS:shop.example.test"

$OPENSSL pkcs12 -export -inkey client.key -in client.crt -certfile root.pem -name ana -passout "pass:$PW" -out client.p12
keytool -importcert -alias root -file root.pem -keystore trust.p12 -storetype PKCS12 -storepass "$PW" -noprompt

cp root.pem shop.crt shop.key impostor.crt impostor.key client.p12 trust.p12 "$OUT/"
