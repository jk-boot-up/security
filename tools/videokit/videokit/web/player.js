/* videokit narration player: a Narration on/off switch and a play/pause
   button for the current step. It watches the page's own narration text,
   works out which step is showing, and plays audio/step-N.m4a beside the
   page (rendered by tools/videokit; not embedded, so the page stays small).
   If the page already plays its own clips, only the pause button is added. */
(function () {
  "use strict";
  var STEPS = __VK_STEPS__;
  var norm = function (s) { return String(s || "").replace(/\s+/g, " ").trim(); };
  var box = document.getElementById("narration") || document.querySelector(".narration");
  if (!box || !STEPS.length) return;

  var pageOwnsAudio = !!document.getElementById("audio");
  var current = null, step = -1, on = false;
  try { on = localStorage.getItem("vk-narration") === "on"; } catch (e) {}

  // Remember whichever clip the page (or this script) last started.
  var realPlay = HTMLMediaElement.prototype.play;
  HTMLMediaElement.prototype.play = function () {
    // While the UX layer jumps across several steps, nothing speaks.
    if (window.vkQuiet) return Promise.resolve();
    if (current && current !== this) { try { current.pause(); } catch (e) {} }
    current = this;
    this.addEventListener("play", label);
    this.addEventListener("pause", label);
    this.addEventListener("ended", label);
    var p = realPlay.apply(this, arguments);
    label();
    return p;
  };

  // Lets a page's Play button wait for the step's narration to finish.
  window.vkSpeaking = function () { return !!(current && !current.paused && !current.ended); };

  var bar = document.querySelector(".controls") || box.parentNode;
  function button(id, text) {
    var b = document.createElement("button");
    b.id = id; b.type = "button"; b.textContent = text;
    bar.appendChild(b);
    return b;
  }
  var narrBtn = pageOwnsAudio ? null : button("vk-narration", "");
  var stepBtn = button("vk-step-audio", "");
  var note = document.createElement("span");
  note.id = "vk-note";
  note.style.cssText = "color:#94a3b8;font-size:.8rem;margin-left:8px";
  bar.appendChild(note);

  function stepFor(text) {
    var t = norm(text);
    for (var i = 0; i < STEPS.length; i++) {
      var s = norm(STEPS[i]);
      if (s && t.indexOf(s.slice(0, 60)) !== -1) return i;
    }
    return -1;
  }
  function stop() { if (current) { try { current.pause(); } catch (e) {} } current = null; label(); }
  function play(i) {
    if (i < 0) return;
    var a = new Audio("__VK_AUDIO_DIR__/step-" + (i + 1) + ".m4a");
    a.play().then(function () { note.textContent = ""; }, function (e) {
      note.textContent = e && e.name === "NotAllowedError"
        ? "Click Play step to hear this step."
        : "Narration audio not found: run tools/videokit/videokit.sh animation <project>.";
      label();
    });
  }
  function label() {
    if (narrBtn) narrBtn.textContent = (on ? "🔊 Narration: on" : "🔇 Narration: off");
    if (narrBtn) narrBtn.setAttribute("aria-pressed", String(on));
    var playing = current && !current.paused && !current.ended;
    var paused = current && current.paused && !current.ended;
    stepBtn.textContent = playing ? "⏸ Pause step" : paused ? "▶ Resume step" : "▶ Play step";
    stepBtn.disabled = step < 0;
  }

  if (narrBtn) narrBtn.addEventListener("click", function () {
    on = !on;
    try { localStorage.setItem("vk-narration", on ? "on" : "off"); } catch (e) {}
    if (on) { if (step >= 0) play(step); } else stop();
    label();
  });
  stepBtn.addEventListener("click", function () {
    if (current && !current.ended) {
      if (current.paused) current.play(); else current.pause();
    } else play(step);
  });

  function changed() {
    var i = stepFor(box.textContent);
    if (i === step) return;
    step = i;
    window.vkStep = i;
    if (!window.vkQuiet && !pageOwnsAudio) { stop(); if (on) play(i); }
    label();
    try { document.dispatchEvent(new CustomEvent("vk:step", { detail: i })); } catch (e) {}
  }
  // After a jump, speak the step that is now showing (if narration is on).
  window.vkReplay = function () { if (!pageOwnsAudio) { stop(); if (on) play(step); } };
  window.vkStepCount = STEPS.length;
  window.vkStepTexts = STEPS;
  new MutationObserver(changed).observe(box, { childList: true, subtree: true, characterData: true });
  changed();
  label();
})();
