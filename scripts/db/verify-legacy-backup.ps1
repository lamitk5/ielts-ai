[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$BackupFile,
    [string]$PgRestore = 'pg_restore'
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) {
    throw "Backup file does not exist: $BackupFile"
}

& $PgRestore --list --file $BackupFile | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Backup archive could not be inspected by pg_restore"
}
Write-Output "BACKUP_RESTORABLE: $BackupFile"
