# The same as scripts/run-android.sh, for Windows.
# Builds the app, starts an emulator if no phone or emulator is connected, installs the app and opens it.
# Another emulator than the first one: $env:AVD = "Pixel_8"; scripts\run-android.cmd
#
# No $ErrorActionPreference = 'Stop' on purpose: Windows PowerShell 5.1 would then stop on the first
# line Gradle or adb prints to stderr (even a warning). We check the exit codes ourselves instead.

$Sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
if ($env:ANDROID_SDK_ROOT) { $Sdk = $env:ANDROID_SDK_ROOT }
if ($env:ANDROID_HOME) { $Sdk = $env:ANDROID_HOME }
$Adb = Join-Path $Sdk 'platform-tools\adb.exe'
$Emulator = Join-Path $Sdk 'emulator\emulator.exe'

function Stop-OnFailure {
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if (-not (Test-Path $Adb)) {
    Write-Host "No adb at $Adb. Install the Android SDK in Android Studio or set ANDROID_HOME." -ForegroundColor Red
    exit 1
}

Set-Location (Join-Path $PSScriptRoot '..\android')

# A device counts only when adb sees it as "device" (not "offline" or "unauthorized").
function Test-Device {
    $devices = & $Adb devices | Select-Object -Skip 1 | Where-Object { ($_ -split '\s+')[1] -eq 'device' }
    return @($devices).Count -gt 0
}

if (Test-Device) {
    Write-Host 'Using the connected device.'
    $startedEmulator = $false
} else {
    $Avd = $env:AVD
    if (-not $Avd) {
        # AVD names have no spaces, so this skips any INFO lines the emulator prints.
        $Avd = & $Emulator -list-avds | Where-Object { $_ -match '^\S+$' } | Select-Object -First 1
    }
    if (-not $Avd) {
        Write-Host 'No emulator found. Create one in Android Studio -> Device Manager.' -ForegroundColor Red
        exit 1
    }
    Write-Host "Starting the emulator $Avd..."
    # Its own hidden console, so Ctrl+C here doesn't close it and it stays open when this script ends.
    Start-Process -FilePath $Emulator -ArgumentList '-avd', $Avd -WindowStyle Hidden
    $startedEmulator = $true
}

# Builds while the emulator boots, so we don't wait for both one after the other.
& .\gradlew.bat assembleDebug
Stop-OnFailure

if ($startedEmulator) {
    Write-Host 'Waiting for the emulator to boot...'
    & $Adb wait-for-device
    while ("$(& $Adb shell getprop sys.boot_completed 2> $null)".Trim() -ne '1') {
        Start-Sleep -Seconds 2
    }
}

& .\gradlew.bat installDebug
Stop-OnFailure

# Android 16+ blocks the local network (where our server is) until the user allows it.
# Granting it here saves the question in the app. Older Androids don't have it, so a failure is fine.
& $Adb shell pm grant com.bookworms.app android.permission.ACCESS_LOCAL_NETWORK 2> $null
& $Adb shell am start -n com.bookworms.app/.MainActivity
exit $LASTEXITCODE
