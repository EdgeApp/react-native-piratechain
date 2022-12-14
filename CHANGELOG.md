# React Native Piratechain

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
