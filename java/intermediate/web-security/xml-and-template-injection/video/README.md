# XML External Entities and Template Injection — Teaching Video

A narrated, slide-based video built by `./build_video.sh` with the shared videokit library.

| File | What it is |
| --- | --- |
| `xml-and-template-injection-explained.mp4` | the video, 1920×1080 |
| `xml-and-template-injection-explained.m4a` | audio only |
| `xml-and-template-injection-explained.srt` | subtitles |
| `poster.png` | the opening frame |

**Narration:** Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice). Scenes are generated from `../concept.toml` into `scenes.py`.

Suggested description:

> XML external entities, XXE, and server-side template injection, SSTI, in Java. An XML parser and a template engine both run instructions they find in their input: an invoice upload can make the parser read a file from the server, and a customer's name pasted into an e-mail template can run as an expression and print the application's mail password. Explained with an online store, like a form whose small print says: attach a copy of whatever is in the manager's drawer, and a clerk who obeys it. We watch both attacks work, then refuse the DOCTYPE that invoices never need, switch to a logic-less template engine with named, escaped holes, and pass the customer's text in as data, never as part of the template. We finish with the cost: some XML really needs a DOCTYPE, and parser settings differ between libraries.
