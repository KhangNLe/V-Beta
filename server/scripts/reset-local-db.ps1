#Requires -Version 5.1
$ErrorActionPreference = "Stop"

# Destructive reset of the local runtime database (v_beta).
# Requires the container from start-local-db.ps1 to already be running.
$ContainerName = if ($env:CONTAINER_NAME) { $env:CONTAINER_NAME } else { "vbeta-postgres" }
$DbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }
$DbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "5434" }
$DbName = if ($env:DB_NAME) { $env:DB_NAME } else { "v_beta" }
$SqlUsername = if ($env:SQL_USERNAME) { $env:SQL_USERNAME } else { "postgres" }
$SqlPassword = if ($env:SQL_PASSWORD) { $env:SQL_PASSWORD } else { "postgres" }
$DbAdminDb = if ($env:DB_ADMIN_DB) { $env:DB_ADMIN_DB } else { "postgres" }
$SchemaFile = if ($env:SCHEMA_FILE) {
  $env:SCHEMA_FILE
} else {
  Join-Path (Split-Path -Parent $PSScriptRoot) "src\main\resources\db\pg-v-beta.sql"
}

if ($DbName -eq "v_beta_test") {
  throw "Refusing to reset test database 'v_beta_test' here. Use reset-test-db.ps1."
}
if ($DbName -ne "v_beta" -and $env:ALLOW_NONDEFAULT_DB_RESET -ne "1") {
  throw "Refusing to reset unexpected database '${DbName}'. Set ALLOW_NONDEFAULT_DB_RESET=1 to override."
}
if (-not (Test-Path -LiteralPath $SchemaFile)) {
  throw "Schema file not found: $SchemaFile"
}

$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
  $running = docker ps --filter "name=^${ContainerName}$" --format "{{.Names}}"
} finally {
  $ErrorActionPreference = $prev
}
if (-not $running) {
  throw "Container '${ContainerName}' is not running. Start it with start-local-db.ps1 first."
}

function Invoke-ContainerPsql {
  param(
    [Parameter(Mandatory = $true)][string]$Database,
    [string]$Command,
    [string]$File
  )
  $prevInner = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $base = @(
      "exec", "-i",
      "-e", "PGPASSWORD=$SqlPassword",
      $ContainerName,
      "psql", "-U", $SqlUsername, "-d", $Database, "-v", "ON_ERROR_STOP=1"
    )
    if ($Command) {
      & docker @($base + @("-c", $Command))
    } elseif ($File) {
      Get-Content -LiteralPath $File -Raw | & docker @base
    } else {
      throw "Need -Command or -File"
    }
    if ($LASTEXITCODE -ne 0) {
      throw "psql failed ($LASTEXITCODE)"
    }
  } finally {
    $ErrorActionPreference = $prevInner
  }
}

Write-Host "Resetting PostgreSQL runtime database '${DbName}' on ${DbHost}:${DbPort}..."
Invoke-ContainerPsql -Database $DbAdminDb -Command "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DbName}' AND pid <> pg_backend_pid();"
Invoke-ContainerPsql -Database $DbAdminDb -Command "DROP DATABASE IF EXISTS ${DbName};"
Invoke-ContainerPsql -Database $DbAdminDb -Command "CREATE DATABASE ${DbName};"
Write-Host "Applying schema from ${SchemaFile}..."
Invoke-ContainerPsql -Database $DbName -File $SchemaFile
Write-Host "PostgreSQL runtime database '${DbName}' is ready."