[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [Parameter(Mandatory = $true)][string]$SourceDatabase,
    [Parameter(Mandatory = $true)][string]$CloneDatabase,
    [string]$PgBase = 'pg_basebackup',
    [switch]$AllowCreateClone
)

$ErrorActionPreference = 'Stop'
if (-not $AllowCreateClone) {
    throw 'Clone creation is gated. Re-run with -AllowCreateClone after backup verification.'
}
if ([string]::IsNullOrWhiteSpace($SourceDatabase) -or [string]::IsNullOrWhiteSpace($CloneDatabase) -or $SourceDatabase -eq $CloneDatabase) {
    throw 'Source and clone database names must be explicit and different.'
}

if ($PSCmdlet.ShouldProcess($CloneDatabase, "Create non-destructive clone of $SourceDatabase")) {
    & $PgBase --dbname=$SourceDatabase --pgdata=$CloneDatabase --no-password
    if ($LASTEXITCODE -ne 0) { throw "Clone command failed with exit code $LASTEXITCODE" }
    Write-Output "CLONE_CREATED: $CloneDatabase"
}
