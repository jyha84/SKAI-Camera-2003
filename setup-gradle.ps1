$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$base = "https://raw.githubusercontent.com/gradle/gradle/v8.11.1"
Invoke-WebRequest "$base/gradle/wrapper/gradle-wrapper.jar" -OutFile "gradle/wrapper/gradle-wrapper.jar"
Invoke-WebRequest "$base/gradlew" -OutFile "gradlew"
Invoke-WebRequest "$base/gradlew.bat" -OutFile "gradlew.bat"
