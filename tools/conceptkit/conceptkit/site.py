"""Markdown to HTML twins: every .md file gets a self-contained .html beside it.

Styles and the small theme switch are inlined; images and links stay ordinary
relative links (images are never embedded). Three themes: Light, Dim, Dark.
"""

import re
from pathlib import Path

import markdown

CSS = """
:root{--bg:#ffffff;--fg:#1f2430;--muted:#5b6475;--line:#d9dde6;--code:#f3f5f9;--accent:#5b3fd1;--link:#1d5fd1}
:root[data-theme=dim]{--bg:#1e2433;--fg:#e3e8f2;--muted:#a3adc2;--line:#374057;--code:#161b27;--accent:#b8a4ff;--link:#8ec2ff}
:root[data-theme=dark]{--bg:#0b0f19;--fg:#e2e8f0;--muted:#94a3b8;--line:#263044;--code:#05070d;--accent:#a78bfa;--link:#7cc4ff}
@media (prefers-color-scheme:dark){:root:not([data-theme]){--bg:#0b0f19;--fg:#e2e8f0;--muted:#94a3b8;--line:#263044;--code:#05070d;--accent:#a78bfa;--link:#7cc4ff}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--fg);font:16px/1.65 -apple-system,"Segoe UI",Roboto,sans-serif}
main{max-width:860px;margin:0 auto;padding:24px 16px 80px}
h1,h2,h3{line-height:1.25}h1{font-size:1.9rem}h2{margin-top:2.2rem;border-bottom:1px solid var(--line);padding-bottom:.3rem}
a{color:var(--link)}img{max-width:100%;height:auto}
code{background:var(--code);padding:.1em .35em;border-radius:5px;font-size:.92em}
pre{background:var(--code);padding:14px 16px;border-radius:10px;overflow-x:auto}pre code{background:none;padding:0}
table{border-collapse:collapse;display:block;overflow-x:auto;max-width:100%}th,td{border:1px solid var(--line);padding:6px 10px;text-align:left;vertical-align:top}
blockquote{margin:1rem 0;padding:.1rem 1rem;border-left:4px solid var(--accent);color:var(--muted)}
.themes{position:fixed;top:10px;right:10px;display:flex;gap:4px;background:var(--code);border:1px solid var(--line);border-radius:8px;padding:3px}
.themes button{background:none;border:0;color:var(--fg);padding:5px 9px;border-radius:6px;cursor:pointer;font:inherit;font-size:.85rem}
.themes button[aria-pressed=true]{background:var(--accent);color:var(--bg)}
"""

JS = """
(function(){var r=document.documentElement,k="security-theme";
function set(t){if(t)r.setAttribute("data-theme",t);else r.removeAttribute("data-theme");
document.querySelectorAll(".themes button").forEach(function(b){b.setAttribute("aria-pressed",String(b.dataset.t===(t||"")))});}
var s="";try{s=localStorage.getItem(k)||""}catch(e){}
set(s);
document.querySelectorAll(".themes button").forEach(function(b){b.addEventListener("click",function(){var t=b.dataset.t;set(t);try{localStorage.setItem(k,t)}catch(e){}})});})();
"""

PAGE = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{title}</title><style>{css}</style></head>
<body><div class="themes" role="group" aria-label="Theme">
<button data-t="" aria-pressed="true">Auto</button><button data-t="light" aria-pressed="false">Light</button>
<button data-t="dim" aria-pressed="false">Dim</button><button data-t="dark" aria-pressed="false">Dark</button></div>
<main>{body}</main><script>{js}</script></body></html>
"""


def to_html(md_text):
    body = markdown.markdown(md_text, extensions=["tables", "fenced_code"])
    # a relative link to another .md file becomes a link to its .html twin
    body = re.sub(r'href="((?!https?:|#|mailto:)[^"]*?)\.md((?:#[^"]*)?)"', r'href="\1.html\2"', body)
    m = re.search(r"<h1>(.*?)</h1>", body)
    title = re.sub(r"<[^>]+>", "", m.group(1)) if m else "Security"
    return PAGE.format(title=title, css=CSS, body=body, js=JS)


def twins(project_dir):
    """Write a .html twin for the project's README.md and every docs/*.md. Returns the count."""
    root = Path(project_dir)
    files = [root / "README.md", *sorted((root / "docs").glob("*.md"))]
    for f in files:
        if f.exists():
            f.with_suffix(".html").write_text(to_html(f.read_text()))
    return len(files)


def tree(root):
    """Write a .html twin for every Markdown file under root (build and Gradle folders skipped)."""
    n = 0
    for f in sorted(Path(root).rglob("*.md")):
        if any(part in ("build", ".gradle", "node_modules") for part in f.parts):
            continue
        f.with_suffix(".html").write_text(to_html(f.read_text()))
        n += 1
    return n
