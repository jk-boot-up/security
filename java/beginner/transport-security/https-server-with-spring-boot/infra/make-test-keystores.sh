#!/usr/bin/env bash
# Makes the demo's PKCS12 keystores once, with the JDK's keytool. The password is a demo value and the keys protect nothing.
# A demo root CA signs two server certificates for shop.example.test (the second stands for a renewed certificate) and one client certificate.
# The root's private key stays in a temporary folder and is thrown away; only what the demo needs is copied to src/main/resources.
set -euo pipefail
PW=demo-password
OUT="$(cd "$(dirname "$0")/.." && pwd)/src/main/resources"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
cd "$WORK"
KT="keytool -storetype PKCS12 -storepass $PW -keyalg EC -groupname secp256r1 -validity 36500"

$KT -genkeypair -alias root -dname "CN=Demo Root CA" -ext bc:c -ext ku:c=keyCertSign,cRLSign -keystore root.p12
keytool -exportcert -alias root -rfc -keystore root.p12 -storepass $PW -file root.pem

issue() { # name alias dn extension
  $KT -genkeypair -alias "$2" -dname "$3" -keystore "$1.p12"
  keytool -certreq -alias "$2" -keystore "$1.p12" -storepass $PW -file "$1.csr"
  keytool -gencert -alias root -keystore root.p12 -storepass $PW -infile "$1.csr" -outfile "$1.pem" -rfc -validity 36500 $4
  keytool -importcert -alias root -file root.pem -keystore "$1.p12" -storepass $PW -noprompt
  keytool -importcert -alias "$2" -file "$1.pem" -keystore "$1.p12" -storepass $PW
}
issue shop shop "CN=shop.example.test" "-ext san=dns:shop.example.test -ext eku=serverAuth"
issue shop2 shop "CN=shop.example.test" "-ext san=dns:shop.example.test -ext eku=serverAuth"
issue client ana "CN=Ana the customer" "-ext eku=clientAuth"

# An impostor: the same name, but signed by nobody anyone trusts.
$KT -genkeypair -alias shop -dname "CN=shop.example.test" -ext san=dns:shop.example.test -keystore impostor.p12

# What both sides trust: just the root.
keytool -importcert -alias root -file root.pem -keystore trust.p12 -storetype PKCS12 -storepass $PW -noprompt

cp shop.p12 shop2.p12 client.p12 impostor.p12 trust.p12 "$OUT/"
