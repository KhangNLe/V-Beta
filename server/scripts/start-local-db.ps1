#Requires -Version 5.1
$ErrorActionPreference = "Stop"

# Local runtime PostgreSQL for the app (v_beta). Separate from the test DB
# container (vbeta-test-postgres on 55432).
$ContainerName = if ($env:CONTAINER_NAME) { $env:CONTAINER_NAME } else { "vbeta-postgres" }
$VolumeName = if ($env:VOLUME_NAME) { $env:VOLUME_NAME } else { "vbeta-postgres-data" }
$PostgresImage = if ($env:POSTGRES_IMAGE) { $env:POSTGRES_IMAGE } else { "postgres:16" }
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
  throw "Refusing to manage test database 'v_beta_test' here. Use start-local-test-db.ps1."
}

function Invoke-DockerExitCode {
  param([Parameter(Mandatory = $true)][string[]]$DockerArgs)
  $prev = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    & docker @DockerArgs 1>$null 2>$null
    return [int]$LASTEXITCODE
  } finally {
    $ErrorActionPreference = $prev
  }
}

function Get-DockerLines {
  param([Parameter(Mandatory = $true)][string[]]$DockerArgs)
  $prev = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $lines = & docker @DockerArgs 2>$null
    return @(
      $lines |
        ForEach-Object { if ($null -ne $_) { $_.ToString().Trim() } } |
        Where-Object { $_ }
    )
  } finally {
    $ErrorActionPreference = $prev
  }
}

function Invoke-ContainerPsql {
  param(
    [Parameter(Mandatory = $true)][string]$Database,
    [string]$Command,
    [string]$File
  )
  $prev = $ErrorActionPreference
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
    $ErrorActionPreference = $prev
  }
}

function Invoke-ContainerPsqlScalar {
  param(
    [Parameter(Mandatory = $true)][string]$Database,
    [Parameter(Mandatory = $true)][string]$Command
  )
  $prev = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $out = & docker @(
      "exec", "-i",
      "-e", "PGPASSWORD=$SqlPassword",
      $ContainerName,
      "psql", "-U", $SqlUsername, "-d", $Database, "-Atc", $Command
    ) 2>$null
    if ($LASTEXITCODE -ne 0) {
      throw "psql failed ($LASTEXITCODE)"
    }
    return (($out | Out-String).Trim())
  } finally {
    $ErrorActionPreference = $prev
  }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "docker is required but not installed."
}
if ((Invoke-DockerExitCode @("info")) -ne 0) {
  throw "Docker daemon unreachable. Start Docker Desktop, then retry."
}
if (-not (Test-Path -LiteralPath $SchemaFile)) {
  throw "Schema file not found: $SchemaFile"
}

$existing = Get-DockerLines @("ps", "-a", "--filter", "name=^${ContainerName}$", "--format", "{{.Names}}")
if ($existing.Count -eq 0) {
  Write-Host "Creating PostgreSQL container ${ContainerName} on port ${DbPort}..."
  $null = Invoke-DockerExitCode @("volume", "create", $VolumeName)
  $createExit = Invoke-DockerExitCode @(
    "run", "-d",
    "--name", $ContainerName,
    "-e", "POSTGRES_USER=$SqlUsername",
    "-e", "POSTGRES_PASSWORD=$SqlPassword",
    "-e", "POSTGRES_DB=$DbAdminDb",
    "-p", "${DbPort}:5432",
    "-v", "${VolumeName}:/var/lib/postgresql/data",
    $PostgresImage
  )
  if ($createExit -ne 0) {
    throw "Failed to create container (exit $createExit). Is port ${DbPort} free? Free disk / restart Docker Desktop and retry."
  }
} else {
  $running = Get-DockerLines @("ps", "--filter", "name=^${ContainerName}$", "--format", "{{.Names}}")
  if ($running.Count -eq 0) {
    Write-Host "Starting existing PostgreSQL container ${ContainerName}..."
    $startExit = Invoke-DockerExitCode @("start", $ContainerName)
    if ($startExit -ne 0) {
      throw "Failed to start container (exit $startExit)."
    }
  }
}

Write-Host "Waiting for PostgreSQL..."
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
  if ((Invoke-DockerExitCode @("exec", $ContainerName, "pg_isready", "-U", $SqlUsername)) -eq 0) {
    $ready = $true
    break
  }
  Start-Sleep -Seconds 1
}
if (-not $ready) {
  Write-Host "Container logs:"
  $prev = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try { docker logs --tail 40 $ContainerName } finally { $ErrorActionPreference = $prev }
  throw "PostgreSQL not ready in time."
}

$dbExists = Invoke-ContainerPsqlScalar -Database $DbAdminDb -Command "SELECT 1 FROM pg_database WHERE datname='${DbName}';"
if ($dbExists -ne "1") {
  Write-Host "Creating database '${DbName}'..."
  Invoke-ContainerPsql -Database $DbAdminDb -Command "CREATE DATABASE ${DbName};"
}

$schemaReady = Invoke-ContainerPsqlScalar -Database $DbName -Command "SELECT to_regclass('public.gym_role');"
if ([string]::IsNullOrWhiteSpace($schemaReady) -or $schemaReady -eq "null") {
  Write-Host "Applying schema from ${SchemaFile}..."
  Invoke-ContainerPsql -Database $DbName -File $SchemaFile
} else {
  Write-Host "Schema already present in '${DbName}'; skipping apply."
}

Write-Host "Local PostgreSQL runtime DB is ready on ${DbHost}:${DbPort}/${DbName}."
Write-Host "Match server/.env: DB_HOST=${DbHost} DB_PORT=${DbPort} DB_NAME=${DbName} SQL_USERNAME=${SqlUsername}"
