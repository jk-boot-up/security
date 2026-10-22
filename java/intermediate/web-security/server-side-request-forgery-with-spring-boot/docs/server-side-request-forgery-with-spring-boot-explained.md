# Server-Side Request Forgery with Spring Boot, Explained

## The idea in one sentence

A server that fetches an address for a customer lends out its network position, so the address that is really connected to must be judged, after resolution, and the connection must go to that same address.

## The 5 acts

### 1. Importing an image from a web address

Staff paste an address into a form. The Spring Boot shop posts it to the importer, which fetches the address with RestClient and returns the image bytes. The honest address gives PNG-cat.

### 2. The server fetches whatever it is told

The first version fetches any address. The cloud metadata address returns a demo access key, localhost returns a demo database password, and the office admin page returns its customer list. None of these can be reached from outside, but the shop's server can.

### 3. A blocklist of names is easy to walk around

The second version refuses a list of host names. The metadata address is refused by name. The same address written as the decimal number 2852039166, localhost as 127.1, the metadata address as a mapped IPv6 address, an attacker's name that resolves to the office address, and a redirect from an outside address all
get through, because the list compares text and the network layer reads an address.

### 4. Check the address the connection really uses

The guarded importer resolves the host name once and judges every address it resolves to. The decimal spelling, 127.1, the mapped IPv6 address, the name that points inside, the redirect and a name with two answers (one private) are all refused, with the address they resolved to in the message.
User info in the address is refused. The attacker's own image from a name that rebinds is fetched, and nothing else, because the connection goes to the address that was judged. The honest address still imports.

### 5. Limits, and why checking and then connecting is not enough

The guarded importer allows only ports 80 and 443, only images, at most 1024 bytes, and an answer within 1500 milliseconds. A web page, a 5000 byte answer and a slow server are each refused. The last row shows the mistake that looks like a fix: a client that resolves the name, judges it and
then resolves the name again to connect. The attacker's name server answers a public address first and the private office address next, so the office page is imported.

## The verdict

Resolve once, judge every address, connect to the judged address, revalidate every redirect, allow only the ports and content you need, cap size and time, prefer an allow-list of hosts where you have one, and add network egress rules as a second wall.

## How to recognise this in code you did not write

- `RestClient.get().uri(url)` with a user-supplied URL.
- `isSiteLocalAddress()` checks in validation helpers.
- A cloud metadata address in an incident report.
- "Import from URL" in an admin form.

## Where you have already met this

- `RestClient.get().uri(url)` with a URL from a request parameter.
- A cloud metadata address in an incident report.
- `InetAddress.isSiteLocalAddress()` in a validation helper.
- A webhook or image import form that takes a URL.
