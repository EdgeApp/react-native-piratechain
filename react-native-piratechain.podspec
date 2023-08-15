require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']
  s.homepage     = package['homepage']
  s.license      = package['license']
  s.authors      = package['author']

  s.swift_version = '5.4'
  s.platform     = :ios, "12.0"
  s.requires_arc = true
  s.source       = { :git => "https://github.com/EdgeApp/react-native-piratechain.git", :tag => "v#{s.version}" }
  s.source_files = "ios/**/*.{h,m,swift}"

 s.dependency "React"
 s.dependency 'PirateLightClientKit', :git => 'https://github.com/peachbits/PirateLightClientKit.git', :commit => '9bd5f5de3448bd4be60b2991e9c9b028d6056ac9'

end
