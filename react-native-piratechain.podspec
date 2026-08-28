require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

# Each bundled C dep module (ios/vendored/cmodules/<Name>/ — headers +
# module.modulemap) becomes one relative clang include path, so the in-pod
# PirateLightClientKit source can resolve the C modules that the pre-built
# Swift dependency modules (SwiftNIO / GRPC) import.
cmodule_flags = Dir.glob(File.join(__dir__, "ios/vendored/cmodules/*"))
  .select { |p| File.directory?(p) }
  .map { |p| "-Xcc -I\"$(PODS_TARGET_SRCROOT)/ios/vendored/cmodules/#{File.basename(p)}\"" }
  .join(" ")

# The pre-built Swift modules under ios/vendored/ are only readable by the
# EXACT Swift compiler that produced them (the binary .swiftmodule format is
# not stable across compilers, and the stable alternative — library-evolution
# .swiftinterface — is unavailable because swift-nio rejects evolution builds
# by upstream policy; see apple/swift-nio#2470/#2897, closed "not planned").
# Fail fast with instructions instead of letting the build die later on a
# cryptic "module compiled with Swift X cannot be imported by Swift Y" error.
stamp_path = File.join(__dir__, "ios/vendored/swift-version.txt")
if File.exist?(stamp_path)
  built_with = File.read(stamp_path).strip
  local_swift = `xcrun swift --version 2>/dev/null`[/swiftlang-[0-9.]+/]
  if !built_with.empty? && !local_swift.nil? && built_with != local_swift
    raise <<~MSG
      react-native-piratechain: the prebuilt Swift dependency modules in
      ios/vendored/ were built with #{built_with}, but this machine's Swift
      compiler is #{local_swift}. Binary .swiftmodule files only load under the
      exact compiler that produced them.

      Fix: rebuild the vendored dependencies with your toolchain:
        cd node_modules/react-native-piratechain && npm run update-sources
      (or switch to the Xcode whose Swift is #{built_with})
    MSG
  end
end

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']
  s.homepage     = package['homepage']
  s.license      = package['license']
  s.authors      = package['author']

  s.platform     = :ios, "13.0"
  s.source = {
    :git => "https://github.com/EdgeApp/react-native-piratechain.git",
    :tag => "v#{s.version}"
  }

  # The bridge + the vendored PirateLightClientKit Swift source, compiled
  # in-pod as ONE module (so the bridge uses SDK types directly — see copySwift
  # in scripts/updateSources.ts).
  s.source_files =
    "ios/libpiratelc.h",
    "ios/react-native-piratechain-Bridging-Header.h",
    "ios/RNPiratechain.m",
    "ios/RNPiratechain.swift",
    "ios/PirateLightClientKit/**/*.swift"
  s.resource_bundles = {
    "piratechain-mainnet" => "ios/PirateLightClientKit/Resources/piratesaplingtree-checkpoints/mainnet/*.json",
    "piratechain-testnet" => "ios/PirateLightClientKit/Resources/piratesaplingtree-checkpoints/testnet/*.json"
  }

  s.dependency "MnemonicSwift", "~> 2.2"
  s.dependency "React-Core"

  # ---------------------------------------------------------------------------
  # The SDK's SwiftPM dependencies (grpc-swift, SwiftNIO, SwiftProtobuf,
  # SQLite.swift) pre-built into libPirateDeps.xcframework, plus their Swift and
  # C modules (ios/vendored/modules + cmodules) that the in-pod
  # PirateLightClientKit source imports at compile time. grpc-swift 1.24+ ships
  # SwiftPM-only with no podspec, so these can no longer be CocoaPods
  # `dependency`s; vendoring them as a static binary keeps the host app on
  # STATIC frameworks.
  #
  # PROVIDER / CONSUMER — the host app chooses:
  # react-native-zcash links the SAME dependency graph and vendors it the same
  # way. Two copies of that graph on one app link line collide under the app's
  # -ObjC flag (it force-loads every Swift archive member → tens of thousands of
  # duplicate symbols), so exactly ONE pod may put the graph on the link line.
  #   * PROVIDER (default): this pod links its own libPirateDeps.xcframework and
  #     is fully self-contained — correct for any app that uses piratechain
  #     without react-native-zcash.
  #   * CONSUMER: an app that ALSO installs react-native-zcash (e.g. Edge) sets
  #       ENV['RN_PIRATECHAIN_VENDOR_DEPS'] = 'false'
  #     in its Podfile. This pod then omits its own archive from the link and its
  #     grpc/NIO symbols resolve from zcash's archive instead. In BOTH modes the
  #     in-pod SDK source still compiles against the modules below.
  # scripts/depsPackage.resolved pins this graph to the EXACT versions
  # react-native-zcash resolves, so the two archives are interchangeable — keep
  # them in lockstep when react-native-zcash bumps its SDK.
  #
  # sqlite3 is not in this binary: on Apple platforms SQLite.swift has no C-shim
  # target — it imports the system `sqlite3` clang module. (Edge's pre-existing
  # duplicate-sqlite3 ld WARNINGS come from libpiratelc vs libzcashlc, the two
  # Rust cores, and are unrelated to this package's deps binary.)
  #
  # Regenerate ios/vendored/ with `npm run update-sources`
  # (scripts/buildVendoredDeps.ts).
  # ---------------------------------------------------------------------------
  vendor_deps = ENV['RN_PIRATECHAIN_VENDOR_DEPS'] != 'false'
  vendored_frameworks = ["ios/libpiratelc.xcframework"] # the Rust core, always
  vendored_frameworks << "ios/vendored/libPirateDeps.xcframework" if vendor_deps
  s.vendored_frameworks = vendored_frameworks

  s.preserve_paths = "ios/vendored/**/*"
  s.pod_target_xcconfig = {
    "SWIFT_INCLUDE_PATHS" => "\"$(PODS_TARGET_SRCROOT)/ios/vendored/modules\"",
    "OTHER_SWIFT_FLAGS" => cmodule_flags
  }
end
