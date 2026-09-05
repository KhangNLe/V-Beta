#Requires -Version 5.1
$ErrorActionPreference='Stop'
$TestDbName=if($env:TEST_DB_NAME){$env:TEST_DB_NAME}else{'v_beta_test'}
$SqlUsername=if($env:TEST_SQL_USERNAME){$env:TEST_SQL_USERNAME}else{'postgres'}
$SqlPassword=if($env:TEST_SQL_PASSWORD){$env:TEST_SQL_PASSWORD}else{'postgres'}
$ContainerName=if($env:CONTAINER_NAME){$env:CONTAINER_NAME}else{'vbeta-test-postgres'}
$SchemaFile=if($env:SCHEMA_FILE){$env:SCHEMA_FILE}else{Join-Path (Split-Path -Parent $PSScriptRoot) 'src\test\resources\db\v_beta_test_schema.sql'}
if($TestDbName -eq 'v_beta'){throw "Refusing to reset 'v_beta'."}
if(-not(Test-Path -LiteralPath $SchemaFile)){throw "Schema not found: $SchemaFile"}
$p=$ErrorActionPreference;$ErrorActionPreference='Continue'
try{$running=docker ps --filter "name=^$ContainerName$" --format '{{.Names}}'}finally{$ErrorActionPreference=$p}
if(-not $running){throw "Container '$ContainerName' is not running. Use start-local-test-db.ps1."}
function Psql([string]$db,[string]$c,[string]$f){$prev=$ErrorActionPreference;$ErrorActionPreference='Continue';try{$b=@('exec','-i','-e',"PGPASSWORD=$SqlPassword",$ContainerName,'psql','-U',$SqlUsername,'-d',$db,'-v','ON_ERROR_STOP=1');if($c){& docker @($b+@('-c',$c))}elseif($f){Get-Content -LiteralPath $f -Raw|& docker @b};if($LASTEXITCODE -ne 0){throw "psql failed ($LASTEXITCODE)"}}finally{$ErrorActionPreference=$prev}}
Write-Host "Resetting '$TestDbName'..."
Psql 'postgres' "DROP DATABASE IF EXISTS $TestDbName;"
Psql 'postgres' "CREATE DATABASE $TestDbName;"
Write-Host "Applying $SchemaFile..."
Psql $TestDbName $null $SchemaFile
Write-Host "PostgreSQL test database '$TestDbName' is ready."
