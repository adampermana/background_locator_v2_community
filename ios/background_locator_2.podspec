#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'background_locator_v2_community'
  s.version          = '2.1.0'
  s.summary          = 'A Flutter plugin for getting location updates even when the app is killed.'
  s.description      = <<-DESC
A new Flutter plugin.
                       DESC
  s.homepage         = 'https://github.com/rekab-app/background_locator'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'REKAB' => 'mehdok@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'

  s.ios.deployment_target = '13.0'
end
