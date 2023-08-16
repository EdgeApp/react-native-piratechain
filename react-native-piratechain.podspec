require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']
  s.homepage     = package['homepage']
  s.license      = package['license']
  s.authors      = package['author']

  s.platform     = :ios, "12.0"
  s.source = {
    :git => "https://github.com/EdgeApp/react-native-piratechain.git",
    :tag => "v#{s.version}"
  }
  s.source_files =
    "ios/react-native-piratechain-Bridging-Header.h",
    "ios/RNPiratechain.m",
    "ios/RNPiratechain.swift",
    "ios/PirateLightClientKit/**/*.swift"
  s.resource_bundles = {
    "piratechain-mainnet" => "ios/PirateLightClientKit/Resources/piratesaplingtree-checkpoints/mainnet/*.json",
    "piratechain-testnet" => "ios/PirateLightClientKit/Resources/piratesaplingtree-checkpoints/testnet/*.json"
  }
  s.vendored_frameworks = "ios/libpiratelc.xcframework"

  s.dependency "gRPC-Swift", "~> 1.0"
  s.dependency "SQLite.swift", "~> 0.12"
  s.dependency "React-Core"
end
