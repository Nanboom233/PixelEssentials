$ErrorActionPreference = "Stop"

$env:GRADLE_USER_HOME = Join-Path (Get-Location) ".gradle-user-home"
Write-Host "Using GRADLE_USER_HOME: $env:GRADLE_USER_HOME"

Write-Host "[1/2] Running unit tests..."
.\gradlew.bat testDebugUnitTest

Write-Host "[2/2] Building debug APK..."
.\gradlew.bat assembleDebug

$apk = Join-Path $PSScriptRoot "..\\app\\build\\outputs\\apk\\debug\\app-debug.apk"
$resolved = Resolve-Path $apk -ErrorAction SilentlyContinue
if ($resolved) {
    Write-Host "APK: $resolved"
} else {
    Write-Error "APK not found at expected path: $apk"
}
