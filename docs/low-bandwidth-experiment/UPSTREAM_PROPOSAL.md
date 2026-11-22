# Upstream Proposal Draft — Cellular-Aware Transfer Policy for Briar

## Summary

Explore a small, reviewable contribution that exposes cellular-network classification to transfer policy and adjusts transfer sizing for constrained links, without changing Briar’s wire protocol or cryptographic handshake in the first stage.

## Motivation

On slow or unstable cellular connections, transfer behavior should avoid assuming broadband-like conditions. A network-aware policy may use smaller transfer units on slower radio categories while preserving compatibility and existing security properties.

## Proposed scope for an initial upstream discussion

1. Detect and represent cellular category (2G/3G/LTE/5G/unknown) in a clear, testable abstraction.
2. Add a policy with conservative target transfer sizes; proposed/experimental values in the fork history: 1 KB (2G), 4 KB (3G), 16 KB (faster cellular).
3. Add unit tests for policy selection and unknown/fallback behavior.
4. Add a benchmark/test harness that compares policy behavior under simulated low throughput, high latency, and interruption.
5. Keep protocol, crypto, and transport changes out of the first proposal unless separately reviewed.

## Evidence currently available

- Developer-reported two-device test: text, images/files, and audio recordings transferred during a 2G test.
- Historical project context reports CI run #64 passing on commit `bfb92ec5eb8656a8b652b745e535f8c2401a2679`.
- Reproducible test details and comparative benchmark data are not yet documented.

## Compatibility and safety

- No wire-format change is proposed in this first-stage discussion.
- Do not claim the ML-KEM primitive integration changes the security of existing Briar handshakes.
- Any future hybrid key agreement must define capability negotiation, protocol encoding, downgrade/fallback behavior, interoperability, and security review before adoption.

## Questions for maintainers

- Is cellular classification and policy selection an acceptable abstraction in Briar’s architecture?
- Which layer should own network-aware transfer sizing?
- What test/benchmark evidence would be required for review?
- Would maintainers prefer a small PR per change rather than a bundled experimental fork?
