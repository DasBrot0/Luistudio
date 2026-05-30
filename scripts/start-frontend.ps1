param(
    [string]$EnvFile = ".env"
)

$root = Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot\Load-Env.ps1"
Set-EnvFromFile -EnvFilePath (Join-Path $root $EnvFile)

if (-not $env:VITE_API_BASE_URL -and $env:API_BASE_URL) {
    $env:VITE_API_BASE_URL = $env:API_BASE_URL
}

Set-Location "$root\frontend\luistudio-app"
try {
    npm.cmd run dev
} finally {
    Set-Location "..\.."
}
