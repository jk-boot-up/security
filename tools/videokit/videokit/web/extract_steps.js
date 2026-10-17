// Print, as JSON, the spoken text of every step in an animation.html.
// The page's STEPS array literal is cut out by bracket matching and
// evaluated on its own, so none of the page's DOM code runs.
const fs = require("fs");
const vm = require("vm");

const html = fs.readFileSync(process.argv[2], "utf8");
const m = /(?:const|let|var)\s+STEPS\s*=\s*\[/.exec(html);
if (!m) { console.log("[]"); process.exit(0); }

let i = m.index + m[0].length - 1, depth = 0, quote = null;
for (; i < html.length; i++) {
  const c = html[i];
  if (quote) {
    if (c === "\\") { i++; continue; }
    if (c === quote) quote = null;
  } else if (c === '"' || c === "'" || c === "`") quote = c;
  else if (c === "[" || c === "{") depth++;
  else if (c === "]" || c === "}") { if (--depth === 0) break; }
}
const literal = html.slice(m.index + m[0].length - 1, i + 1);
// Unknown names (constants defined elsewhere in the page) read as a harmless stub.
const stub = new Proxy(function () { return stub; }, { get: (t, k) => k === Symbol.toPrimitive ? () => "" : stub });
const scope = new Proxy({}, { has: (t, k) => typeof k === "string" && !(k in globalThis),
                               get: (t, k) => k === Symbol.unscopables ? undefined : stub });
const steps = vm.runInNewContext("with (scope) { (" + literal + ") }", { scope }, { timeout: 2000 });

const KEYS = ["narration", "say", "speech", "voice", "text", "body", "caption"];
const plain = s => String(s).replace(/<[^>]*>/g, "")
  .replace(/&nbsp;/g, " ").replace(/&amp;/g, "&").replace(/&lt;/g, "<")
  .replace(/&gt;/g, ">").replace(/&quot;/g, '"').replace(/&#39;/g, "'")
  .replace(/&mdash;/g, "—").replace(/&ndash;/g, "–").replace(/&rarr;/g, "→")
  .replace(/\s+/g, " ").trim();
const key = KEYS.find(k => steps.some(s => s && typeof s[k] === "string"));
console.log(JSON.stringify(steps.map(s => (s && key && typeof s[key] === "string") ? plain(s[key]) : "")));
