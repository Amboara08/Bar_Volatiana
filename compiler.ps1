$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sdkPath = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$javaPath = "C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot"
$gradlePath = "C:\Gradle\gradle-2.2.1\bin\gradle.bat"

if (-not (Test-Path (Join-Path $sdkPath "platforms\android-22\android.jar"))) {
    throw "Android SDK Platform 22 est absent de $sdkPath"
}
if (-not (Test-Path (Join-Path $sdkPath "build-tools\22.0.1\aapt.exe"))) {
    throw "Android Build-Tools 22.0.1 est absent de $sdkPath"
}
if (-not (Test-Path $javaPath)) { throw "Java 8 est absent de $javaPath" }
if (-not (Test-Path $gradlePath)) { throw "Gradle 2.2.1 est absent de $gradlePath" }

$env:JAVA_HOME = $javaPath
$env:Path = "$javaPath\bin;$env:Path"
$sdkForGradle = $sdkPath -replace '\\', '/'
"sdk.dir=$sdkForGradle" | Set-Content (Join-Path $projectRoot "local.properties")

Push-Location $projectRoot
try {
    & $gradlePath clean assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "La compilation Android a échoué." }
    Write-Host ""
    Write-Host "APK créé :" -ForegroundColor Green
    Write-Host (Join-Path $projectRoot "app\build\outputs\apk\app-debug.apk")
}
finally {
    Pop-Location
}
