# The same as scripts/start.sh, for Windows (run it with scripts\start.cmd):
# the database + server and, at the same time, the app on the emulator. Ctrl+C stops the server.

$Root = Split-Path -Parent $PSScriptRoot

Set-Location (Join-Path $Root 'server')
docker compose up -d --wait
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# The app in the background, in the same window, its lines marked with [android]
# so they don't mix up with the server's. -EncodedCommand spares us quoting the path.
$runAndroid = (Join-Path $PSScriptRoot 'run-android.ps1').Replace("'", "''")
$command = "& '$runAndroid' 2>&1 | ForEach-Object { '[android] ' + `$_ }"
$encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))
$android = Start-Process powershell -NoNewWindow -PassThru `
    -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', $encoded

try {
    & .\gradlew.bat bootRun
} finally {
    # The app's build stops together with the server, like on the Mac. The emulator stays open.
    if (-not $android.HasExited) {
        taskkill /PID $android.Id /T /F 2> $null | Out-Null
    }
}
