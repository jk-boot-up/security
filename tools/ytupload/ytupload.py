"""Upload the security videos to YouTube through YouTube Studio, following the
YouTube section of docs/beginner-plan.md.

YouTube Studio is driven in a real Chrome window with its own profile, started
by `chrome` below. The channel owner signs in to that window by hand once; the
session is kept in the profile for later uploads. This script never types a
password. It attaches to the running window over Chrome's debugging port.

    ytupload.sh chrome                 start the dedicated Chrome window
    ytupload.sh sheet   <project-dir>  print what would be uploaded
    ytupload.sh upload  <project-dir>  attach the video and fill every field,
                                       then stop on the Visibility page
    ytupload.sh resume  <project-dir>  carry on filling the open upload after a
                                       failed step, without re-attaching the video
    ytupload.sh publish               choose Public and publish the open upload
    ytupload.sh batch   <targets>     upload every project not yet on YouTube,
                                       in learning order (e.g. `batch beginner`)

A video is never published without its custom thumbnail: when YouTube refuses
the thumbnail, that video stays a draft and the batch stops for the day.

The data API is not used: videos uploaded through an unaudited API project are
locked to Private, and these videos are published as Public.
"""
import os
import re
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
JAVA = os.path.join(REPO, "java")
CHANNEL = "UCc6PxT_gmoPZj_r1q1mwx7Q"
PORT = 9222
PROFILE = os.path.expanduser("~/.ytupload-chrome")
SHOTS = os.path.join(PROFILE, "screenshots", "security")
THUMBNAIL = "thumbnail-security-photo-cyan-theme.png"

# Two playlists per module (docs/beginner-plan.md, section 8): the plain-Java videos,
# and the versions that add a framework or tool, never mixed.
PLAIN_SUFFIX = " - Without Tools"
FRAMEWORK_SUFFIX = " - With Tools"
PLAYLISTS = {
    "foundations": "Security Foundations in Java",
    "identity": "Identity Basics in Java",
    "passwords-and-mfa": "Passwords and MFA in Java",
    "cryptography": "Cryptography Basics in Java",
    "networking": "Networking for Security in Java",
    "transport-security": "TLS and Certificates in Java",
    "safe-habits": "Safe Habits in Java",
    # intermediate
    "web-security": "Web Security in Java",
    "secure-configuration": "Secure Configuration in Java",
    "api-security": "API Security in Java",
    "identity-and-access": "Identity and Access in Java",
    "tls-in-practice": "TLS in Practice in Java",
    "secure-coding": "Secure Coding in Java",
    "secrets-and-supply-chain": "Secrets and Supply Chain Security in Java",
    "platform-hardening": "Platform Hardening in Java",
    "detection-and-response": "Detection and Response in Java",
    "secure-design": "Secure Design in Java",
    # advanced
    "cloud-security-without-a-cloud": "Cloud Security Without a Cloud in Java",
    "infrastructure-as-code-security": "Infrastructure as Code Security in Java",
    "kubernetes-and-container-security": "Kubernetes and Container Security in Java",
    "devsecops-pipelines": "DevSecOps Pipelines in Java",
    "service-identity-and-zero-trust": "Service Identity and Zero Trust in Java",
    "applied-cryptography-and-pki": "Applied Cryptography and PKI in Java",
    "detection-engineering": "Detection Engineering in Java",
    "offensive-security-methodology": "Offensive Security Methodology in Java",
    "resilience-and-recovery": "Resilience and Recovery in Java",
    "governance-risk-and-compliance": "Governance, Risk and Compliance in Java",
}

# Learning order comes from java/learning-path.md, which numbers every project in the
# order a learner should take them (a with-tools version straight after its plain one).
# Videos are uploaded in that order, so each playlist lists them in that order too.
LEARNING_PATH = os.path.join(JAVA, "learning-path.md")


def learning_order():
    """{project path relative to java/: its number in the learning path}."""
    order = {}
    for line in open(LEARNING_PATH):
        m = re.match(r"\|\s*(\d+)\s*\|\s*\[[^\]]+\]\(([^)]+)\)", line)
        if m:
            order.setdefault(m.group(2).strip("/"), len(order))
    return order


def learning_key(rel):
    """Sort key: the project's place in the learning path; unlisted projects last."""
    return (learning_order().get(rel, 10 ** 6), rel)


# ---------------------------------------------------------------- the sheet

def section(md, heading):
    m = re.search(r"^## %s\n(.*?)(?=^## |\Z)" % re.escape(heading), md, re.S | re.M)
    if not m:
        sys.exit("docs/youtube.md has no %s section" % heading)
    return m.group(1).strip()


def title_of(md):
    return section(md, "Title").strip("`\n ")


def duration(video):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                          "-of", "csv=p=0", video], capture_output=True, text=True).stdout
    return float(out.strip() or 0)


def seconds(mmss):
    parts = [int(x) for x in mmss.split(":")]
    return sum(v * 60 ** i for i, v in enumerate(reversed(parts)))


def sheet(project):
    project = os.path.abspath(project)
    slug = os.path.basename(project)
    module = os.path.basename(os.path.dirname(project))
    video = os.path.join(project, "video", slug + "-explained.mp4")
    srt = video[:-4] + ".srt"
    thumb = os.path.join(project, "docs", THUMBNAIL)
    for f in (video, srt, thumb):
        if not os.path.exists(f):
            sys.exit("missing: " + f)

    md = open(os.path.join(project, "docs", "youtube.md")).read()
    title = title_of(md)
    body = section(md, "Description")
    m = re.search(r"Chapters:\s*```\n(.*?)\n```", body, re.S)
    if not m:
        sys.exit("the description has no chapters block")
    chapters = [ln.strip() for ln in m.group(1).splitlines() if ln.strip()]
    intro = body[:m.start()].strip()
    desc = intro + "\n\nCHAPTERS\n" + "\n".join(chapters)
    desc = re.sub(r"https?://", "", desc)
    tags = [t.strip() for t in section(md, "Tags").split(",") if t.strip()]

    # Checks before anything is sent.
    if not chapters[0].startswith("00:00 ") or len(chapters) < 3:
        sys.exit("chapters must start at 00:00 and number at least three")
    length = duration(video)
    if seconds(chapters[-1].split()[0]) >= length:
        sys.exit("the last chapter starts after the video ends: chapters are stale")
    for field in (title, desc):
        if "<" in field or ">" in field:
            sys.exit("YouTube rejects < and > in the title or description")
    if len(title) > 100 or len(desc) > 5000 or len(", ".join(tags)) > 500:
        sys.exit("title, description or tags too long for YouTube")
    from_png = open(thumb, "rb").read(24)
    if from_png[16:24] != (1280).to_bytes(4, "big") + (720).to_bytes(4, "big"):
        sys.exit("the thumbnail is not 1280x720")
    return {"video": video, "srt": srt, "thumbnail": thumb, "title": title,
            "description": desc, "tags": tags, "playlist": playlist(module, slug)}


def learning_kinds():
    """{project path relative to java/: True if the learning path marks it With tools}."""
    kinds = {}
    for line in open(LEARNING_PATH):
        m = re.match(r"\|\s*\d+\s*\|\s*\[[^\]]+\]\(([^)]+)\)\s*\|\s*([^|]+)\|", line)
        if m:
            kinds[m.group(1).strip("/")] = m.group(2).strip().lower().startswith("with tools")
    return kinds


def is_framework(module, slug):
    """True for a version that adds a framework or a tool, as the learning path marks it;
    a <concept>-with-<tool> slug counts too, for a project the path does not list."""
    for rel, with_tools in learning_kinds().items():
        if rel.split("/")[1:] == [module, slug]:
            return with_tools
    return "-with-" in slug


def playlist(module, slug):
    """Plain-Java and with-tools videos go to separate playlists, never mixed."""
    name = PLAYLISTS[module]
    return name + (FRAMEWORK_SUFFIX if is_framework(module, slug) else PLAIN_SUFFIX)


# ---------------------------------------------------------------- the browser

def chrome():
    os.makedirs(PROFILE, exist_ok=True)
    subprocess.Popen(["open", "-na", "Google Chrome", "--args",
                      "--user-data-dir=" + PROFILE,
                      "--remote-debugging-port=%d" % PORT,
                      "--no-first-run",
                      "https://studio.youtube.com/channel/%s/videos" % CHANNEL])
    print("Chrome started with the upload profile. Sign in to YouTube there if asked.")


def attach():
    from playwright.sync_api import sync_playwright
    pw = sync_playwright().start()
    try:
        browser = pw.chromium.connect_over_cdp("http://127.0.0.1:%d" % PORT)
    except Exception:
        pw.stop()
        sys.exit("no upload Chrome on port %d: run `ytupload.sh chrome` first" % PORT)
    ctx = browser.contexts[0]
    page = next((p for p in ctx.pages if "studio.youtube.com" in p.url), None)
    page = page or ctx.new_page()
    page.bring_to_front()
    return pw, page


def shot(page, name):
    os.makedirs(SHOTS, exist_ok=True)
    path = os.path.join(SHOTS, name + ".png")
    page.screenshot(path=path)
    return path


def set_text(page, selector, text):
    box = page.locator(selector).first
    box.click()
    page.keyboard.press("Meta+A")
    page.keyboard.press("Backspace")
    box.fill(text)


def button_box(page, text):
    """Screen position of the visible <button> labelled exactly `text`, or None."""
    return page.evaluate("""t => { const b = [...document.querySelectorAll('button')]
        .find(b => b.textContent.trim() === t && b.getBoundingClientRect().width > 0);
        if (!b) return null; const r = b.getBoundingClientRect();
        return {x: r.x + r.width / 2, y: r.y + r.height / 2}; }""", text)


def press_button(page, text):
    """Real mouse click on the visible button labelled `text`. Studio's
    buttons answer a genuine pointer press; a script-level click() on the
    element is ignored, and Playwright's own click refuses because a
    transparent layer covers the playlist list."""
    box = button_box(page, text)
    if not box:
        raise RuntimeError("no visible button labelled " + text)
    page.mouse.click(box["x"], box["y"])
    time.sleep(1)


def playlist_state(page, name):
    """Where the playlist's row is in the open list and whether it is ticked,
    or None if the list has no such playlist."""
    return page.evaluate("""n => {
        const el = [...document.querySelectorAll('*')].find(e =>
            e.childElementCount === 0 && e.textContent.trim() === n
            && e.getBoundingClientRect().width > 0
            && e.closest('li, label') !== null && e.closest('[contenteditable]') === null);
        if (!el) return null;
        el.scrollIntoView({block: 'center'});        // the list outgrows the dialog
        let p = el, box = null;
        for (let i = 0; i < 8 && p && !box; i++, p = p.parentElement) {
            const all = p.querySelectorAll('[aria-checked]');
            if (all.length > 1) break;               // reached the whole list
            box = all[0] || null;
        }
        const r = el.getBoundingClientRect();
        return {x: r.x + r.width / 2, y: r.y + r.height / 2,
                checked: !!box && box.getAttribute('aria-checked') === 'true'}; }""", name)


def choose_playlist(page, name, untick=()):
    """Tick the category's playlist, creating it first if the channel lacks it.

    The list is found by what it shows (the playlist names, the New playlist
    and Done buttons) rather than by Studio's element names, which change.
    """
    new = page.get_by_role("button", name=re.compile(r"^New playlist", re.I)) \
        .filter(visible=True)
    if not button_box(page, "Done"):                 # the list may already be open
        page.locator("ytcp-video-metadata-playlists ytcp-dropdown-trigger, "
                     "#playlists-container ytcp-dropdown-trigger").first.click()
        page.wait_for_function("() => [...document.querySelectorAll('button')].some("
                               "b => b.textContent.trim() === 'Done' "
                               "&& b.getBoundingClientRect().width > 0)", timeout=15000)
    # The list fills in after it opens: wait for its entries before deciding
    # that the playlist is missing.
    for _ in range(30):
        if playlist_state(page, name) is not None:
            break
        time.sleep(0.5)
    if playlist_state(page, name) is None:
        create_playlist(page, name)
    state = playlist_state(page, name)
    if not state["checked"]:
        page.mouse.click(state["x"], state["y"])
        time.sleep(1)
        if not playlist_state(page, name)["checked"]:
            raise RuntimeError("could not tick " + name)
    for old in untick:                               # moving a video out of a playlist
        state = playlist_state(page, old)
        if state and state["checked"]:
            page.mouse.click(state["x"], state["y"])
            time.sleep(1)
    press_button(page, "Done")


def create_playlist(page, name):
    """Create a Public playlist from the upload form's New playlist menu."""
    press_button(page, "New playlist")
    item = page.get_by_role("menuitem", name=re.compile("New playlist", re.I))
    if item.count():
        item.first.click()
    # The title box is a plain editable area: click its placeholder and type.
    page.wait_for_function("() => document.body.innerText.includes('Create a new playlist')",
                           timeout=15000)
    pos = page.evaluate("""() => { const el = [...document.querySelectorAll(
        '[contenteditable][aria-label="Add title"]')].find(e => e.getBoundingClientRect().width > 0);
        if (!el) return null; const r = el.getBoundingClientRect();
        return {x: r.x + r.width / 2, y: r.y + r.height / 2}; }""")
    if not pos:
        raise RuntimeError("no title box in the new-playlist form")
    page.mouse.click(pos["x"], pos["y"])
    page.keyboard.type(name)
    time.sleep(1)
    if not page.evaluate("() => document.body.innerText.includes('Public')"):
        raise RuntimeError("new playlist is not Public by default; stopped")
    pos = page.evaluate("""() => { const bs = [...document.querySelectorAll('button')]
        .filter(b => b.textContent.trim() === 'Create' && b.getBoundingClientRect().width > 0
                     && !b.closest('ytcp-topbar-logo, #header, ytcp-header'));
        const b = bs[bs.length - 1]; if (!b) return null; const r = b.getBoundingClientRect();
        return {x: r.x + r.width / 2, y: r.y + r.height / 2}; }""")
    if not pos:
        raise RuntimeError("no Create button in the new-playlist form")
    page.mouse.click(pos["x"], pos["y"])
    page.wait_for_function("() => !document.body.innerText.includes('Create a new playlist')",
                           timeout=20000)
    for _ in range(30):
        time.sleep(0.5)
        if playlist_state(page, name) is not None:
            print("created playlist:", name)
            return
    raise RuntimeError("new playlist did not appear in the list")


def choose_radio(page, pattern):
    """Tick the radio button whose label matches `pattern`, with a real mouse
    click on the label, and confirm it is ticked."""
    js = """src => { const re = new RegExp(src, 'i');
        const el = [...document.querySelectorAll('*')].find(e =>
            e.childElementCount === 0 && re.test(e.textContent.trim())
            && e.getBoundingClientRect().width > 0);
        if (!el) return null;
        const radio = el.closest('[role=radio], tp-yt-paper-radio-button');
        const r = el.getBoundingClientRect();
        el.scrollIntoView({block: 'center'});
        const q = el.getBoundingClientRect();
        return {x: q.x + q.width / 2, y: q.y + q.height / 2,
                checked: !!radio && (radio.getAttribute('aria-checked') === 'true'
                                     || radio.hasAttribute('checked'))}; }"""
    state = page.evaluate(js, pattern)
    if state is None:
        raise RuntimeError("no option matching " + pattern)
    if not state["checked"]:
        page.mouse.click(state["x"], state["y"])
        time.sleep(1)
        if not page.evaluate(js, pattern)["checked"]:
            raise RuntimeError("could not tick " + pattern)


def tick(page, pattern):
    """Tick the checkbox whose label matches `pattern` with a real mouse click on
    the box itself, and confirm it is ticked."""
    js = """src => { const re = new RegExp(src, 'i');
        const el = [...document.querySelectorAll('*')].find(e =>
            e.childElementCount === 0 && re.test(e.textContent.trim())
            && e.getBoundingClientRect().width > 0);
        if (!el) return null;
        let p = el, box = null;
        for (let i = 0; i < 6 && p && !box; i++, p = p.parentElement) {
            const all = p.querySelectorAll('[aria-checked]');
            if (all.length > 1) break;
            box = all[0] || null;
        }
        const target = box || el;
        target.scrollIntoView({block: 'center'});
        const r = target.getBoundingClientRect();
        return {x: r.x + r.width / 2, y: r.y + r.height / 2,
                checked: !!box && box.getAttribute('aria-checked') === 'true'}; }"""
    state = page.evaluate(js, pattern)
    if state is None:
        raise RuntimeError("no checkbox labelled " + pattern)
    if not state["checked"]:
        page.mouse.click(state["x"], state["y"])
        time.sleep(1)
        if not page.evaluate(js, pattern)["checked"]:
            raise RuntimeError("could not tick " + pattern)


def dropdown(page, trigger, option, warn):
    """Pick an option from a Studio dropdown and read the field back.

    The language list is long and only builds its entries as it scrolls, so the
    option is found from inside the page, scrolled into view and clicked with the
    mouse. Never press Escape to recover: Escape closes the whole upload dialog.
    On failure the list is closed by clicking an empty part of the dialog."""
    box = page.locator(trigger).first
    for attempt in range(3):
        try:
            pick(page, box, option)
            print("set", option)
            return
        except Exception as e:
            last = e
            close_lists(page)
    warn.append("could not set %s (%s)" % (option, str(last).splitlines()[0]))
    shot(page, "dropdown-" + option)


def pick(page, box, option):
    """One attempt at choosing `option` in the dropdown `box`."""
    if True:
        box.scroll_into_view_if_needed(timeout=5000)
        page.mouse.move(5, 5)                        # clear any tag tooltip first
        time.sleep(0.5)
        b = box.bounding_box()
        page.mouse.click(b["x"] + b["width"] / 2, b["y"] + b["height"] / 2)
        time.sleep(1)
        pos = None
        for _ in range(40):                          # scroll the list until it appears
            pos = page.evaluate("""o => {
                // The entries are plain text elements, not marked as options:
                // find the one reading exactly `o` outside the field itself.
                const hit = [...document.querySelectorAll('yt-formatted-string, [role=option], tp-yt-paper-item')]
                    .find(e => e.textContent.trim() === o && e.getBoundingClientRect().width > 0
                               && !e.closest('ytcp-dropdown-trigger'));
                if (!hit) return null;
                hit.scrollIntoView({block: 'center'});
                const r = hit.getBoundingClientRect();
                return {x: r.x + r.width / 2, y: r.y + r.height / 2}; }""", option)
            if pos:
                break
            time.sleep(0.3)
        if not pos:
            raise RuntimeError("option %s not in the list" % option)
        time.sleep(0.5)
        page.mouse.click(pos["x"], pos["y"])
        time.sleep(1)
        shown = box.inner_text(timeout=5000)
        if option not in shown:
            raise RuntimeError("field shows %r" % shown)


def close_lists(page):
    """Close an open dropdown list by clicking the right end of the dialog's
    title, which lists never cover. Escape would close the whole dialog."""
    try:
        b = page.locator("ytcp-uploads-dialog #dialog-title, #dialog-title").first.bounding_box()
        page.mouse.click(b["x"] + b["width"] - 5, b["y"] + b["height"] / 2)
        time.sleep(1)
    except Exception:
        pass


def step(page, name, fn):
    """Run one form step; on failure save a screenshot and stop with its name."""
    try:
        fn()
    except Exception as e:
        path = shot(page, "failed-" + name)
        sys.exit("step '%s' failed (%s). Screenshot: %s" % (name, str(e).splitlines()[0], path))


# Chrome on the debug port counts as "not co-located" for Playwright, which refuses files over 50 MB.
# Chrome runs on this machine, so hand it the file's path through the DevTools protocol instead.
FIND_FILE_INPUT = """(() => {
  const walk = (root) => {
    const hit = root.querySelector('input[type=file]');
    if (hit) return hit;
    for (const el of root.querySelectorAll('*')) {
      if (el.shadowRoot) { const deep = walk(el.shadowRoot); if (deep) return deep; }
    }
    return null;
  };
  return walk(document);
})()"""


def attach_file(page, path, timeout=60):
    page.locator("input[type=file]").first.wait_for(state="attached", timeout=timeout * 1000)
    cdp = page.context.new_cdp_session(page)
    try:
        found = cdp.send("Runtime.evaluate", {"expression": FIND_FILE_INPUT})
        oid = found.get("result", {}).get("objectId")
        if not oid:
            raise RuntimeError("no file input on the page")
        cdp.send("DOM.setFileInputFiles", {"files": [os.path.abspath(path)], "objectId": oid})
    finally:
        cdp.detach()


def upload(project):
    s = sheet(project)
    pw, page = attach()
    page.goto("https://studio.youtube.com/channel/%s/videos/upload?d=ud" % CHANNEL)
    if "accounts.google.com" in page.url:
        pw.stop()
        sys.exit("not signed in: sign in to YouTube in the upload Chrome, then run again")
    step(page, "attach video", lambda: attach_file(page, s["video"]))
    step(page, "details page", lambda: page.locator("#title-textarea #textbox").first
         .wait_for(timeout=120000))
    print("video attached:", os.path.basename(s["video"]))
    report_fill(pw, page, fill(pw, page, s))


def report_fill(pw, page, warn):
    print("filled; stopped on the Visibility page:", shot(page, "before-publish"))
    for w in warn:
        print("WARNING:", w)
    pw.stop()


def resume(project):
    """Carry on filling the upload dialog that is already open, without
    attaching the video again (which would make a duplicate upload)."""
    s = sheet(project)
    pw, page = attach()
    if page.locator("#title-textarea #textbox").first.is_visible():
        # Start from a fresh page: a list left open by a failed step can block
        # the form. Wait first, because leaving mid-upload cancels the upload.
        step(page, "wait for upload", lambda: wait_uploaded(page))
    if True:
        # Reopen the draft from Channel content with its Edit draft button.
        row = draft_row(page, s["title"])
        step(page, "find draft", lambda: row.wait_for(timeout=30000))
        step(page, "edit draft", lambda: row.get_by_role(
            "button", name=re.compile("Edit draft", re.I)).first.click())
        step(page, "details page", lambda: page.locator("#title-textarea #textbox")
             .first.wait_for(timeout=60000))
        print("reopened the draft")
    try:                                             # close a list left open
        press_button(page, "Done")
    except RuntimeError:
        pass
    close_lists(page)
    report_fill(pw, page, fill(pw, page, s))


THUMB_LIMIT = os.path.join(PROFILE, "thumbnail-limit")


def thumbnail_limited():
    """True for 2 hours after YouTube said the daily custom-thumbnail limit was hit.

    The limit does not last a fixed 24 hours (one hit at 08:51 had cleared by
    midnight), so a batch tries again every 2 hours. That is safe: its first step
    re-tries the thumbnail that was refused, and if YouTube still refuses it the
    limit is recorded afresh and the batch stops before uploading anything."""
    try:
        return time.time() - float(open(THUMB_LIMIT).read()) < 2 * 3600
    except Exception:
        return False


def fill(pw, page, s):
    warn = []
    step(page, "title", lambda: set_text(page, "#title-textarea #textbox", s["title"]))
    step(page, "description",
         lambda: set_text(page, "#description-textarea #textbox", s["description"]))
    def thumbnail():
        """Attach the custom thumbnail, or stop: no video goes out without it."""
        if thumbnail_limited():
            raise RuntimeError("thumbnail refused: YouTube's daily custom-thumbnail limit")
        page.locator("input#file-loader").first.set_input_files(s["thumbnail"], timeout=20000)
        time.sleep(3)
        text = page.evaluate("() => document.body.innerText")
        if re.search(r"custom thumbnail limit|thumbnail.{0,40}(limit|not allowed|verify)", text, re.I):
            try:
                press_button(page, "Close")
            except RuntimeError:
                pass
            open(THUMB_LIMIT, "w").write(str(time.time()))
            raise RuntimeError("thumbnail refused: YouTube's daily custom-thumbnail limit")
    step(page, "thumbnail", thumbnail)
    step(page, "playlist", lambda: choose_playlist(page, s["playlist"]))
    step(page, "not made for kids", lambda: page.locator(
        'tp-yt-paper-radio-button[name="VIDEO_MADE_FOR_KIDS_NOT_MFK"]').first.click())
    step(page, "show more", lambda: page.locator("#toggle-button")
         .filter(has_text=re.compile("Show more", re.I)).first.click())
    altered = page.locator('tp-yt-paper-radio-button[name="VIDEO_HAS_ALTERED_CONTENT_NO"]')
    if altered.count():
        altered.first.click()
    else:
        warn.append("altered-content question not found; set it to No by hand")
    step(page, "no paid promotion", lambda: choose_radio(
        page, r"^No, my video doesn.t include paid promotion"))
    shot(page, "details-more")

    def tags():
        box = page.locator("#tags-container input, input[aria-label='Tags']").first
        box.click()
        box.fill(",".join(s["tags"]) + ",")
    step(page, "tags", tags)
    for trigger, option in (("#language-input ytcp-dropdown-trigger", "English"),
                            ("#category ytcp-dropdown-trigger", "Science & Technology")):
        missed = []
        dropdown(page, trigger, option, missed)
        if missed:                                   # required: stop, leave a draft
            step(page, option, lambda: (_ for _ in ()).throw(RuntimeError(missed[0])))

    def to_visibility():
        for _ in range(3):                            # Video elements, Checks, Visibility
            page.locator("#next-button").first.click()
            time.sleep(2)
        page.locator('tp-yt-paper-radio-button[name="PUBLIC"]').first.wait_for(timeout=30000)
    step(page, "go to visibility", to_visibility)
    return warn


def published_link(page):
    """The youtu.be address shown in the "Video published" dialog, or None."""
    return page.evaluate("""() => { const m = document.body.innerText
        .match(/https:\\/\\/youtu\\.be\\/[\\w-]+/); return m ? m[0] : null; }""")


def close_published(page):
    """Close the "Video published" dialog with its Close button."""
    try:
        press_button(page, "Close")
    except RuntimeError:
        pass


def wait_uploaded(page, minutes=45):
    """Wait until the file has finished uploading: leaving or saving the page
    while it is still uploading would cancel the upload."""
    deadline = time.time() + minutes * 60
    while time.time() < deadline:
        text = page.evaluate("() => document.body.innerText")
        if re.search(r"upload failed|processing abandoned", text, re.I):
            raise RuntimeError("YouTube reports the upload failed")
        if not re.search(r"Uploading\b", text):
            return
        time.sleep(10)
    raise RuntimeError("upload still running after %d minutes" % minutes)


def wait_checked(page, minutes=20):
    """Wait for YouTube's own checks (copyright and the like) to finish."""
    deadline = time.time() + minutes * 60
    while time.time() < deadline:
        text = page.evaluate("() => document.body.innerText")
        if not re.search(r"Checking\b|still checking", text):
            return
        time.sleep(15)


def do_publish(page, warn, visibility="PUBLIC"):
    """Public as an instant Premiere (or Unlisted, for a copy still to be checked),
    then Save. Returns the video's link. Premiere can only be chosen before the
    video goes public, never afterwards."""
    if button_box(page, "Publish anyway"):          # left open by an earlier attempt
        press_button(page, "Go back")
    step(page, visibility.lower(), lambda: page.locator(
        'tp-yt-paper-radio-button[name="%s"]' % visibility).first.click())
    if visibility == "PUBLIC":
        step(page, "instant premiere", lambda: tick(page, r"^Set as instant Premiere"))
    link = published_link(page)                      # shown before saving, too
    step(page, "wait for upload", lambda: wait_uploaded(page))
    step(page, "wait for checks", lambda: wait_checked(page))
    step(page, "publish", lambda: page.locator("#done-button").first.click())
    for _ in range(40):                              # "We're still checking your content"
        time.sleep(3)
        if not button_box(page, "Publish anyway"):
            break
        press_button(page, "Go back")
        wait_checked(page, minutes=0.5)
        page.locator("#done-button").first.click()
    else:
        step(page, "publish anyway", lambda: press_button(page, "Publish anyway"))   # after 20 minutes of checks
    step(page, "saved", lambda: page.wait_for_function(
        """() => /Video published|Premiere/.test([...document.querySelectorAll('#dialog-title')]
                 .map(e => e.getBoundingClientRect().width ? e.textContent : '').join(' '))
              || [...document.querySelectorAll('button')].some(b => b.textContent.trim() === 'Close'
                 && b.getBoundingClientRect().width > 0)""", timeout=300000))
    time.sleep(2)
    link = published_link(page) or link
    close_published(page)
    return link


def publish():
    pw, page = attach()
    warn = []
    print("published:", do_publish(page, warn))
    for w in warn:
        print("WARNING:", w)
    pw.stop()

# ---------------------------------------------------------------- the registry

import json
import datetime

REGISTRY = os.path.join(REPO, "docs", "youtube-links.json")


def load_registry():
    if os.path.exists(REGISTRY):
        return json.load(open(REGISTRY))
    return {}


def save_registry(reg):
    with open(REGISTRY, "w") as f:
        json.dump(dict(sorted(reg.items())), f, indent=1, ensure_ascii=False)
        f.write("\n")


def all_projects():
    """{project path relative to java/, like beginner/foundations/cia-triad: its YouTube
    title}, every project with a YouTube sheet, in learning order."""
    out = {}
    for level in sorted(os.listdir(JAVA)):
        ldir = os.path.join(JAVA, level)
        if not os.path.isdir(ldir):
            continue
        for module in sorted(os.listdir(ldir)):
            if module not in PLAYLISTS:
                continue
            for slug in sorted(os.listdir(os.path.join(ldir, module))):
                yt = os.path.join(ldir, module, slug, "docs", "youtube.md")
                if os.path.exists(yt):
                    try:
                        out["%s/%s/%s" % (level, module, slug)] = title_of(open(yt).read())
                    except SystemExit:
                        pass
    return dict(sorted(out.items(), key=lambda kv: learning_key(kv[0])))


def channel_videos(page):
    """[(title, video id, visibility)] for every video on the channel, read page by
    page from Studio's Channel content list."""
    page.goto("https://studio.youtube.com/channel/%s/videos/upload" % CHANNEL)
    page.locator("ytcp-video-row").first.wait_for(timeout=60000)
    seen, rows = set(), []
    while True:
        time.sleep(2)
        batch = page.evaluate("""() => [...document.querySelectorAll('ytcp-video-row')]
            .map(r => { const a = r.querySelector('a#video-title, #video-title');
                const href = (r.querySelector('a[href*="/video/"]') || {}).getAttribute
                    ? r.querySelector('a[href*="/video/"]').getAttribute('href') : '';
                const vis = (r.querySelector('.tablecell-visibility, #visibility-cell')
                             || {innerText: ''}).innerText.trim();
                return [a ? a.textContent.trim() : '', href || '', vis]; })""")
        new = 0
        for title, href, vis in batch:
            m = re.search(r"/video/([\w-]+)/", href)
            if m and m.group(1) not in seen:
                seen.add(m.group(1))
                rows.append((title, m.group(1), vis.split("\n")[0]))
                new += 1
        nxt = page.locator("#navigate-after").first
        if new == 0 or nxt.count() == 0 or nxt.get_attribute("aria-disabled") == "true" \
                or nxt.get_attribute("disabled") is not None:
            return rows
        first = next((h for _, h, _ in batch if h), "")
        nxt.click()
        for _ in range(60):                          # wait for the next page to replace this one
            time.sleep(0.5)
            now = page.evaluate("() => { const r = document.querySelector('ytcp-video-row a[href*=\"/video/\"]');"
                                " return r ? r.getAttribute('href') : ''; }")
            if now and now != first:
                break


def video_exists(vid):
    """True if YouTube still has the video, False if it was deleted, None if the
    check could not tell (network trouble). Uses the public oEmbed address."""
    import urllib.request
    import urllib.error
    url = "https://www.youtube.com/oembed?format=json&url=https://www.youtube.com/watch?v=" + vid
    try:
        urllib.request.urlopen(url, timeout=20).read()
        return True
    except urllib.error.HTTPError as e:
        if e.code in (401, 403):                     # exists, but private or restricted
            return True
        if e.code in (400, 404):
            return False
        return None
    except Exception:
        return None


def verify():
    """Drop registry entries whose video has been deleted, so the next batch
    uploads those projects again."""
    reg = load_registry()
    gone = [rel for rel, e in reg.items() if e.get("id") and video_exists(e["id"]) is False]
    for rel in gone:
        del reg[rel]
    save_registry(reg)
    if gone:
        regenerate()
    print("checked %d; deleted on YouTube, to upload again: %d" % (len(reg) + len(gone), len(gone)))
    for rel in gone:
        print("  " + rel)
    return gone


def sync():
    """Match every video on the channel to its project by title, and record it."""
    pw, page = attach()
    videos = channel_videos(page)
    pw.stop()
    by_title = {}
    for rel, title in all_projects().items():
        by_title.setdefault(title, []).append(rel)
    reg, unmatched = load_registry(), []
    for title, vid, vis in videos:
        rels = by_title.get(title)
        if not rels:
            unmatched.append("%s (%s, %s)" % (title, vid, vis))
            continue
        for rel in rels:
            entry = reg.get(rel, {})
            entry.update({"title": title, "id": vid, "visibility": vis,
                          "module": rel.split("/")[1]})
            entry.setdefault("recorded", datetime.date.today().isoformat())
            reg[rel] = entry
    save_registry(reg)
    print("channel videos: %d; matched to security projects: %d; registry: %d"
          % (len(videos), len(videos) - len(unmatched), len(reg)))


# ---------------------------------------------------------------- batches

LOG = os.path.join(PROFILE, "security-batch.log")


class DailyLimit(Exception):
    pass


#: the link of the upload in progress, so a failed one is recorded as a draft
DRAFT = {"link": None}
STATUS = os.path.join(PROFILE, "security-status.txt")


def status(text):
    """One-line progress summary, overwritten after every project."""
    with open(STATUS, "w") as f:
        f.write(time.strftime("%Y-%m-%d %H:%M ") + text + "\n")


def expand(targets):
    """Levels (beginner), modules (cryptography) or project paths -> project paths
    relative to java/, in learning order."""
    known = all_projects()
    out = []
    for target in targets:
        name = os.path.relpath(os.path.abspath(target), JAVA) if os.path.sep in target else target
        if name in known:
            out.append(name)
        else:
            out += [rel for rel in known if name in rel.split("/")[:2] or name == rel.split("/")[2]]
    return sorted(dict.fromkeys(out), key=learning_key)


def regenerate():
    """Refresh docs/youtube.md and .html, the upload and review progress page."""
    py = os.path.expanduser("~/.cache/videokit/venv/bin/python")   # has the markdown package
    subprocess.run([py if os.path.exists(py) else sys.executable, os.path.join(HERE, "make_progress.py")],
                   stdout=subprocess.DEVNULL, check=False)


def add_thumbnails(page=None):
    """Attach docs/thumbnail.png to published videos that went up without one
    (YouTube's daily custom-thumbnail limit). Stops at the limit."""
    if thumbnail_limited():
        return 0
    own = page is None
    if own:
        pw, page = attach()
    added = 0
    try:
        reg = load_registry()
        for rel, e in reg.items():
            if e.get("thumbnail") is not False or e.get("visibility") != "Public" or not e.get("id"):
                continue
            page.goto("https://studio.youtube.com/video/%s/edit" % e["id"])
            page.locator("#title-textarea #textbox").first.wait_for(timeout=60000)
            page.locator("input#file-loader").first.set_input_files(
                os.path.join(JAVA, rel, "docs", THUMBNAIL), timeout=20000)
            time.sleep(3)
            if "custom thumbnail limit" in page.evaluate("() => document.body.innerText"):
                press_button(page, "Close")
                open(THUMB_LIMIT, "w").write(str(time.time()))
                break
            press_button(page, "Save")
            time.sleep(3)
            reg = load_registry()
            reg[rel].pop("thumbnail", None)
            if "thumbnail" in reg[rel].get("problem", ""):
                reg[rel].pop("problem")
            save_registry(reg)
            added += 1
            log("%s %s: thumbnail added" % (time.strftime("%m-%d %H:%M"), rel))
    finally:
        if own:
            pw.stop()
    return added


def draft_row(page, title):
    """The Channel content row of the draft with exactly this title. A title can
    be the start of another one (DNS Basics, DNS Basics with CoreDNS ...), so the
    row must match it whole and still have its Edit draft button."""
    page.goto("https://studio.youtube.com/channel/%s/videos/upload" % CHANNEL)
    row = (page.locator("ytcp-video-row")
           .filter(has=page.locator("#video-title", has_text=re.compile(
               "^\\s*%s\\s*$" % re.escape(title))))
           .filter(has=page.get_by_role("button", name=re.compile("Edit draft", re.I)))
           .first)
    try:
        row.wait_for(timeout=15000)
    except Exception:                                # not on the first page: filter by title
        page.locator("input[placeholder='Filter']").first.click()
        page.locator("tp-yt-paper-item, [role=option], [role=menuitem]").filter(
            has_text=re.compile("^\\s*Title\\s*$")).first.click()
        dialog = page.locator("tp-yt-paper-dialog:visible").last
        dialog.locator("input").first.fill(title)
        dialog.get_by_role("button", name="Apply").click()
    return row


def finish_draft(page, rel):
    """Reopen a recorded draft from Channel content, fill and publish it."""
    s = sheet(os.path.join(JAVA, rel))
    row = draft_row(page, s["title"])
    step(page, "find draft", lambda: row.wait_for(timeout=30000))
    step(page, "edit draft", lambda: row.get_by_role(
        "button", name=re.compile("Edit draft", re.I)).first.click())
    step(page, "details page", lambda: page.locator("#title-textarea #textbox")
         .first.wait_for(timeout=60000))
    DRAFT["link"] = published_link(page)
    warn = fill(None, page, s)
    return do_publish(page, warn), warn


def upload_one(page, rel, visibility="PUBLIC"):
    """Upload, fill and publish one project. Returns (link, warnings)."""
    s = sheet(os.path.join(JAVA, rel))
    page.goto("https://studio.youtube.com/channel/%s/videos/upload?d=ud" % CHANNEL)
    if "accounts.google.com" in page.url:
        raise SystemExit("not signed in: sign in to YouTube in the upload Chrome")
    step(page, "attach video", lambda: attach_file(page, s["video"]))
    for _ in range(60):                              # the form, or the daily limit
        text = page.evaluate("() => document.body.innerText")
        if re.search(r"daily upload limit", text, re.I):
            raise DailyLimit()
        if page.locator("#title-textarea #textbox").first.is_visible():
            break
        time.sleep(2)
    else:
        step(page, "details page", lambda: (_ for _ in ()).throw(
            RuntimeError("upload form did not open")))
    DRAFT["link"] = published_link(page)
    warn = fill(None, page, s)
    return do_publish(page, warn, visibility), warn


def set_visibility(page, vid, visibility):
    """Change a video's visibility (PUBLIC, UNLISTED, PRIVATE) on its edit page."""
    page.goto("https://studio.youtube.com/video/%s/edit" % vid)
    step(page, "edit page", lambda: page.locator("ytcp-video-metadata-visibility")
         .first.wait_for(timeout=60000))
    step(page, "open visibility", lambda: page.locator("ytcp-video-metadata-visibility")
         .first.click())
    step(page, visibility.lower(), lambda: page.locator(
        'tp-yt-paper-radio-button[name="%s"]' % visibility).first.click(timeout=30000))
    step(page, "done", lambda: press_button(page, "Done"))
    time.sleep(1)
    step(page, "save", lambda: page.locator("ytcp-button#save").first.click())
    want = {"PUBLIC": "Public", "UNLISTED": "Unlisted", "PRIVATE": "Private"}[visibility]
    for _ in range(60):
        time.sleep(1)
        shown = page.evaluate("() => (document.querySelector('#visibility-text') || {}).innerText || ''")
        saved = page.evaluate("() => { const b = document.querySelector('ytcp-button#save');"
                              " return !b || b.hasAttribute('disabled') || b.getAttribute('aria-disabled') === 'true'; }")
        if want in shown and saved:
            return
    raise SystemExit("could not confirm %s is %s; check Studio" % (vid, want))


def delete_video(page, vid):
    """Delete one video forever, from its row in Channel content, and confirm it is gone."""
    page.goto("https://studio.youtube.com/channel/%s/videos/upload" % CHANNEL)
    page.locator("ytcp-video-row").first.wait_for(timeout=60000)
    row = page.locator("ytcp-video-row").filter(
        has=page.locator('a[href*="/video/%s/"]' % vid)).first
    step(page, "find row", lambda: row.wait_for(timeout=30000))
    row.hover()
    time.sleep(1)
    step(page, "options", lambda: row.locator("[aria-label='Options']").first.click())
    time.sleep(1)
    step(page, "delete forever", lambda: page.locator(
        "tp-yt-paper-item, [role=menuitem], [role=option]").filter(
        has_text=re.compile(r"^\s*Delete forever\s*$")).first.click())
    time.sleep(2)
    dialog = page.locator("ytcp-dialog:visible, tp-yt-paper-dialog:visible").last
    step(page, "confirm box", lambda: dialog.locator(
        "ytcp-checkbox-lit, tp-yt-paper-checkbox, [role=checkbox]").first.click())
    time.sleep(1)
    step(page, "confirm", lambda: press_button(page, "Delete forever"))
    for _ in range(36):
        time.sleep(5)
        if video_exists(vid) is False:
            return
    raise SystemExit("asked YouTube to delete %s, but it still answers; check Studio" % vid)


def batch(targets, wait_hours=6):
    """Upload every project in `targets` not yet on YouTube, one at a time.

    A failed project is recorded as a draft (with its video ID when the file got
    attached), so it is never uploaded twice; finish it with `resume` + `publish`.
    When YouTube's daily limit is reached the batch waits and carries on.
    Progress: one line per project in batch.log, a summary in status.txt."""
    todo = expand(targets)
    done = failed = 0
    streak = [0]                                     # failures in a row

    def remaining():
        reg = load_registry()
        return [rel for rel in todo if rel not in reg or reg[rel].get("visibility") == "Held"
                or (reg[rel].get("visibility") == "Draft" and reg[rel].get("tries", 0) < 2)]

    verify()
    status("starting: %d projects, %d to upload" % (len(todo), len(remaining())))
    while remaining():
        pw, page = attach()
        limited = False
        try:
            try:
                add_thumbnails(page)
            except Exception as e:
                log("add-thumbnails stopped: %s" % str(e).splitlines()[0])
            if thumbnail_limited():                  # a video without its thumbnail looks unfinished
                status("STOPPED: YouTube's daily custom-thumbnail limit; run again tomorrow")
                return
            for rel in remaining():
                stamp = time.strftime("%m-%d %H:%M")
                DRAFT["link"] = None
                entry = load_registry().get(rel, {})
                try:
                    if entry.get("visibility") == "Draft" and entry.get("id"):
                        link, warn = finish_draft(page, rel)
                    else:
                        link, warn = upload_one(page, rel)
                except DailyLimit:
                    limited = True
                    break
                except SystemExit as e:
                    failed += 1
                    reason = str(e.code).split(" Screenshot:")[0]
                    try:
                        DRAFT["link"] = DRAFT["link"] or published_link(page)
                    except Exception:
                        pass
                    if DRAFT["link"]:                  # let the file finish, so the draft is whole
                        try:
                            wait_uploaded(page)
                        except Exception:
                            pass
                    vid = re.search(r"youtu\.be/([\w-]+)", DRAFT["link"] or "")
                    if vid or "attach video" not in reason:
                        reg = load_registry()          # a draft now exists: never re-upload
                        tries = reg.get(rel, {}).get("tries", 0) + 1
                        reg[rel] = {"title": sheet(os.path.join(JAVA, rel))["title"],
                                    "id": vid.group(1) if vid else entry.get("id", ""),
                                    "visibility": "Draft", "tries": tries,
                                    "module": rel.split("/")[1], "problem": reason,
                                    "recorded": datetime.date.today().isoformat()}
                        save_registry(reg)
                    if thumbnail_limited():            # the owner's rule: no thumbnail, no uploads
                        reg = load_registry()
                        if rel in reg:
                            reg[rel]["tries"] = max(0, reg[rel].get("tries", 1) - 1)
                            save_registry(reg)
                        log("%s %s: left as a draft, thumbnail refused; stopping" % (stamp, rel))
                        status("STOPPED: YouTube refused the custom thumbnail; %d uploaded. "
                               "Run the batch again later to finish." % done)
                        return
                    log("%s %s: FAILED, %s" % (stamp, rel, reason))
                    streak[0] += 1
                    if streak[0] >= 3:                   # something is wrong with every video
                        status("STOPPED after 3 failures in a row; last: %s: %s" % (rel, reason))
                        return
                    if "not signed in" in reason:
                        status("STOPPED: not signed in to YouTube in the upload Chrome")
                        return
                    status("running: %d uploaded, %d failed, %d to go; last failed: %s"
                           % (done, failed, len(remaining()), rel))
                    continue
                streak[0] = 0
                vid = re.search(r"youtu\.be/([\w-]+)", link or "")
                reg = load_registry()
                reg[rel] = {"title": sheet(os.path.join(JAVA, rel))["title"],
                            "id": vid.group(1) if vid else "",
                            "visibility": "Public" if vid else "unknown",
                            "module": rel.split("/")[1],
                            "recorded": datetime.date.today().isoformat()}
                if warn:
                    reg[rel]["problem"] = "; ".join(warn)
                if any("thumbnail" in w for w in warn):
                    reg[rel]["thumbnail"] = False
                save_registry(reg)
                regenerate()
                done += 1
                log("%s %s: %s%s" % (stamp, rel, link,
                                      "  (" + "; ".join(warn) + ")" if warn else ""))
                status("running: %d uploaded, %d failed, %d to go; last: %s"
                       % (done, failed, len(remaining()), rel))
        finally:
            pw.stop()
        if not limited:
            break
        status("waiting %dh for YouTube's daily upload limit: %d uploaded, %d failed, %d to go"
               % (wait_hours, done, failed, len(remaining())))
        log("%s daily upload limit reached; waiting %dh" % (time.strftime("%m-%d %H:%M"), wait_hours))
        time.sleep(wait_hours * 3600)
        streak[0] = 0                                # a fresh day: start counting again
        verify()
    regenerate()
    status("FINISHED: %d uploaded, %d failed, %d left" % (done, failed, len(remaining())))
    print("batch finished: %d uploaded, %d failed; see %s" % (done, failed, LOG))


def log(line):
    print(line, flush=True)
    with open(LOG, "a") as f:
        f.write(line + "\n")

def main():
    cmd, args = (sys.argv[1] if len(sys.argv) > 1 else "help"), sys.argv[2:]
    if cmd == "chrome":
        chrome()
    elif cmd == "sheet" and args:
        for k, v in sheet(args[0]).items():
            print("== %s\n%s\n" % (k, ", ".join(v) if isinstance(v, list) else v))
    elif cmd == "upload" and args:
        upload(args[0])
    elif cmd == "resume" and args:
        resume(args[0])
    elif cmd == "publish":
        publish()
    elif cmd in ("fix", "no-paid-promotion") and args:   # fix a published video
        pw, page = attach()
        page.goto("https://studio.youtube.com/video/%s/edit" % args[0])
        step(page, "edit page", lambda: page.locator("#title-textarea #textbox")
             .first.wait_for(timeout=60000))
        step(page, "show more", lambda: page.locator("#toggle-button")
             .filter(has_text=re.compile("Show more", re.I)).first.click())
        step(page, "no paid promotion", lambda: choose_radio(
            page, r"^No, my video doesn.t include paid promotion"))
        for trigger, option in (("#language-input ytcp-dropdown-trigger", "English"),
                                ("#category ytcp-dropdown-trigger", "Science & Technology")):
            missed = []
            dropdown(page, trigger, option, missed)
            for m in missed:
                print("WARNING:", m)
        if button_box(page, "Save"):
            step(page, "save", lambda: press_button(page, "Save"))
        time.sleep(3)
        print("saved:", shot(page, "no-paid-promotion"))
        pw.stop()
    elif cmd == "batch" and args:
        batch(args)
    elif cmd == "move-playlist" and len(args) == 3:  # <video id> <to playlist> <from playlist>
        pw, page = attach()
        page.goto("https://studio.youtube.com/video/%s/edit" % args[0])
        step(page, "edit page", lambda: page.locator("#title-textarea #textbox")
             .first.wait_for(timeout=60000))
        step(page, "playlist", lambda: choose_playlist(page, args[1], untick=[args[2]]))
        if button_box(page, "Save"):
            step(page, "save", lambda: press_button(page, "Save"))
        time.sleep(3)
        print("moved:", shot(page, "move-playlist"))
        pw.stop()
    elif cmd == "state":                             # what the upload window shows
        pw, page = attach()
        print(page.evaluate("""() => ({
            title: [...document.querySelectorAll('#dialog-title, #title-textarea #textbox')]
                   .filter(e => e.getBoundingClientRect().width).map(e => e.innerText.trim()),
            link: (document.body.innerText.match(/https:\\/\\/youtu\\.be\\/[\\w-]+/) || [null])[0],
            status: (document.body.innerText.match(/(Uploading|Processing|Checking|Checks|Upload)[^\\n]{0,60}/) || [''])[0]
        })"""))
        pw.stop()
    elif cmd == "add-thumbnails":
        print("thumbnails added:", add_thumbnails())
    elif cmd == "verify":
        verify()
    elif cmd == "sync":
        sync()
    elif cmd == "close":                             # close a leftover dialog
        pw, page = attach()
        print("link:", published_link(page))
        close_published(page)
        pw.stop()
    elif cmd == "probe" and args:                    # debugging: where is <text>?
        pw, page = attach()
        print(page.evaluate("""t => [...document.querySelectorAll('*')]
            .filter(e => e.childElementCount === 0 && e.textContent.trim() === t)
            .map(e => { const r = e.getBoundingClientRect();
                let p = e, chain = [];
                for (let i = 0; i < 4 && p; i++, p = p.parentElement)
                    chain.push(p.tagName.toLowerCase() + (p.id ? '#' + p.id : '')
                               + (p.getAttribute('role') ? '[' + p.getAttribute('role') + ']' : ''));
                return chain.join(' < ') + '  @' + Math.round(r.x) + ',' + Math.round(r.y)
                       + ' ' + Math.round(r.width) + 'x' + Math.round(r.height); })""", args[0]))
        pw.stop()
    else:
        print(__doc__)


if __name__ == "__main__":
    main()
