param(
    [switch]$Rebuild
)

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

try {
    $arguments = @('compose', 'up')
    if ($Rebuild) { $arguments += '--build' }
    & docker @arguments
} finally {
    Set-Location $PSScriptRoot
}
