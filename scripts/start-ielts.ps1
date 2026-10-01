param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$ProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
$BackendRoot = Join-Path $ProjectRoot 'backend'
$FrontendRoot = Join-Path $ProjectRoot 'frontend'
$DatabaseContainer = 'ielts-ai-tutor-postgres'
$QaDatabase = 'ielts_ai_overnight_qa_20260927'
$DbUser = 'ielts'
$DbPassword = 'ielts_dev_password'
$PreferredBackendPort = 8081
$PreferredFrontendPort = 5173

function Get-Listener([int]$Port) {
    $connections = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
    foreach ($connection in $connections) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($connection.OwningProcess)" -ErrorAction SilentlyContinue
        if ($null -ne $process) {
            return [pscustomobject]@{
                Port = $Port
                Pid = [int]$connection.OwningProcess
                Name = $process.Name
                CommandLine = $process.CommandLine
            }
        }
    }
    return $null
}

function Test-ProjectProcess($Listener) {
    if ($null -eq $Listener) { return $false }
    $processId = $Listener.Pid
    for ($depth = 0; $depth -lt 6 -and $processId -gt 0; $depth++) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
        if ($null -eq $process) { return $false }
        if ($null -ne $process.CommandLine -and
            $process.CommandLine.IndexOf($ProjectRoot, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
            return $true
        }
        $processId = [int]$process.ParentProcessId
    }
    return $false
}

function Test-Http([string]$Url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 400
    } catch {
        return $false
    }
}

function Wait-Http([string]$Url, [int]$Attempts = 90) {
    for ($i = 0; $i -lt $Attempts; $i++) {
        if (Test-Http $Url) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Wait-PortFree([int]$Port, [int]$Attempts = 20) {
    for ($i = 0; $i -lt $Attempts; $i++) {
        if ($null -eq (Get-Listener $Port)) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Start-Terminal([string]$Title, [string]$WorkingDirectory, [string]$Command) {
    Start-Process -FilePath 'cmd.exe' -WorkingDirectory $WorkingDirectory -ArgumentList @('/k', "title $Title && $Command") | Out-Null
}

function Stop-ProjectListener($Listener) {
    if ($null -eq $Listener -or -not (Test-ProjectProcess $Listener)) { return $false }
    Stop-Process -Id $Listener.Pid -ErrorAction SilentlyContinue
    return Wait-PortFree $Listener.Port
}

function Find-BackendPort {
    foreach ($port in $PreferredBackendPort..($PreferredBackendPort + 3)) {
        $listener = Get-Listener $port
        if ($null -eq $listener) { return [pscustomobject]@{ Port = $port; Reuse = $false } }
        if (Test-ProjectProcess $listener) {
            if (Test-Http "http://127.0.0.1:$port/api/health") {
                return [pscustomobject]@{ Port = $port; Reuse = $true }
            }
            if (-not (Stop-ProjectListener $listener)) {
                throw "Không thể giải phóng backend IELTS stale trên cổng $port."
            }
            return [pscustomobject]@{ Port = $port; Reuse = $false }
        }
    }
    throw 'Không tìm thấy cổng backend an toàn trong dải 8081-8084.'
}

function Find-FrontendPort([int]$BackendPort) {
    foreach ($port in $PreferredFrontendPort..($PreferredFrontendPort + 2)) {
        $listener = Get-Listener $port
        if ($null -eq $listener) { return [pscustomobject]@{ Port = $port; Reuse = $false } }
        if (Test-ProjectProcess $listener) {
            if ($port -eq $PreferredFrontendPort -and (Test-Http "http://127.0.0.1:$port/api/health")) {
                return [pscustomobject]@{ Port = $port; Reuse = $true }
            }
            if (-not (Stop-ProjectListener $listener)) {
                throw "Không thể giải phóng frontend IELTS stale trên cổng $port."
            }
            return [pscustomobject]@{ Port = $port; Reuse = $false }
        }
    }
    throw 'Không tìm thấy cổng frontend an toàn trong dải 5173-5175.'
}

if (-not (Test-Path (Join-Path $BackendRoot 'mvnw.cmd'))) { throw "Không tìm thấy backend trong $BackendRoot" }
if (-not (Test-Path (Join-Path $FrontendRoot 'package.json'))) { throw "Không tìm thấy frontend trong $FrontendRoot" }

Write-Host 'IELTS AI Tutor local launcher'
Write-Host "Project: $ProjectRoot"
Write-Host "QA database: $QaDatabase"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker CLI không khả dụng; không thể xác nhận PostgreSQL QA.'
}

$containerState = (& docker inspect -f '{{.State.Running}}' $DatabaseContainer 2>$null).Trim()
if ($containerState -eq 'false') {
    Write-Host 'Starting existing PostgreSQL container...'
    & docker start $DatabaseContainer | Out-Null
} elseif ($containerState -ne 'true') {
    throw "Không tìm thấy container PostgreSQL đã được xác minh: $DatabaseContainer"
}

if (-not (Test-NetConnection -ComputerName 127.0.0.1 -Port 5432 -InformationLevel Quiet -WarningAction SilentlyContinue)) {
    throw 'PostgreSQL QA chưa lắng nghe trên 127.0.0.1:5432.'
}
Write-Host 'PostgreSQL: READY'

$backend = Find-BackendPort
if (-not $backend.Reuse) {
    $mvnw = Join-Path $BackendRoot 'mvnw.cmd'
    $backendCommand = "@echo off&&set PORT=$($backend.Port)&&set RAG_DB_HOST=127.0.0.1&&set RAG_DB_PORT=5432&&set RAG_DB_NAME=$QaDatabase&&set RAG_DB_USER=$DbUser&&set RAG_DB_PASSWORD=$DbPassword&&$mvnw spring-boot:run"
    Write-Host "Starting IELTS backend on 127.0.0.1:$($backend.Port)..."
    Start-Terminal 'IELTS Backend' $BackendRoot $backendCommand
    if (-not (Wait-Http "http://127.0.0.1:$($backend.Port)/api/health")) {
        throw "Backend không sẵn sàng trên cổng $($backend.Port)."
    }
} else {
    Write-Host "Reusing verified IELTS backend on 127.0.0.1:$($backend.Port)."
}

$frontend = Find-FrontendPort $backend.Port
if (-not $frontend.Reuse) {
    $frontendCommand = "@set VITE_API_PROXY_TARGET=http://127.0.0.1:$($backend.Port) && @npm.cmd run dev -- --host 127.0.0.1 --port $($frontend.Port)"
    Write-Host "Starting IELTS frontend on 127.0.0.1:$($frontend.Port)..."
    Start-Terminal 'IELTS Frontend' $FrontendRoot $frontendCommand
    if (-not (Wait-Http "http://127.0.0.1:$($frontend.Port)/api/health")) {
        throw "Frontend/proxy không sẵn sàng trên cổng $($frontend.Port)."
    }
} else {
    Write-Host "Reusing verified IELTS frontend on 127.0.0.1:$($frontend.Port)."
}

$frontendUrl = "http://127.0.0.1:$($frontend.Port)/"
Write-Host "Frontend: $frontendUrl"
Write-Host "Backend: http://127.0.0.1:$($backend.Port)/"
Start-Process $frontendUrl | Out-Null
Write-Host 'Browser opened. Backend and frontend terminals remain running.'
