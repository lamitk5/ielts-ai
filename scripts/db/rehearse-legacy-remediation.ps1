[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$CloneDatabaseUrl,
    [Parameter(Mandatory = $true)][string]$ExpectedCloneDatabase,
    [string]$Psql = 'psql',
    [switch]$REHEARSAL_ONLY
)

$ErrorActionPreference = 'Stop'
if (-not $REHEARSAL_ONLY) { throw 'REHEARSAL_ONLY is required; original DB remediation is never performed by this script.' }
if ($CloneDatabaseUrl -notmatch [regex]::Escape($ExpectedCloneDatabase)) { throw 'Clone URL does not identify the expected clone database.' }
Write-Output 'DO NOT DROP, reset, truncate, or edit flyway_schema_history.'
& $Psql $CloneDatabaseUrl -v ON_ERROR_STOP=1 -c "SELECT current_database(), current_user;"
if ($LASTEXITCODE -ne 0) { throw 'Clone connectivity check failed' }
& $Psql $CloneDatabaseUrl -v ON_ERROR_STOP=1 -c "SELECT installed_rank,version,success FROM flyway_schema_history ORDER BY installed_rank;"
if ($LASTEXITCODE -ne 0) { throw 'Clone Flyway inspection failed' }
& $Psql $CloneDatabaseUrl -v ON_ERROR_STOP=1 -c "SELECT table_name, table_type FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"
if ($LASTEXITCODE -ne 0) { throw 'Clone schema inspection failed' }
Write-Output 'REHEARSAL_ONLY: no migration remediation or original database operation performed.'
