# Experimental Fork Change Log (Documentation Snapshot)

This is a curated summary from the project’s recorded PR descriptions and prior development notes. It is not a verified diff of every commit.

## Cellular / low-bandwidth policy
- Cellular category recognition: 2G, 3G, LTE, 5G/NR, unknown.
- Low-bandwidth policy targets recorded: 1 KB for 2G, 4 KB for 3G, 16 KB for faster cellular.
- Unit tests and an Actions workflow were described in PR #1.
- PR #1 was closed without merge.

## ML-KEM-768
- Primitive/library support was added as a foundation for future hybrid Curve25519 + ML-KEM key agreement.
- The full hybrid handshake, wire protocol integration, and capability negotiation are not documented as completed.
- Do not claim post-quantum-secure message exchange.

## Nearby Devices diagnostics (PR #2)
- Fail fast with a warning if scanned QR data has no transport in common with the local device.
- Log key-agreement connection task exceptions with stack traces at WARNING level.
- PR remains open; build/test status is not recorded as run.

## Voice recorder release setup/cleanup (PR #3)
- Register `VoiceActivity` in the main Android manifest so debug/release variants receive it.
- Place `RECORD_AUDIO` permission in the main manifest and remove duplicate debug-only activity registration.
- Guard `MediaRecorder` setup/start; handle `IOException` and `RuntimeException`, release resources, and delete partial output after failure.
- PR remains open and draft; Android build/device tests not recorded as run.

## Experimental status
The developer reported successful 2G transfers of text, images/files, and audio recordings between two devices. Detailed reproducibility data has not yet been added.
