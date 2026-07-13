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
    corepack enable
    if (-not (Test-Path "node_modules\.bin\vite.cmd")) {
        Write-Host "Instalando dependencias del frontend..."
        pnpm.cmd install --frozen-lockfile
    }
    pnpm.cmd run dev
} finally {
    Set-Location "..\.."
}
