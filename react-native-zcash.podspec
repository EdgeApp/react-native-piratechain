require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']
  s.homepage     = package['homepage']
  s.license      = package['license']
  s.authors      = package['author']

  s.platform     = :ios, "8.0"
  s.requires_arc = true
  s.source       = { :git => "https://github.com/EdgeApp/react-native-zcash.git", :tag => "v#{s.version}" }
  s.source_files = "ios/**/*.{h,m, swift}", ""
  s.script_phase = {
      :name => 'Build generate constants and build librustzcash',
      :script => 'sh ${PODS_TARGET_SRCROOT}/Scripts/build_librustzcash_xcode.sh',
      :execution_position => :before_compile
   }
   s.prepare_command = <<-CMD
   sh Scripts/prepare_zcash_sdk.sh
 CMD


#   s.prepare_command = <<-CMD
#   sh Scripts/prepare_zcash_sdk.sh
# CMD
# s.xcconfig = {
  #   'CLANG_CXX_LANGUAGE_STANDARD' => 'gnu++0x',
  #   'CLANG_CXX_LIBRARY' => 'libc++'
  # }

  s.dependency 'ZcashLightClientKit', '0.12.0-beta.4'
  s.dependency "React"
end
