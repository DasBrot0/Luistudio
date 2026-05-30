function Set-EnvFromFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$EnvFilePath
    )

    if (-not (Test-Path $EnvFilePath)) {
        throw "No se encontro el archivo de entorno: $EnvFilePath"
    }

    Get-Content $EnvFilePath | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }

        $pair = $line -split '=', 2
        if ($pair.Count -ne 2) { return }

        $key = $pair[0].Trim()
        $value = $pair[1].Trim()

        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
    }
}
