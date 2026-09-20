# Investment Rebalance Tool Handoff

## Current State

- Android Kotlin project for calculating target investment allocations and rebalance amounts.
- Public repository: https://github.com/matt-xb/investment-rebalance-tool
- Gradle caches, IDE state, build output, and `local.properties` are excluded.
- No open-source license has been selected yet; public visibility alone does not grant reuse rights.

## Verification

- Source snapshot uploaded on 2026-09-20.
- Android build and calculation acceptance were not rerun during repository organization.
- GitHub prerelease `v0.1.0` contains `investment-rebalance-v0.1.0-debug.apk` (10,533,487 bytes, SHA-256 `d8a122042def6194cc462e49c5b75a11b607426b65d1f3b520218b5fe7d11017`).
- APK metadata confirms application ID `com.example.rebalance`, version `0.1.0`, min SDK 26, target SDK 35; APK Signature Scheme v2 verification passes with the Android Debug certificate.

## Next Step

- Restore the local Android SDK path, build the debug variant, and verify calculations with known allocation examples before releasing.
- Select and add an open-source license before inviting external reuse or contributions.
