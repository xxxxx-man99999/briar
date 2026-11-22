# Briar

Briar is a messaging app designed for activists, journalists, and anyone else who needs a safe, easy and robust way to communicate.

Unlike traditional messaging apps, Briar doesn't rely on a central server - messages are synchronized directly between the users' devices.

If the Internet's down, Briar can sync via Bluetooth or Wi-Fi, keeping information flowing in a crisis. If the Internet's up, Briar can sync via the Tor network, protecting users and their relationships from surveillance.

---

## Experimental fork: low-bandwidth & cellular-aware communication

> **Independent research / experimental changes — not an official Briar release and not endorsed by the upstream project.**

This fork explores ways to make peer-to-peer communication more usable under weak or constrained mobile connectivity, with an emphasis on cellular-aware transfer behavior and Android.

### Focus areas

- Cellular-aware and low-bandwidth transfer-policy experiments
- Android communication and voice-message improvements
- ML-KEM-768 cryptographic primitive integration foundation (not a complete hybrid protocol integration)
- Test notes, technical documentation, and upstream-oriented proposals

### Reported 2G device test

The developer reports a two-Android-device test in which the modified build exchanged:

| Content | Reported outcome |
|---|---|
| Text messages | Successfully exchanged |
| Images / files | Transfer reportedly succeeded |
| Audio recordings | Transfer reportedly succeeded |

This is **qualitative, developer-reported evidence**, not a benchmark or independently reproduced result. The test record currently lacks full device/OS and carrier details, verified radio-mode evidence, payload sizes, elapsed times, trial counts, transfer logs, and APK/build identifiers. No comparative claim against upstream Briar is made.

### Experiment documentation

- [Experiment overview](docs/low-bandwidth-experiment/README.md)
- [Technical case study](docs/low-bandwidth-experiment/TECHNICAL_CASE_STUDY.md)
- [Test report & reproducibility checklist](docs/low-bandwidth-experiment/TEST_REPORT.md)
- [Experimental changelog](docs/low-bandwidth-experiment/CHANGELOG_EXPERIMENTAL.md)
- [Draft upstream proposal](docs/low-bandwidth-experiment/UPSTREAM_PROPOSAL.md)

### Project status

The work is experimental and ongoing. Changes may be incomplete, may not be suitable for production, and are not guaranteed to preserve upstream behavior. Please review the relevant branches/commits and test carefully before using.

---

## Download Briar

[<img src="https://briarproject.org//img/fdroid_badge.png" width="240">](https://briarproject.org/fdroid)
[<img src="https://briarproject.org/img/google_play_badge_web_generic.png" width="240">](https://play.google.com/store/apps/details?id=org.briarproject.briar.android)

You can also [download the APK file](https://briarproject.org/apk) directly from our site.

## Useful links

- [Project website](https://briarproject.org/)
- [Upstream source code](https://code.briarproject.org/briar/briar/tree/master)
- [User manual](https://briarproject.org/manual/)
- [Wiki](https://code.briarproject.org/briar/briar/-/wikis/home)
- [Privacy policy](https://briarproject.org/privacy)

## Reproducible builds

We provide [docker images](https://code.briarproject.org/briar/briar-reproducer#briar-reproducer) to ease the task of verifying that the published APK binaries include nothing but our publicly available source code.

You can either use those images or use them as a blueprint to build your own environment for reproduction.

## Donate

[![Donate using Liberapay](https://briarproject.org/img/liberapay.svg)](https://liberapay.com/Briar/donate)

Bitcoin and BCH: 1NZCKkUCtJV2U2Y9hDb9uq8S7ksFCFGR6K
