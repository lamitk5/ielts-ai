[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$DatabaseUrl,
    [string]$Psql = 'psql'
)

$ErrorActionPreference = 'Stop'
if ($DatabaseUrl -match '(?i)(password|token|key|secret)=') {
    throw 'DatabaseUrl must not contain printable secret query parameters.'
}
Write-Output 'DO NOT EDIT flyway_schema_history: inspection only.'
& $Psql $DatabaseUrl -v ON_ERROR_STOP=1 -c "SELECT installed_rank,version,description,success FROM flyway_schema_history ORDER BY installed_rank;"
if ($LASTEXITCODE -ne 0) { throw 'Flyway history inspection failed' }
& $Psql $DatabaseUrl -v ON_ERROR_STOP=1 -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"
if ($LASTEXITCODE -ne 0) { throw 'Schema inspection failed' }
