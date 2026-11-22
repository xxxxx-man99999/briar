# Briar Experimental Fork — Low-Bandwidth, Cellular-Aware and Voice Work

> **Status: experimental fork; not an official Briar release.**
> This documentation describes work in the public fork [`xxxxx-man99999/briar`](https://github.com/xxxxx-man99999/briar).
> It is not a security audit, release announcement, or claim that all changes are upstreamed.

## Project intent

Explore practical improvements and foundations for Briar on constrained cellular connections, alongside experimental post-quantum key-agreement support and voice-message fixes.

### Areas of work

- **Cellular-aware policy:** identify the active cellular generation (2G, 3G, LTE, 5G/NR, or unknown) and expose it to application/network policy.
- **Low-bandwidth transfer policy:** smaller target chunk sizes on slower cellular networks (reported policy targets: 1 KB for 2G, 4 KB for 3G, and 16 KB for faster cellular); policy values are implementation targets, not universal performance guarantees.
- **Transfer resilience concepts:** adaptive queue/retry/backoff and resumption are design goals/experimental behavior. Verify the exact code path and per-feature status before describing any item as complete.
- **Post-quantum cryptography (PQC):** ML-KEM-768 primitive integration is a foundation for a future hybrid classical + PQ handshake. The wire protocol and peer capability negotiation are **not confirmed complete**; do not describe Briar communications as post-quantum protected.
- **Voice messages:** experimental recording foundation and a separate release-manifest/recorder-cleanup fix are tracked in the fork. End-to-end voice-message transfer and release/device validation remain unverified.

## Reported testing

The developer reported a two-device experiment in which text, images/files, and audio recordings were transferred while using a 2G connection. This is a **user-reported test outcome**; a reproducible test log (device models, carrier, radio state, file sizes, duration, failure/retry counts, and build SHA) has not been published in this documentation pack.

Earlier project context also recorded a CI run (#64) passing on commit `bfb92ec5eb8656a8b652b745e535f8c2401a2679` for build/tests/source packaging. That historical run does not establish that every later branch or PR is green.

## Current repository / PR status (checked 2026-11-22)

- PR #1 — [2G/3G-aware low-bandwidth networking and ML-KEM support](https://github.com/xxxxx-man99999/briar/pull/1): **closed, not merged**. Its description documents cellular detection, low-bandwidth chunk targets, ML-KEM-768 primitive support, tests, and an Actions workflow. The PR explicitly says a full hybrid handshake needs wire-format/capability negotiation and was not implemented there.
- PR #2 — [Nearby Devices QR connection diagnostics](https://github.com/xxxxx-man99999/briar/pull/2): **open, not merged**. Fail-fast when no shared transport is detected and warning-level diagnostics for connection task exceptions. Build/tests not recorded as run in PR description.
- PR #3 — [Voice activity registration and recorder cleanup](https://github.com/xxxxx-man99999/briar/pull/3): **open draft, not merged**. Main-manifest activity/permission registration and recorder error cleanup. Android build/device validation not recorded as run in PR description.

## Important limits

- This fork is experimental. Use at your own risk and do not rely on it for high-risk communication without independent review.
- Cellular network labels do not guarantee actual throughput, latency, or packet loss.
- Chunk targets do not by themselves guarantee successful transfer; transport, scheduling, buffering, protocol limits, and device conditions matter.
- No claim is made that a complete ML-KEM hybrid handshake, PQC wire protocol, or end-to-end voice transport is finished.
- Reported 2G testing needs reproducible details and independent confirmation.

## Contributing / reporting results

Please include: fork commit SHA, Android version and device models, network/operator, whether the phone was forced to 2G or merely showed 2G, payload type/size, elapsed time, success/failure, and logs with sensitive information removed.
