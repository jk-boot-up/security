/* videokit UX layer for every animation page (injected by tools/videokit).

   It never reaches into a page's own code: it drives the page through its own
   Step and Reset buttons and reads which step is showing from the narration
   player (window.vkStep). It adds:
     - a Light / Dim / Dark theme switch, remembered per viewer;
     - a clickable timeline of the steps, a Back button and a progress bar;
     - Predict mode: before each step, the learner is asked what happens next;
     - keyboard control: arrows, space, R, N, 1-9;
     - a short glow on whatever just changed, and console lines that slide in;
     - an "All steps" list for revisiting any step.
   Page colours were turned into --vk-* variables when the page was injected,
   so the themes below remap the palette rather than filter the page. */
(function () {
  "use strict";
  var N = window.vkStepCount || 0;
  var TEXTS = window.vkStepTexts || [];
  var box = document.querySelector(".narration") || document.getElementById("narration");
  if (!box || !N) return;

  // ---------- themes ----------
  var THEMES = {
    dark: {},
    dim: {
      "0f172a": "#253044", "020617": "#1a2232", "1e293b": "#323e54", "334155": "#4b5a73",
      "475569": "#5f6f8a", "e2e8f0": "#f1f5f9", "94a3b8": "#bac6d8", "3f1d24": "#5a2f38",
      "10261f": "#24473b", "2a2110": "#4d3f22", "0c2b3d": "#1f4560", "1e1b4b": "#3b3670",
      "123a52": "#24506c", "2b0f12": "#522a2f", "06281f": "#1f4a3c", "2a1f05": "#4d3d18",
      "2c1010": "#522a2a", "2b2247": "#453a68"
    },
    light: {
      "0f172a": "#f4f6fb", "020617": "#ffffff", "1e293b": "#ffffff", "334155": "#cfd6e2",
      "475569": "#b4bfcf", "e2e8f0": "#0f172a", "94a3b8": "#475569", "f87171": "#c0262d",
      "34d399": "#047857", "fbbf24": "#a15c07", "7c3aed": "#6d28d9", "8b5cf6": "#7c3aed",
      "a78bfa": "#6d28d9", "3f1d24": "#fde8e8", "10261f": "#dcf7ea", "2a2110": "#fdf1d6",
      "0c2b3d": "#e0f2fe", "38bdf8": "#0369a1", "1e1b4b": "#ede9fe", "b9a3ff": "#6d28d9",
      "123a52": "#cdebfb", "2b0f12": "#fde8e8", "06281f": "#dcf7ea", "fca5a5": "#b91c1c",
      "0f766e": "#ccfbf1", "14b8a6": "#0f766e", "2a1f05": "#fdf1d6", "4ade80": "#15803d",
      "2c1010": "#fde8e8", "2b2247": "#ede9fe", "c4b5fd": "#6d28d9", "f472b6": "#be185d",
      "60a5fa": "#1d4ed8", "22d3ee": "#0e7490", "fde68a": "#92400e", "86efac": "#166534"
    }
  };
  var LABELS = { light: "☀ Light", dim: "◐ Dim", dark: "☾ Dark" };
  var root = document.documentElement;
  var themeCss = document.createElement("style");
  themeCss.id = "vk-theme-vars";
  document.head.appendChild(themeCss);
  function applyTheme(name) {
    var map = THEMES[name] || {};
    themeCss.textContent = ":root{" + Object.keys(map).map(function (k) {
      return "--vk-" + k + ":" + map[k];
    }).join(";") + "}";
    root.setAttribute("data-vk-theme", name);
    root.style.colorScheme = name === "light" ? "light" : "dark";
    try { localStorage.setItem("vk-theme", name); } catch (e) {}
    [].forEach.call(document.querySelectorAll("#vk-theme button"), function (b) {
      b.setAttribute("aria-pressed", String(b.dataset.theme === name));
    });
  }
  var saved = null;
  try { saved = localStorage.getItem("vk-theme"); } catch (e) {}
  var prefersLight = window.matchMedia && window.matchMedia("(prefers-color-scheme: light)").matches;
  var theme = THEMES[saved] ? saved : (prefersLight ? "light" : "dark");

  // ---------- styles for the added controls ----------
  var css = document.createElement("style");
  css.id = "vk-ux-css";
  css.textContent = [
    "#vk-top{display:flex;justify-content:flex-end;gap:8px;flex-wrap:wrap;margin:0 0 10px}",
    "#vk-theme{display:inline-flex;border:1px solid var(--vk-334155,#334155);border-radius:999px;overflow:hidden}",
    "#vk-theme button{border:0;border-radius:0;background:transparent;color:var(--vk-94a3b8,#94a3b8);padding:6px 12px;font-size:.8rem;min-height:32px}",
    "#vk-theme button[aria-pressed=true]{background:var(--vk-7c3aed,#7c3aed);color:#fff}",
    "#vk-predict{border-radius:999px;padding:6px 12px;font-size:.8rem;min-height:32px}",
    "html[data-vk-theme=light] button#play,html[data-vk-theme=light] button.primary,html[data-vk-theme=light] button.on{color:#fff!important}",
    "#vk-predict[aria-pressed=true]{background:var(--vk-7c3aed,#7c3aed);border-color:var(--vk-7c3aed,#7c3aed);color:#fff}",
    "#vk-bar{height:4px;border-radius:4px;background:var(--vk-334155,#334155);margin:10px 0 0;overflow:hidden}",
    "#vk-bar i{display:block;height:100%;width:0;background:linear-gradient(90deg,var(--vk-7c3aed,#7c3aed),var(--vk-34d399,#34d399));transition:width .35s ease}",
    "#vk-timeline{display:flex;gap:6px;flex-wrap:wrap;margin:0 0 14px}",
    "#vk-timeline button{min-width:38px;min-height:38px;padding:4px 10px;border-radius:999px;font-weight:700;font-size:.85rem;",
    "  background:var(--vk-1e293b,#1e293b);color:var(--vk-94a3b8,#94a3b8);border:1px solid var(--vk-334155,#334155)}",
    "#vk-timeline button.seen{color:var(--vk-e2e8f0,#e2e8f0);border-color:var(--vk-475569,#475569)}",
    "#vk-timeline button[aria-current=step]{background:var(--vk-7c3aed,#7c3aed);border-color:var(--vk-7c3aed,#7c3aed);color:#fff;transform:scale(1.08)}",
    "#vk-back{order:-1}",
    "#vk-keys{color:var(--vk-94a3b8,#94a3b8);font-size:.78rem;margin:8px 0 0}",
    "#vk-keys kbd{border:1px solid var(--vk-475569,#475569);border-bottom-width:2px;border-radius:4px;padding:0 5px;font:inherit;font-size:.74rem}",
    "#vk-predict-card{border:2px dashed var(--vk-a78bfa,#a78bfa);border-radius:10px;padding:12px 16px;margin:0 0 14px;",
    "  background:var(--vk-1e293b,#1e293b)}",
    "#vk-predict-card b{color:var(--vk-a78bfa,#a78bfa)}",
    "#vk-predict-card button{margin-top:10px}",
    "#vk-all{margin:16px 0 0;border:1px solid var(--vk-334155,#334155);border-radius:10px;padding:10px 14px;background:var(--vk-1e293b,#1e293b)}",
    "#vk-all summary{cursor:pointer;font-weight:700}",
    "#vk-all ol{margin:10px 0 0;padding-left:1.4em}",
    "#vk-all li{margin:6px 0;line-height:1.45}",
    "#vk-all li a{color:var(--vk-e2e8f0,#e2e8f0);text-decoration:none;cursor:pointer}",
    "#vk-all li a:hover{text-decoration:underline}",
    "#vk-all li.here a{color:var(--vk-a78bfa,#a78bfa);font-weight:700}",
    "button:focus-visible,#vk-all a:focus-visible{outline:3px solid var(--vk-a78bfa,#a78bfa);outline-offset:2px}",
    ".vk-flash{animation:vkFlash 1s ease-out}",
    "@keyframes vkFlash{0%{box-shadow:0 0 0 0 rgba(167,139,250,.9)}100%{box-shadow:0 0 0 14px rgba(167,139,250,0)}}",
    ".vk-fade{animation:vkFade .45s ease-out}",
    "@keyframes vkFade{from{opacity:.15;transform:translateY(4px)}to{opacity:1;transform:none}}",
    ".console span,.console .ln,.console div{animation:vkIn .35s ease-out both}",
    ".console span:nth-child(2){animation-delay:.08s}.console span:nth-child(3){animation-delay:.16s}",
    ".console span:nth-child(4){animation-delay:.24s}.console span:nth-child(5){animation-delay:.32s}",
    ".console span:nth-child(6){animation-delay:.4s}.console span:nth-child(n+7){animation-delay:.48s}",
    "@keyframes vkIn{from{opacity:0;transform:translateX(-6px)}to{opacity:1;transform:none}}",
    "@media (max-width:600px){#vk-keys{display:none}#vk-timeline button{min-width:40px;min-height:40px}}",
    // Phone widths: no sideways scrolling, whatever the page's own grid says.
    "body{overflow-wrap:anywhere}",
    "@media (max-width:760px){",
    "  body{padding-left:14px!important;padding-right:14px!important}",
    "  .wrap,.stage,.boxes,.controls,.col-main,.col-client{max-width:100%!important;min-width:0!important}",
    "  .stage,.cols,.grid,[class*=grid]{grid-template-columns:minmax(0,1fr)!important}",
    "  .stage>*,.boxes>*,.row,.box,.family,.lane,.col-main>*,.col-client>*{min-width:0!important;max-width:100%}",
    "  [style*=grid-template-columns]{grid-template-columns:minmax(0,1fr)!important}",
    "  .row,.box,.name,.job,.role,.nm,.val,.ms,.note,.tag{white-space:normal!important}",
    "  svg{max-width:100%;height:auto}",
    "}",
    "@media (prefers-reduced-motion:reduce){.vk-flash,.vk-fade,.console span,.console .ln,.console div{animation:none!important}#vk-bar i{transition:none}}"
  ].join("\n");
  document.head.appendChild(css);

  // ---------- the page's own buttons ----------
  function find(id, re) {
    var b = document.getElementById(id);
    if (b) return b;
    return [].filter.call(document.querySelectorAll("button"), function (x) {
      return !/^vk-/.test(x.id) && re.test(x.textContent);
    })[0] || null;
  }
  var nextBtn = find("step", /^\s*(step|next)\b/i);
  var resetBtn = find("reset", /reset|restart|start over/i);
  var playBtn = find("play", /^\s*play\b|pause/i);
  if (!nextBtn || !resetBtn) return;

  // ---------- step titles ----------
  function titleOf(k) {
    var own = (typeof STEPS !== "undefined" && STEPS[k]) || null;   // eslint-disable-line no-undef
    var t = own && (own.label || own.title || own.name || own.heading);
    if (t) return String(t).replace(/<[^>]*>/g, "");
    var first = String(TEXTS[k] || "").split(/(?<=[.!?])\s/)[0];
    return first.length > 90 ? first.slice(0, 87) + "…" : first;
  }
  function current() { return typeof window.vkStep === "number" ? window.vkStep : -1; }

  // ---------- jumping ----------
  function jump(k) {
    k = Math.max(-1, Math.min(N - 1, k));
    var cur = current();
    if (k === cur) return;
    if (k === cur + 1) { nextBtn.click(); return; }
    window.vkQuiet = true;
    try {
      resetBtn.click();
      for (var j = 0; j <= k; j++) nextBtn.click();
    } finally {
      window.vkQuiet = false;
    }
  }

  // ---------- controls ----------
  var wrap = box.parentNode;
  var top = document.createElement("div");
  top.id = "vk-top";
  var themeBox = document.createElement("div");
  themeBox.id = "vk-theme";
  themeBox.setAttribute("role", "group");
  themeBox.setAttribute("aria-label", "Colour theme");
  ["light", "dim", "dark"].forEach(function (name) {
    var b = document.createElement("button");
    b.type = "button"; b.dataset.theme = name; b.textContent = LABELS[name];
    b.addEventListener("click", function () { applyTheme(name); });
    themeBox.appendChild(b);
  });
  var predictBtn = document.createElement("button");
  predictBtn.type = "button"; predictBtn.id = "vk-predict";
  predictBtn.textContent = "🤔 Predict first";
  predictBtn.title = "Before each step, stop and guess what will happen";
  var predict = false;
  try { predict = localStorage.getItem("vk-predict") === "on"; } catch (e) {}
  top.appendChild(predictBtn);
  top.appendChild(themeBox);
  var h1 = document.querySelector("h1");
  if (h1) h1.parentNode.insertBefore(top, h1);
  else document.body.insertBefore(top, document.body.firstChild);

  var timeline = document.createElement("div");
  timeline.id = "vk-timeline";
  timeline.setAttribute("aria-label", "Steps");
  var chips = [];
  for (var k = 0; k < N; k++) (function (k) {
    var b = document.createElement("button");
    b.type = "button";
    b.textContent = String(k + 1);
    b.title = "Step " + (k + 1) + ": " + titleOf(k);
    b.setAttribute("aria-label", b.title);
    b.addEventListener("click", function () { hidePredict(); jump(k); });
    timeline.appendChild(b);
    chips.push(b);
  })(k);
  var controlsRow = nextBtn.parentNode;
  var anchor = (box.compareDocumentPosition(controlsRow) & Node.DOCUMENT_POSITION_FOLLOWING) ? box : controlsRow;
  anchor.parentNode.insertBefore(timeline, anchor);

  var bar = document.createElement("div");
  bar.id = "vk-bar";
  bar.innerHTML = "<i></i>";
  box.appendChild(bar);

  var back = document.createElement("button");
  back.type = "button"; back.id = "vk-back"; back.textContent = "◂ Back";
  back.addEventListener("click", function () { hidePredict(); jump(current() - 1); });
  nextBtn.parentNode.insertBefore(back, nextBtn);

  var keys = document.createElement("p");
  keys.id = "vk-keys";
  keys.innerHTML = "Keys: <kbd>→</kbd> next · <kbd>←</kbd> back · <kbd>Space</kbd> play/pause · " +
    "<kbd>1</kbd>–<kbd>9</kbd> jump · <kbd>R</kbd> reset · <kbd>N</kbd> narration · <kbd>P</kbd> predict";
  var controls = nextBtn.parentNode;
  controls.parentNode.insertBefore(keys, controls.nextSibling);

  var all = document.createElement("details");
  all.id = "vk-all";
  var list = "<summary>All steps — revisit any of them</summary><ol>";
  for (var q = 0; q < N; q++) list += '<li><a tabindex="0" data-k="' + q + '"></a></li>';
  all.innerHTML = list + "</ol>";
  [].forEach.call(all.querySelectorAll("a"), function (a) {
    var k = +a.dataset.k;
    a.textContent = titleOf(k);
    a.addEventListener("click", function () { hidePredict(); jump(k); });
    a.addEventListener("keydown", function (e) { if (e.key === "Enter") { hidePredict(); jump(k); } });
  });
  var main = document.querySelector(".wrap") || (h1 && h1.parentNode) || document.body;
  main.appendChild(all);

  // ---------- predict mode ----------
  var card = null, passOnce = false;
  function hidePredict() { if (card) { card.remove(); card = null; } }
  function showPredict() {
    var next = current() + 1;
    if (next >= N) return false;
    card = document.createElement("div");
    card.id = "vk-predict-card";
    card.setAttribute("role", "status");
    card.innerHTML = "<b>Predict first.</b> Step " + (next + 1) + " is: <span></span><br>" +
      "What do you expect to happen? Say it out loud, then reveal.";
    card.querySelector("span").textContent = titleOf(next);
    var reveal = document.createElement("button");
    reveal.type = "button"; reveal.textContent = "Reveal ▸";
    reveal.addEventListener("click", function () { hidePredict(); passOnce = true; nextBtn.click(); });
    card.appendChild(document.createElement("br"));
    card.appendChild(reveal);
    box.parentNode.insertBefore(card, box);
    reveal.focus({ preventScroll: true });
    return true;
  }
  function setPredict(v) {
    predict = v;
    predictBtn.setAttribute("aria-pressed", String(v));
    try { localStorage.setItem("vk-predict", v ? "on" : "off"); } catch (e) {}
    if (!v) hidePredict();
  }
  predictBtn.addEventListener("click", function () { setPredict(!predict); });
  // In predict mode, the page's own Step button asks before it reveals.
  nextBtn.addEventListener("click", function (e) {
    if (passOnce) { passOnce = false; return; }
    if (!predict || window.vkQuiet || card) return;
    if (showPredict()) { e.stopImmediatePropagation(); e.preventDefault(); }
  }, true);

  // ---------- keyboard ----------
  document.addEventListener("keydown", function (e) {
    if (e.altKey || e.ctrlKey || e.metaKey) return;
    var t = e.target;
    if (t && (t.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(t.tagName))) return;
    var k = e.key;
    if (k === "ArrowRight") { e.preventDefault(); if (card) { hidePredict(); passOnce = true; } nextBtn.click(); }
    else if (k === "ArrowLeft") { e.preventDefault(); hidePredict(); jump(current() - 1); }
    else if (k === " " && playBtn && !(t && t.tagName === "BUTTON")) { e.preventDefault(); playBtn.click(); }
    else if (k === "r" || k === "R" || k === "Home") { hidePredict(); resetBtn.click(); }
    else if (k === "n" || k === "N") { var nb = document.getElementById("vk-narration") || document.getElementById("audio"); if (nb) nb.click(); }
    else if (k === "p" || k === "P") setPredict(!predict);
    else if (/^[1-9]$/.test(k) && +k <= N) { hidePredict(); jump(+k - 1); }
  });

  // ---------- show what changed ----------
  var stage = document.querySelector(".stage") || document.body;
  new MutationObserver(function (muts) {
    muts.forEach(function (m) {
      var el = m.target;
      if (m.type !== "attributes" || !el.classList || /^vk-/.test(el.id || "")) return;
      var was = String(m.oldValue || "").replace(/\bvk-\S+/g, "").trim();
      var now = String(el.className || "").replace(/\bvk-\S+/g, "").trim();
      if (was === now || /\bdim\b/.test(now)) return;
      el.classList.remove("vk-flash");
      void el.offsetWidth;
      el.classList.add("vk-flash");
      setTimeout(function () { el.classList.remove("vk-flash"); }, 1000);
    });
  }).observe(stage, { subtree: true, attributes: true, attributeFilter: ["class"], attributeOldValue: true });

  var textEl = document.getElementById("narration") || box;
  function refresh() {
    var cur = current();
    chips.forEach(function (c, k) {
      if (k === cur) c.setAttribute("aria-current", "step"); else c.removeAttribute("aria-current");
      if (k <= cur) c.classList.add("seen");
    });
    bar.firstChild.style.width = (N ? ((cur + 1) / N) * 100 : 0) + "%";
    back.disabled = cur < 0;
    [].forEach.call(all.querySelectorAll("li"), function (li, k) { li.classList.toggle("here", k === cur); });
    textEl.classList.remove("vk-fade");
    void textEl.offsetWidth;
    if (cur >= 0) textEl.classList.add("vk-fade");
  }
  document.addEventListener("vk:step", refresh);

  applyTheme(theme);
  setPredict(predict);
  refresh();
})();
