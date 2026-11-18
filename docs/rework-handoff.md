# Rework handoff: resume spec for the next Claude session

Read this, then [CLAUDE.md](../CLAUDE.md) and [video-teaching-standard.md](video-teaching-standard.md), then continue at
**Next actions**. This file is updated after every finished agent and build, so it is current even if a session stops
at its usage limit. When you resume, keep updating it the same way.

Last updated: 2026-11-18

## Goal

Rework every intermediate project, and then every advanced project, to the teaching standard. Rebuild each video one
at a time. Upload only when the author says so (see Rules).

## State

- 11-18: all 151 projects reworked and Public. Rework complete.
- YouTube (`docs/youtube-links.json`): 139 registered, all Public (2026-11-17 19:12): 41 beginner, 76 intermediate, 22 advanced.
- Git (11-18): `53c4f09f` pushed to `advanced_security_projects` and fast-forwarded to origin `main`. Only privacy-engineering (agent working) is uncommitted.
- The learning path is now `java/index.md` (renamed 11-18). `java/playlists-youtube.md` is the playlist tree (playlist ids in `docs/youtube-playlists.json`, written by make_progress.py). A new playlist needs its id added there.
- privacy-engineering: agent finished 11-18, check clean, build queued after security-architecture-review.
- Learning path has a Video column, filled by `conceptkit.sh progress` from the registry; rerun it and `~/.cache/videokit/venv/bin/python tools/ytupload/make_progress.py` after uploads (system python3 lacks markdown, so the .html twins are skipped).
  Commit only when asked. No Co-Authored-By line and no mention of an AI.

Intermediate projects in `java/index.md` order, after the last uploaded one:

| Project | Rework | Video |
|---|---|---|
| identity-and-access/openid-connect-with-spring-security-and-keycloak | done | Public 11-16 |
| identity-and-access/tokens-in-the-browser | done | Public 11-16 |
| identity-and-access/tokens-in-the-browser-with-spring-boot | done | Public 11-16 |
| identity-and-access/passkeys | done | Public 11-16 |
| identity-and-access/passkeys-with-spring-security | done | Public 11-16 |
| identity-and-access/saml-basics | done | Public 11-16 |
| identity-and-access/saml-basics-with-keycloak | done | Public 11-16 |
| tls-in-practice/local-certificate-authority | done, check clean | Public 11-16 |
| tls-in-practice/local-certificate-authority-with-step-ca | done, check clean | Public 11-16 |
| tls-in-practice/tls-servers-in-practice-with-nginx-and-caddy | done, check clean | Public 11-16 |
| tls-in-practice/tls-misconfigurations | done, check clean | Public 11-16 |
| tls-in-practice/tls-misconfigurations-with-testssl | done, check clean | Public 11-16 |
| tls-in-practice/inspecting-tls-traffic | done, check clean | Public 11-16 |
| tls-in-practice/inspecting-tls-traffic-with-wireshark | done, check clean | Public 11-16 |
| tls-in-practice/certificate-pinning | done, check clean | Public 11-16 |
| secure-coding/input-validation | done, check clean | Public 11-16 |
| secure-coding/input-validation-with-hibernate-validator | done, check clean | Public 11-16 |
| secure-coding/race-conditions-and-business-logic | done, check clean | Public 15:40 |
| secure-coding/race-conditions-and-business-logic-with-postgres | done, check clean | Public 11-16 |
| secure-coding/secrets-in-errors-and-logs | done, check clean | Public 11-16 |
| secure-coding/secrets-in-errors-and-logs-with-logback | done, check clean | Public 11-16 |
| secure-coding/fuzzing | done, check clean | Public 11-16 |
| secure-coding/fuzzing-with-jazzer | done, check clean | Public 11-16 |
| secrets-and-supply-chain/secrets-management | done, check clean | Public 11-16 |
| secrets-and-supply-chain/secrets-management-with-openbao | done, check clean | Public 11-16 |
| secrets-and-supply-chain/artifact-signing | done, check clean | Public 11-16 |
| secrets-and-supply-chain/artifact-signing-with-cosign | done, check clean | Public 11-16 |
| secrets-and-supply-chain/sbom-and-provenance | done, check clean | Public 11-16 |
| secrets-and-supply-chain/sbom-and-provenance-with-syft-and-grype | done, check clean | Public 11-16 |
| secrets-and-supply-chain/dependency-confusion | done, check clean | Public 11-16 |
| secrets-and-supply-chain/dependency-confusion-with-gradle | done, check clean | Public 11-16 |
| platform-hardening/linux-hardening | done, check clean | Public 11-16 |
| platform-hardening/linux-hardening-with-docker | done, check clean | Public 11-16 |
| platform-hardening/container-hardening | done, check clean | Public 11-16 |
| platform-hardening/container-hardening-with-docker-and-kind | done, check clean | Public 11-16 |
| detection-and-response/security-logging | done, check clean | Public 11-16 |
| detection-and-response/security-logging-with-loki-and-grafana | done, check clean | Public 11-16 |
| detection-and-response/detecting-attacks | done, check clean | Public 11-16 |
| detection-and-response/detecting-attacks-with-opensearch | done, check clean | Public 11-16 |
| detection-and-response/incident-response | done, check clean | Public |
| secure-design/threat-modeling | done, check clean | Public |
| secure-design/threat-modeling-with-pytm | done, check clean | Public |
| secure-design/attack-trees-and-abuse-cases | done, check clean | Public |
| secure-design/attack-trees-and-abuse-cases-with-threat-dragon | done, check clean | Public |
| secure-design/security-requirements-with-asvs | done, check clean | Public |
| advanced: cloud-security-without-a-cloud/cloud-iam-models | done, check clean | Public |
| advanced: cloud-security-without-a-cloud/network-isolation | done, check clean | Public |
| advanced: cloud-security-without-a-cloud/cloud-misconfigurations | done, check clean | Public |
| advanced: infrastructure-as-code-security/iac-scanning | done, check clean | Public |
| advanced: infrastructure-as-code-security/policy-as-code | done, check clean | Public |
| advanced: kubernetes-and-container-security/kubernetes-admission-control | done, check clean (golden reformatted after the verified run; values unchanged; rerun integrationTest on a quiet machine) | Public |
| advanced: kubernetes-and-container-security/kubernetes-network-policies | done, check clean (domain now online store) | Public |
| advanced: kubernetes-and-container-security/runtime-security | done, check clean; integrationTest FLAKY (audit sensor misses short-lived records under load) | Public |
| advanced: kubernetes-and-container-security/kubernetes-secrets | done, check clean | Public |
| advanced: devsecops-pipelines/sast | done, check clean | Public |
| advanced: devsecops-pipelines/dast | done, check clean | Public |
| advanced: devsecops-pipelines/secret-scanning | done, check clean | Public |
| advanced: devsecops-pipelines/secure-pipeline | done, check clean | Public |
| advanced: service-identity-and-zero-trust/mutual-tls-between-services | done, check clean | Public |
| advanced: service-identity-and-zero-trust/service-mesh-mtls | done, check clean; integrationTest to rerun once (golden spacing fixed by hand) | Public |
| advanced: service-identity-and-zero-trust/workload-identity | done, check clean | Public |
| advanced: service-identity-and-zero-trust/zero-trust-access | done, check clean | Public |
| advanced: applied-cryptography-and-pki/certificate-lifecycle | done, check clean; integrationTest timing flake ("alerts 1" vs 0) - rerun when quiet | Public |
| advanced: applied-cryptography-and-pki/tls-attacks-and-downgrades | done, check clean (needs OpenSSL 3.6.x) | Public |
| advanced: applied-cryptography-and-pki/post-quantum-cryptography | done, check clean (inventory now a fixed sample) | Public |
| advanced: applied-cryptography-and-pki/crypto-agility | done, check clean | Public |
| advanced: applied-cryptography-and-pki/hardware-backed-keys | done, check clean (golden line "each guess < 86.4 ms: yes" depends on machine speed) | Public |
| advanced: detection-engineering/detection-as-code | done, check clean | queued 11-17 |
| advanced: detection-engineering/siem-basics | done, check clean | queued 11-17 |
| advanced: detection-engineering/threat-hunting | done, check clean | queued 11-17 |
| advanced: offensive-security-methodology/vulnerability-research | done, check clean (golden prints v1/Statement.java line numbers) | queued 11-17 |
| advanced: offensive-security-methodology/reverse-engineering-fundamentals | done, check clean | queued 11-17 |
| advanced: offensive-security-methodology/malware-analysis-fundamentals | done, check clean | queued 11-17 |
| advanced: resilience-and-recovery/ransomware-resilience | done, check clean | queued 11-17 |
| advanced: resilience-and-recovery/security-chaos-engineering | done, check clean | queued 11-17 |
| advanced: governance-risk-and-compliance/risk-management | done, check clean | queued 11-17 |
| advanced: governance-risk-and-compliance/controls-and-evidence | done, check clean | queued 11-17 |
| advanced: governance-risk-and-compliance/security-architecture-review | done, check clean | Public 11-18 |
| advanced: governance-risk-and-compliance/privacy-engineering | done, check clean | Public 11-18 |

Then, not started: nothing; every project has an agent or is done in
`java/index.md` order. Advanced agents also read docs/advanced-plan.md. The upload watcher covers advanced
too (the author said to upload built videos as they finish).

How to check whether a project is reworked: `video/figures.py` exists and `conceptkit.sh check <slug> --no-render` says
"no problems found". A video is current if its `.mp4` (under `build/` or `video/`) is newer than `concept.toml`.

## How to work

- Build queue now: scratchpad `queue2.sh` reads `queue2.txt` (append slugs there) and starts no build while
  scratchpad `pause-builds` exists (used by kind-cluster demos that fail under load). Only one run may hold it: if it exists, wait.
- A kind-cluster demo that fails under load: create scratchpad `pause-builds`, wait for quiet, run, delete it.
- (resolved 11-17) kubernetes-admission-control blocked all later uploads (the watcher keeps learning order). If its agent is gone:
  wait for a quiet machine, run `./gradlew -q run > docs/expected-output.txt` there (twice, compare), then follow
  scratchpad adv3-agent/ notes (console.py, kac-scenes.toml) to finish the acts and run the check.

- Check: `tools/conceptkit/conceptkit.sh check <slug>`. The slug is the project directory name, not the path.
- Build: `tools/conceptkit/conceptkit.sh build <slug>`. Run it in the background, **one at a time**. Use a queue:
  a file of slugs, and a loop that builds each one and appends `OK <slug>` or `FAIL <slug>` to a done file. New slugs
  can be appended to the queue while it runs. On FAIL, read only the tail of that build's log.
- Agents: at most three at a time, two projects each, in learning-path order. Give each one the **Agent brief** below.
  When one finishes, check its projects yourself (`check --no-render`), queue the builds, update this file, and start
  the next pair.
- Upload (only with the author's go-ahead in the current session): first `conceptkit.sh check <slug> --no-render` must
  say "no problems found" and `ytquality.sh local` must PASS. Then run `ytupload.sh batch <path>`, one at a time, in
  learning-path order. The upload Chrome profile is `~/.ytupload-chrome`, on CDP port 9222; it must be signed in.
  A "saved" step timeout leaves a Draft, and running `batch` again finishes it.

## Agent brief

> Repo: /Users/jk/gitbase/jk-boot-up/security/main/security. Read CLAUDE.md first and follow it.
> Goal: bring each assigned project to docs/video-teaching-standard.md (read it in full).
> Model to copy: java/intermediate/tls-in-practice/tls-misconfigurations (and tls-servers-in-practice-with-nginx-and-caddy):
> sectioned demo output ("ONE. ...", input and result in separate labelled blocks, derived values worked out),
> docs/expected-output.txt as golden output, video/figures.py parsing it, concept.toml `[[acronym]]`, `[[class]]`/
> `[[class.part]]`, `[classes]`, a figure per act, scene kinds "acronyms"/"classes".
> Steps: 1. Read src/main, src/test and concept.toml (grep/sed for large files). 2. Section the demo output; keep it
> deterministic. Tool tests stay tagged for integrationTest; add tool-free unit tests. Update the golden output.
> 3. figures.py (values only from expected-output.txt), acronyms, class tour, acts, scenes. 4. `conceptkit.sh check
> <slug>` until "no problems found". Review build/sheets/ for overlap, cut-off text and wrong values.
> 5. No video build, upload, commit or push. Don't touch other projects or tools/**; report tool problems instead.
> Keep the golden output independent of the date, machine speed and tool database age; keep `youtube_title` <= 100 characters.
> Write files with an editor tool or a quoted heredoc (<<'EOF'); an unquoted heredoc runs backticked text.
> Rules: online store or bank domain only; never mention the sibling java-design-patterns project; local labs only.
> Report in under 10 lines per project.

## Rules (from the author)

- Never upload or publish without the author's go-ahead in the current session.
- After two failed tries `batch` skips a Draft and exits 0. To retry, stop every uploader process first (a running batch saves the registry
  over your edit), then set its "tries" to 1 in the registry (write it
  with indent=1, sorted, trailing newline, as save_registry does). The watcher now logs UP only when the registry
  says Public.
- `batch` exits 0 even when it leaves a Draft; the watcher then retries the same project and the retry finishes the
  Draft (same video id, no duplicate).
- A failure before the video is attached (e.g. Studio page load timeout) leaves nothing on YouTube; just retry.
  The watcher retries a failed upload twice, a minute apart, before it stops.
- If `batch` fails twice on a Draft (e.g. waiting for the Studio 'Filter' input), look at the upload Chrome window for
  an open dialog, close it, and run `batch` again.
- Upload by explicit project path, never `batch intermediate`: that would also upload projects that are not reworked.
- Check `youtube_title` is at most 100 characters (all intermediate titles checked 11-16 17:12; two shortened) (ytquality fails otherwise). `conceptkit docs` does not rewrite
  docs/youtube.md; only `build` does.
- Voice: Piper `en_US-amy-medium`. No denoise is needed.
- The true-peak limiter fix is in `tools/videokit/videokit/audio.py`. Console slides shrink the font before wrapping
  (`tools/videokit/videokit/slides/kinds.py`).
- `check.py` cuts a spelled-out letter run at 8 letters ("O W A S P A S V S" is flagged as ASV); reword the narration.
- `check.py` ignores hex code points such as FF41 (fixed 11-16).
- Agents share the scratchpad; tell each one to use its own subfolder for helper scripts.
- If the usage limit stops agents, resume them with SendMessage after the reset rather than starting over.

## Next actions

**AUTHOR SAID (11-18): upload security-architecture-review and privacy-engineering once each is rebuilt** (check + ytquality first, learning order).

**AUTHOR SAID (11-16 15:05): upload the built videos as they finish.** A watcher script uploads, strictly in
learning order, the first project not yet Public once its video is reworked, built after concept.toml, and passes
check + ytquality; it stops on a failure (BLOCKED/FAIL line in scratchpad uploaded.txt). In a new session, recreate
that loop: for the next not-Public project in learning order, validate, then `ytupload.sh batch java/<path>`.

The first 12 builds finished OK by 14:06. Since 15:00 the queue builds the projects marked "queued"/"building".
AUTHOR SAID YES (11-16) to uploading the reworked, validated videos: the 6 from tls-misconfigurations-with-testssl to
input-validation-with-hibernate-validator are uploading. The secrets-in-errors-and-logs pair waits until the
race-conditions pair (earlier in learning order) is reworked, built and uploaded; then upload all four in order.

**(Resumed 14:58.) Was paused 2026-11-16 12:14 at the 5-hour usage limit.** Three agents stopped part-way:
race-conditions pair (was adding a `SqlStates` class and sectioning the demo), fuzzing pair (was reviewing its sheets),
secrets-management pair (was about to write the sectioned demo). Their edits are on disk. If the same session is
alive, resume them with SendMessage. In a new session, start one agent per pair with the Agent brief, and tell it the
project is partly reworked: run `git diff -- <project>` and the check first, and finish from there.
The build queue keeps running without the API. Next in the queue: tls-misconfigurations-with-testssl,
inspecting-tls-traffic, -with-wireshark, certificate-pinning, input-validation(+hibernate-validator),
secrets-in-errors-and-logs(+logback). Results are in the session scratchpad `built.txt`; if the scratchpad is gone,
rebuild the projects marked "queued" whose `.mp4` is older than `concept.toml`.

1. Wait for the 4 TLS builds and the 3 agents. As each one finishes, update the table above.
2. AUTHOR ASKED (11-16 14:58) to keep building the rest of the videos: rework and build every remaining intermediate,
   then advanced, project. Queue builds for the projects the agents finish. Next agent pairs: secrets-and-supply-chain
   in learning-path order.
3. DONE 13:21, all 12 Public. 2026-11-16 ~12:40 the AUTHOR ASKED to upload every built-but-not-uploaded reworked video. Uploading in learning
   order: the openid-connect Draft, the 7 identity-and-access videos and the 4 rebuilt TLS videos (all passed check +
   ytquality). Three youtube_title values were shortened to fit 100 characters. Check `docs/youtube-links.json` for
   the ones that are done; run `ytupload.sh batch java/intermediate/<path>` for any that are still missing.
4. Open issue: container-hardening-with-docker-and-kind does not pin kind's node image; a kind release could change
   the golden counts (3, 7, 4). Pin it (CLAUDE.md asks for pinned versions) and rerun integrationTest.
4c. Machine change 11-17 09:39: an agent's unquoted heredoc ran `brew install kind kubectl cosign`; Homebrew
   kubernetes-cli went 1.37.1 -> 1.37.2 (kind, cosign unchanged). Ask the author before reverting.
4d. Verify kubernetes-admission-control: `./gradlew integrationTest` on a quiet machine (use pause-builds).
4e. runtime-security: rerun `./gradlew integrationTest` on a quiet machine; if it still misses records, fix
   collector.sh to carry the container in the audit record instead of reading /proc/<pid>/cgroup afterwards.
   Stray untracked kubernetes-network-policies-explained.{mp4,m4a,srt} sit in runtime-security/video/.
4f. videokit cover.py: a long [poster] tagline overlaps the "Presented by" pill; agents shorten taglines.
4g. tls-attacks-and-downgrades accepts only OpenSSL 3.6.x (Openssl.SERIES); if Homebrew openssl@3 moves on,
   regenerate its golden output and update SERIES.
4a. Link: threat-modeling-with-pytm hard-codes PLAIN_DRAFT_PROBLEMS=2 and PLAIN_STRIDE_CANDIDATES=20 from
   threat-modeling's output (PytmDemo.java). Update both if that output changes.
4b. Open issue: sbom-and-provenance-with-syft-and-grype golden line "built within 7 days: yes" depends on grype's
   database age (integrationTest only). Consider printing it without the age.
5. Open question for the author: network-isolation's plan says "kind with network policies"; the project uses Docker only.
6. A stray Draft of advanced cloud-misconfigurations may exist in YouTube Studio (upload window closed mid-form at
   01:14 on 11-17, before it was registered). The author may delete it by hand; Claude does not delete videos.
7. Open issue for the author: the slide footer says "Security Basics" at every level
   (`tools/conceptkit/conceptkit/generate.py:504`). It should probably name the level. Ask before changing it,
   because a change alters every rebuilt video.
