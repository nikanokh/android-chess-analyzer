param(
  [ValidateSet("debug", "release")]
  [string]$Mode = "debug"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host " Chess Insight Analyzer - Pure Kotlin + Jetpack Compose Builder " -ForegroundColor Cyan
Write-Host " Package: com.nikanokh.chessinsightanalyzermobile               " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

# 1. Configure JAVA_HOME to Android Studio JBR (JDK 17 / 21)
$defaultJbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path "$defaultJbr\bin\java.exe") {
  $env:JAVA_HOME = $defaultJbr
  $env:Path = "$env:JAVA_HOME\bin;$env:Path"
  Write-Host "[OK] Using Android Studio JDK: $env:JAVA_HOME" -ForegroundColor Green
}

# 2. Configure local.properties with Android SDK path
$sdkDir = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
if (Test-Path $sdkDir) {
  $escapedSdk = $sdkDir.Replace("\", "\\")
  Set-Content -Path ".\local.properties" -Value "sdk.dir=$escapedSdk" -Encoding ASCII
  Write-Host "[OK] Configured local.properties -> $sdkDir" -ForegroundColor Green
}

# 3. Ensure gradle/wrapper/gradle-wrapper.jar exists
$wrapperJar = ".\gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
  New-Item -ItemType Directory -Force -Path ".\gradle\wrapper" | Out-Null
  Write-Host "[INFO] Downloading gradle-wrapper.jar (v8.13)..." -ForegroundColor Yellow
  Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar" -OutFile $wrapperJar
  Write-Host "[OK] Downloaded gradle-wrapper.jar" -ForegroundColor Green
}

# 4. Build APK with Gradle
if ($Mode -eq "release") {
  Write-Host "[Building Release APK (Jetpack Compose)]..." -ForegroundColor Yellow
  .\gradlew.bat assembleRelease
  $apk = Resolve-Path ".\app\build\outputs\apk\release\app-release.apk"
  Write-Host "[SUCCESS] Release APK ready at: $apk" -ForegroundColor Green
} else {
  Write-Host "[Building Debug APK (Jetpack Compose)]..." -ForegroundColor Yellow
  .\gradlew.bat assembleDebug
  $apk = Resolve-Path ".\app\build\outputs\apk\debug\app-debug.apk"
  Write-Host "[SUCCESS] Debug APK ready at: $apk" -ForegroundColor Green
}
