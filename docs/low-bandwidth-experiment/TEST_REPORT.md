# Experimental Test Report — Briar Fork

**Report date:** 2026-11-22  
**Repository:** https://github.com/xxxxx-man99999/briar  
**Report status:** preliminary; combines project records with developer-reported testing.

## 1. Test outcome summary

| Test | Evidence/status | Confidence / limitation |
|---|---|---|
| Text transfer on 2G | Developer reports success between two devices | No run log, SHA, carrier, exact radio-lock state, or repeat count recorded |
| Image/file transfer on 2G | Developer reports success | File size, time, retries, and test repetition not recorded |
| Audio recording transfer on 2G | Developer reports success | Need to distinguish recording creation from message attachment/transfer; pipeline and test evidence not fully documented |
| Build/CI historical run | Project history reports CI run #64 passed on `bfb92ec5eb8656a8b652b745e535f8c2401a2679` | Does not certify current PR branches |
| PR #2 build/device tests | PR body says not run | Pending |
| PR #3 Android build/device tests | PR body says not run; draft | Pending |
| Full hybrid PQ handshake | Not implemented/verified | Do not claim PQ-secure Briar messaging |
| Upstream↔fork compatibility | Not tested/recorded | Pending |

## 2. Reported test environment

The developer described testing with two Android devices. The exact models, Android versions, carrier/operator, build commit, and APK hashes are not present in the currently available test record.

## 3. Reproducibility checklist

For each test run, record:

- [ ] Date/time and location context (general area only; do not publish precise private location)
- [ ] Device A model / Android version
- [ ] Device B model / Android version
- [ ] App version, branch, commit SHA, APK hash
- [ ] Mobile operator and whether 2G was forced/verified
- [ ] Payload type (text/image/file/audio) and exact size
- [ ] Number of trials
- [ ] Start/end time and elapsed time
- [ ] Result: pass/fail
- [ ] Retry count / resume after interruption
- [ ] Relevant logs, sanitized of names, keys, contact identifiers, and message content

## 4. Proposed matrix (not yet results)

| Network | Payload | Trials | Metrics |
|---|---|---:|---|
| 2G | Short text | 3+ | delivery success, latency |
| 2G | Image (small/medium) | 3+ each | success, duration, retries |
| 2G | File (small/large) | 3+ each | success, duration, interruption recovery |
| 2G | Audio attachment | 3+ | attachment creation vs transfer, success, duration |
| 3G | Same payload set | 3+ | comparison |
| LTE | Same payload set | 3+ | baseline comparison |
| Upstream Briar vs fork | Matched payload/conditions | 3+ | comparative measurements |

**Do not fill this matrix with estimates.** Add only observed values.

## 5. Interpretation

The reported transfer success is useful preliminary evidence that the modified build was able to operate in the tested conditions. Without detailed logs, controlled comparison, and repeated trials, it does not prove a general 2G performance improvement, reliable resume, or universal support across carriers/devices.
