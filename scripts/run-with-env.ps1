param(
    [string]$EnvFile = ".env.local",
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = if ([System.IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile
} else {
    Join-Path $projectRoot $EnvFile
}

if (-not (Test-Path -LiteralPath $envPath)) {
    throw "Soubor s proměnnými nebyl nalezen: $envPath"
}

foreach ($line in Get-Content -LiteralPath $envPath) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith("#")) {
        continue
    }
    if ($trimmed -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') {
        throw "Neplatný řádek v souboru s proměnnými. Očekáván je formát KEY=VALUE."
    }
    [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
}

if ($ValidateOnly) {
    Write-Host "Soubor s proměnnými je syntakticky v pořádku."
    exit 0
}

Push-Location $projectRoot
try {
    & ".\mvnw.cmd" spring-boot:run
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
