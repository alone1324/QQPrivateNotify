$ErrorActionPreference = 'Stop'
$tasks = @($args)
if ($tasks.Count -eq 0) {
    $tasks = @(':app:assembleDebug', ':app:assembleRelease', ':app:testDebugUnitTest', ':app:lintDebug')
}
$jdkCandidates = @('C:\Program Files\Java\jdk-17', $env:JAVA_HOME,
    'C:\Program Files\Zulu\zulu-21')
$jdk = $jdkCandidates | Where-Object {
    $_ -and (Test-Path (Join-Path $_ 'bin\javac.exe'))
} | Select-Object -First 1
if (-not $jdk) {
    Write-Error 'JDK 17 or 21 is required. Set JAVA_HOME to the JDK directory.'
    exit 1
}
$env:JAVA_HOME = $jdk
$env:ANDROID_USER_HOME = Join-Path $PSScriptRoot '.android'
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-home'
$projectSdk = Join-Path $PSScriptRoot 'android-sdk'
if (Test-Path (Join-Path $projectSdk 'platforms\android-35\android.jar')) {
    $env:ANDROID_HOME = $projectSdk
    $env:ANDROID_SDK_ROOT = $projectSdk
}

$buildExitCode = 1
Push-Location $PSScriptRoot
try {
    $ErrorActionPreference = 'Continue'
    & .\gradlew.bat --no-daemon --console plain @tasks 2>&1 |
        Tee-Object -FilePath (Join-Path $PSScriptRoot 'build.log')
    $buildExitCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($buildExitCode -eq 0) {
        Write-Host "Installable debug APK: $PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
        Write-Host 'The release APK is unsigned and requires your signing key.'
    }
} finally {
    Pop-Location
}
exit $buildExitCode
