param(
    [string]$EnvFile = ".env"
)

$root = Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot\Load-Env.ps1"

$envCandidates = @(
    (Join-Path $root $EnvFile),
    (Join-Path $root "backend\reservas\.env")
)

$resolvedEnvFile = $envCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $resolvedEnvFile) {
    throw "No se encontro archivo .env. Rutas probadas: $($envCandidates -join ', ')"
}

Write-Host "Cargando variables de entorno desde: $resolvedEnvFile"
Set-EnvFromFile -EnvFilePath $resolvedEnvFile

if (-not $env:JAVA_HOME) {
    $defaultJava = "C:\Program Files\Java\jdk-21.0.11"
    if (Test-Path $defaultJava) {
        $env:JAVA_HOME = $defaultJava
    }
}

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME no esta configurado en .env ni detectado automaticamente."
}

$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location "$root\backend\reservas"
try {
    .\mvnw.cmd spring-boot:run
} finally {
    Set-Location "..\.."
}
