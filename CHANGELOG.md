# React Native Piratechain

## Unreleased

## 0.5.11 (2025-07-22)

- changed: Updated checkpoints

## 0.5.10 (2025-05-20)

- fixed: Ambiguous `Expression` imports in Swift.

## 0.5.9 (2025-04-30)

- changed: Updated checkpoints

## 0.5.8 (2025-04-02)

- changed: Updated checkpoints

## 0.5.7 (2025-02-28)

- changed: Updated checkpoints
- fixed: Fix txid returned from broadcast

## 0.5.6 (2024-11-21)

- changed: Updated checkpoints

## 0.5.5 (2024-11-21)

- changed: (Android) Update pirate-lightwallet-client to v1.19.0-beta02
- changed: Updated checkpoints

## 0.5.4 (2024-10-31)

- changed: Updated checkpoints

## 0.5.3 (2024-10-16)

- changed: Replace `pirate.lightwallet.client` with precompiled .aar that resolves incompatibility with newer versions of `io.grpc`
- changed: Updated checkpoints

## 0.5.2 (2024-10-01)

- changed: Updated checkpoints

## 0.5.1 (2024-09-16)

- changed: Updated checkpoints

## 0.5.0 (2024-09-05)

- added: Add error listeners
- changed: Updated checkpoints

## 0.4.17 (2024-08-19)

- changed: Updated checkpoints

## 0.4.16 (2024-08-07)

- changed: Updated checkpoints

## 0.4.15 (2024-07-24)

- changed: Updated checkpoints

## 0.4.14 (2024-06-10)

- changed: Updated checkpoints
- fixed: Fix deriveViewingKey return value type

## 0.4.13 (2024-06-06)

- changed: Updated checkpoints

## 0.4.12 (2024-05-27)

- fixed: Add a missing header file to the podspec.

## 0.4.11 (2024-05-17)

- fixed: Pause synchronizer events until JavaScript is ready to receive them.

## 0.4.10 (2024-05-11)

- fixed: Stop depending on the iOS-provided SQLite, which causes crashes on iOS 13-15 because it is too old.

## 0.4.9 (2024-04-24)

- changed: Updated checkpoints

## 0.4.8 (2024-04-12)

- changed: Updated checkpoints
- fixed: Update the packaging scripts to clean leftover files.
- fixed: Update the packaging scripts to correctly report errors, so we don't send failed packages to NPM.

## 0.4.7 (2024-03-27)

- changed: Updated checkpoints

## 0.4.6 (2024-03-12)

- changed: Updated checkpoints

## 0.4.5 (2024-02-23)

- changed: Updated checkpoints
- fixed: (android) Wrap sdk methods in try/catch to prevent native crashes

## 0.4.4 (2024-02-13)

- changed: Updated checkpoints

## 0.4.3 (2024-01-14)

- changed: Updated checkpoints

## 0.4.2 (2023-11-09)

- fixed: (Android) Make sure parseTx always returns a `memos` field
- changed: Updated checkpoints

## 0.4.1 (2023-11-03)

- changed: Updated checkpoints

## 0.4.0 (2023-10-09)

- changed: Upgrade pirate-android-sdk to v1.19.0-beta01
- changed: Upgrade PirateLightClientKit to v0.19.0-beta
- changed: Repackage `KoyTool` and `AddressTool` methods synchronizer-independent `Tools`
- changed: Always return memos array with transactions

## 0.3.5 (2023-08-17)

- fixed: Update our default Kotlin version to be compatible with React Native v0.72.
- changed: Remove our iOS dependency on PirateLightClientKit by copying the Swift sources directly into this NPM package. This removes the need for users to touch checkpoints on either platform.
- added: Add checkpoints to repo with script to update and copy them from Android to iOS build directories
- changed: Proper install instructions for Android in README

## 0.3.3 (2023-06-22)

- fixed: Update the Android build.gradle to use the upstream-specified Kotlin version and upstream-specified appcompat library version.

## 0.3.2 (2022-12-20)

- getBirthdayHeight: Remove Android specific network name and use host and port for both platforms

## 0.3.0 (2022-12-19)

- Init Piratechain
- Port features and fixes from react-native-zcash:
  - Add `getBirthdayHeight` method to query blockheight without an active synchronizer
  - iOS: Add missing `getLatestNetworkHeight` method
  - RN: Remove unimplemented methods and POC comments
  - Fix exported types
  - iOS: Handle potential throw in synchronizer.latestHeight()

## 0.2.2 (2022-06-10)

- Upgrade SDKs to NU5 compatible versions
  - Android: Upgrade zcash-android-sdk to v1.5.0-beta01
  - iOS: Upgrade ZcashLightClientKit to v0.14.0-beta
- iOS: Fix memory leak after stopping synchronizer
- ANdroid: White space and import cleanups

## 0.2.1 (2022-03-16)

- Update the ZcashLightClientKit dependency
- Remove unused build scripts

## 0.2.0 (2022-01-10)

- Add iOS support
- Android: Cleanup unused methods

## 0.1.0 (2021-11-09)

- Initial release

## 0.0.2

- Add stubs for deriveViewKey and getShieldedBalance

## 0.0.1

- Initial release
