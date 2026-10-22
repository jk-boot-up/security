"""Counts built and planned projects in java/learning-path.md and writes the Status table between its markers."""
import re

START, END = "<!-- status:start -->", "<!-- status:end -->"
LEVELS = ("Beginner", "Intermediate", "Advanced")


def count(text):
    """{level: {module heading: [built, planned]}} from the table rows under each level and module heading."""
    out, level, module = {}, None, None
    for line in text.splitlines():
        m = re.match(r"## (Beginner|Intermediate|Advanced)\b", line)
        if m:
            level, module = m.group(1), None
            out[level] = {}
        elif level and (m := re.match(r"### \d+\. (.+)", line)):
            module = re.split(r"[:(]", m.group(1))[0].strip()
            out[level][module] = [0, 0]
        elif module and re.match(r"\| \d+ \|", line):
            out[level][module][1 if "*planned*" in line or "*on hold*" in line else 0] += 1
    return out


def kinds(text):
    """{level: {kind: [built, planned]}}; Plain or With tools from the Kind column, Unclassified for rows without one (advanced)."""
    out, level = {}, None
    for line in text.splitlines():
        if m := re.match(r"## (Beginner|Intermediate|Advanced)\b", line):
            level = m.group(1)
            out[level] = {"Plain": [0, 0], "With tools": [0, 0], "Unclassified": [0, 0]}
        elif level and re.match(r"\| \d+ \|", line):
            k = "Plain" if re.search(r"\| Plain \|", line) else "With tools" if "| With tools" in line else "Unclassified"
            out[level][k][1 if "*planned*" in line or "*on hold*" in line else 0] += 1
    return out


def kind_table(by):
    rows = ["| Level | Kind | Built | Planned | Total |", "| --- | --- | ---: | ---: | ---: |"]
    tot = {}
    for level in LEVELS:
        for k, (b, p) in by.get(level, {}).items():
            if b + p:
                rows.append("| %s | %s | %d | %d | %d |" % (level, k, b, p, b + p))
                t = tot.setdefault(k, [0, 0])
                t[0] += b
                t[1] += p
    for k, (b, p) in tot.items():
        rows.append("| **All** | **%s** | **%d** | **%d** | **%d** |" % (k, b, p, b + p))
    return "\n".join(rows)


def table(counts):
    rows = ["| Level | Module | Built | Planned | Total |", "| --- | --- | ---: | ---: | ---: |"]
    for level in LEVELS:
        mods = counts.get(level, {})
        b = sum(v[0] for v in mods.values())
        p = sum(v[1] for v in mods.values())
        rows.append("| **%s** | | **%d** | **%d** | **%d** |" % (level, b, p, b + p))
        for name, (mb, mp) in mods.items():
            rows.append("| | %s | %d | %d | %d |" % (name, mb, mp, mb + mp))
    b = sum(v[0] for m in counts.values() for v in m.values())
    p = sum(v[1] for m in counts.values() for v in m.values())
    rows.append("| **All** | | **%d** | **%d** | **%d** |" % (b, p, b + p))
    return "\n".join(rows), b, p


def update(path):
    """Rewrites the Status block of the learning path; returns (built, planned)."""
    text = path.read_text()
    tbl, b, p = table(count(text))
    block = "%s\n## Status\n\n**Built: %d projects. Planned: %d projects.** Counted from the tables below by `conceptkit.sh progress`.\n\n%s\n\n**By kind.** *Plain* uses the JDK only; *With tools* uses a library, framework or local tool; *Unclassified* are advanced projects not yet designed as one or the other.\n\n%s\n%s" % (START, b, p, tbl, kind_table(kinds(text)), END)
    if START in text:
        text = re.sub(re.escape(START) + r".*?" + re.escape(END), lambda _: block, text, flags=re.S)
    else:
        text = text.replace("\n## Beginner:", "\n" + block + "\n\n## Beginner:", 1)
    path.write_text(text)
    return b, p
