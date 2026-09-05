#Requires -Version 5.1
$ErrorActionPreference='Stop'
$env:TEST_DB_HOST=if($env:TEST_DB_HOST){$env:TEST_DB_HOST}else{'127.0.0.1'}
$env:TEST_DB_PORT=if($env:TEST_DB_PORT){$env:TEST_DB_PORT}else{'55432'}
$env:TEST_DB_NAME=if($env:TEST_DB_NAME){$env:TEST_DB_NAME}else{'v_beta_test'}
$env:TEST_SQL_USERNAME=if($env:TEST_SQL_USERNAME){$env:TEST_SQL_USERNAME}else{'postgres'}
$env:TEST_SQL_PASSWORD=if($env:TEST_SQL_PASSWORD){$env:TEST_SQL_PASSWORD}else{'postgres'}
& "$PSScriptRoot\start-local-test-db.ps1"
Write-Host "Running backend tests with PostgreSQL '$($env:TEST_DB_NAME)'..."
Push-Location (Split-Path -Parent $PSScriptRoot)
try{& .\mvnw.cmd test;if($LASTEXITCODE -ne 0){throw 'mvnw test failed.'}}finally{Pop-Location}