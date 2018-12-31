# Disklet

## 0.3.0 (2018-11-16)

- Add a new, radically simplified API. The existing API is still available but deprecated:
  - `makeLocalStorageFolder` → `makeLocalStorageDisklet`
  - `makeMemoryFolder` → `makeMemoryDisklet`
  - `makeNodeFolder` → `makeNodeDisklet`
  - `makeReactNativeFolder` → `makeReactNativeDisklet`
- New utilities:
  - `deepList`
  - `logDisklet`
  - `mergeDisklets`
  - `navigateDisklet`
- Add a new `downgradeDisklet` helper to convert new `Disklet` objects to the old `DiskletFolder` interface.

## 0.2.10 (2018-11-13)

- Fix Flow errors.

## 0.2.9 (2018-11-12)

- Add complete Flow typing coverage.
- Fix binary data on React Native (`setData` / `getData` were corrupting data before).

## 0.2.8 (2018-11-03)

- Upgrade build tooling.
- Begin adding Flow types.

## 0.2.7

- Fix the `EISDIR` error on React Native Android

## 0.2.6

- Add getPath() endpoint for Files

## 0.2.3

- Fix Android due to varied error codes from OS

## 0.2.2

- Add support for using react-native-fs

## 0.1.3

- Create directories even more carefully

## 0.1.2

- Fix some documentation typos
- Fix the `browser` section of `package.json`
- Do not fail on parallel disk writes to Node.js folders

## 0.1.1

- Clean up the documentation
- Add missing typings for `mapFolders`
- Add the `makeLoggedFolder` helper

## 0.1.0

- Initial release