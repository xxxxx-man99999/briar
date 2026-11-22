# Technical Case Study: Briar Experiments for Constrained Cellular Connectivity

## Executive summary

This fork explores cellular-aware transfer policy and related foundations in Briar. The project goal is to make application behavior more suitable for slow or unstable cellular connectivity, while keeping existing protocol behavior intact unless a compatible change is explicitly implemented and tested.

A developer-reported two-device test transferred text, images/files, and audio recordings while on 2G. The result is encouraging but remains a reported experiment until a detailed, reproducible test record is published.

## Problem statement

Slow cellular connections can combine low throughput, high latency, intermittent connectivity, and limited practical transfer windows. Large transfer units or aggressive retry behavior may waste available bandwidth or delay smaller messages. A transfer strategy should be network-aware, prioritize lightweight messages where possible, and tolerate interruption.

## Design / implementation areas documented by project history

### Cellular classification and policy
- Recognize cellular categories: 2G, 3G, LTE, 5G/NR, and unknown.
- Provide the radio category to policy code.
- Reported chunk-size targets: 2G = 1 KB; 3G = 4 KB; faster cellular = 16 KB.
- These are policy targets—not measured throughput guarantees or proof that every transfer path uses these sizes.

### Transfer resilience
Queueing, retries, backoff, priority for text, and resumption have been discussed/implemented experimentally in project history. Before publishing definitive implementation claims, verify the current branch’s exact classes and call sites. The documentation intentionally marks feature completeness as requiring source verification.

### Post-quantum cryptography
ML-KEM-768 primitive support is described as a building block toward a hybrid Curve25519 + ML-KEM key agreement. A primitive/library integration is not the same as a protocol-level hybrid handshake. The project history states that wire-format key material and capability negotiation remain unfinished. Therefore, this fork must **not** be advertised as post-quantum-secure messaging.

### Voice
The voice work includes an experimental recording foundation and a PR to correct activity registration/permission placement and improve recorder failure cleanup. A fully integrated, tested voice-message transport and release validation are not established by the records available.

## User-reported experiment

**Reported outcome:** two Android devices exchanged text, images/files, and audio recordings during a 2G test.

**Not yet captured in a reproducible report:**
- exact device models and Android builds;
- operator, coverage, and whether 2G was manually forced;
- app commit SHA and APK hash;
- payload sizes and counts;
- time to send/receive;
- transfer retries, interruptions, and resume behavior;
- screenshots/logcat excerpts with private content removed;
- repeated trial count and control comparison against upstream Briar.

This report therefore uses “reported successful test” rather than claiming a benchmark or verified reproducibility.

## Repository and contribution status (2026-11-22)

- PR #1: closed without merge. Describes cellular classification, low-bandwidth policy, ML-KEM primitive integration, tests/workflow; protocol-level hybrid handshake explicitly not completed.
- PR #2: open. QR nearby-device failure diagnostics/fail-fast behavior; validation not yet recorded as run.
- PR #3: open draft. Voice activity manifest/permission and recorder cleanup; Android build and device tests not recorded as run.

## Next steps for credible publication

1. Record a repeatable test protocol and run at least 3 trials per payload/network condition.
2. Capture results for upstream baseline vs fork using the same devices and carrier/conditions.
3. Measure elapsed time, success rate, bytes transferred if available, retries, and interruption recovery.
4. Verify exact code paths and document source file/class references.
5. Avoid security claims until protocol changes are implemented, reviewed, and tested.
