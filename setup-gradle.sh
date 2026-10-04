#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
base=https://raw.githubusercontent.com/gradle/gradle/v8.11.1
curl -fL "$base/gradle/wrapper/gradle-wrapper.jar" -o gradle/wrapper/gradle-wrapper.jar
curl -fL "$base/gradlew" -o gradlew
curl -fL "$base/gradlew.bat" -o gradlew.bat
chmod +x gradlew
