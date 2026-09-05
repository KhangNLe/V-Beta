#Requires -Version 5.1
$ErrorActionPreference = "Stop"

$ContainerName = if ($env:CONTAINER_NAME) { $env:CONTAINER_NAME } else { "vbeta-test-postgres" }
$PostgresImage = if ($env:POSTGRES_IMAGE) { $env:POSTGRES_IMAGE } else { "postgres:16" }
$DbHost = if ($env:TEST_DB_HOST) { $env:TEST_DB_HOST } else { "127.0.0.1" }
$DbPort = if ($env:TEST_DB_PORT) { $env:TEST_DB_PORT } else { "55432" }
$TestDbName = if ($env:TEST_DB_NAME) { $env:TEST_DB_NAME } else { "v_beta_test" }
$SqlUsername = if ($env:TEST_SQL_USERNAME) { $env:TEST_SQL_USERNAME } else { "postgres" }
$SqlPassword = if ($env:TEST_SQL_PASSWORD) { $env:TEST_SQL_PASSWORD } else { "postgres" }
$SchemaFile = Join-Path (Split-Path -Parent $PSScriptRoot) "src\test\resources\db\v_beta_test_schema.sql"

function Invoke-DockerExitCode {
  param([Parameter(Mandatory = $true)][string[]]$DockerArgs)
  $prev = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    # Discard stdout so callers only receive the numeric exit code.
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

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "docker is required but not installed."
}

if ((Invoke-DockerExitCode @("info")) -ne 0) {
  throw "Docker daemon unreachable. Start Docker Desktop, then retry."
}

$existing = Get-DockerLines @("ps", "-a", "--filter", "name=^${ContainerName}$", "--format", "{{.Names}}")
if ($existing.Count -eq 0) {
  Write-Host "Creating PostgreSQL container ${ContainerName}..."
  $createExit = Invoke-DockerExitCode @(
    "run", "-d",
    "--name", $ContainerName,
    "-e", "POSTGRES_USER=$SqlUsername",
    "-e", "POSTGRES_PASSWORD=$SqlPassword",
    "-e", "POSTGRES_DB=postgres",
    "-p", "${DbPort}:5432",
    $PostgresImage
  )
  if ($createExit -ne 0) {
    throw "Failed to create container (exit $createExit). Free disk space / restart Docker Desktop and retry."
  }
} else {
  $running = Get-DockerLines @("ps", "--filter", "name=^${ContainerName}$", "--format", "{{.Names}}")
  if ($running.Count -eq 0) {
    Write-Host "Starting existing PostgreSQL container ${ContainerName}..."
    $startExit = Invoke-DockerExitCode @("start", $ContainerName)
    if ($startExit -ne 0) {
      throw "Failed to start container (exit $startExit). Try: docker rm -f $ContainerName && re-run this script."
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
  throw "PostgreSQL not ready in time. If logs show 'exec format error', run: docker rm -f $ContainerName; docker pull $PostgresImage"
}

if ($TestDbName -eq "v_beta") {
  throw "Refusing to reset 'v_beta'."
}
if (-not (Test-Path -LiteralPath $SchemaFile)) {
  throw "Schema not found: $SchemaFile"
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

Write-Host "Resetting '$TestDbName' on ${DbHost}:${DbPort}..."
Invoke-ContainerPsql -Database "postgres" -Command "DROP DATABASE IF EXISTS ${TestDbName};"
Invoke-ContainerPsql -Database "postgres" -Command "CREATE DATABASE ${TestDbName};"
Write-Host "Applying $SchemaFile..."
Invoke-ContainerPsql -Database $TestDbName -File $SchemaFile
Write-Host "Local PostgreSQL test DB is ready on ${DbHost}:${DbPort}."
