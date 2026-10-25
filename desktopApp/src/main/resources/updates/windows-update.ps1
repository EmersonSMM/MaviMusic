param(
    [Parameter(Mandatory=$true)][int]$AppPid,
    [Parameter(Mandatory=$true)][string]$Installer,
    [Parameter(Mandatory=$true)][string]$ExpectedHash,
    [Parameter(Mandatory=$true)][string]$InstallRoot,
    [Parameter(Mandatory=$true)][string]$Launcher,
    [Parameter(Mandatory=$true)][string]$StateDirectory,
    [switch]$StartHidden
)
$ErrorActionPreference = 'Stop'
$stage = Split-Path -Parent $Installer
$ready = Join-Path $stage 'ready'
$backup = Join-Path $stage 'previous'
$result = Join-Path $StateDirectory 'result.json'
New-Item -ItemType Directory -Force $StateDirectory | Out-Null
$log = Join-Path $StateDirectory ('update-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
function Write-UpdateLog($text) { Add-Content -LiteralPath $log -Value ((Get-Date -Format o) + ' ' + $text) }
function Save-Result($status, $code) {
    @{ status=$status; code=$code; log=[IO.Path]::GetFileName($log) } | ConvertTo-Json | Set-Content -LiteralPath $result -Encoding UTF8
}
function Start-App {
    if ($StartHidden) { Start-Process -FilePath $Launcher -ArgumentList '--mavimusic-start-hidden' }
    else { Start-Process -FilePath $Launcher }
}
$closed = $false
$backedUp = $false
try {
    $root = [IO.Path]::GetFullPath($InstallRoot).TrimEnd('\')
    if (-not (Test-Path -LiteralPath (Join-Path $root 'app\MaviMusic.cfg')) -or
        -not (Test-Path -LiteralPath (Join-Path $root 'runtime')) -or
        [IO.Path]::GetFullPath($Launcher) -ne (Join-Path $root 'MaviMusic.exe')) { throw 'Unsupported application layout' }
    # Silent upgrades are only supported for the existing per-user MSI installation.
    $installed = @(Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue |
        Where-Object { $_.DisplayName -eq 'MaviMusic' -and (($_.InstallLocation -and $_.InstallLocation.TrimEnd('\') -eq $root) -or $_.DisplayIcon -like "$root\*") })
    if ($installed.Count -eq 0) { throw 'No matching per-user MSI installation; portable and machine-wide builds require manual installation' }
    if ((Get-FileHash -LiteralPath $Installer -Algorithm SHA256).Hash -ne $ExpectedHash) { throw 'Installer hash mismatch' }
    if (Test-Path -LiteralPath $backup) { Remove-Item -LiteralPath $backup -Recurse -Force }
    New-Item -ItemType Directory -Force $backup | Out-Null
    # Only shipped application files, never preferences/library/cache/statistics.
    foreach ($name in @('app','runtime','MaviMusic.exe')) {
        Copy-Item -LiteralPath (Join-Path $root $name) -Destination $backup -Recurse -Force
    }
    $backedUp = $true
    $owned = @{}
    function Remember-AppProcesses {
        $all = @(Get-CimInstance Win32_Process)
        $ids = @($AppPid)
        do {
            $before = $ids.Count
            $ids += @($all | Where-Object { $_.ParentProcessId -in $ids -and $_.ProcessId -notin $ids -and $_.ProcessId -ne $PID } | ForEach-Object { $_.ProcessId })
        } while ($ids.Count -gt $before)
        foreach ($p in $all) {
            if ($p.ProcessId -eq $PID) { continue }
            if ($p.ProcessId -in $ids -or ($p.ExecutablePath -and $p.ExecutablePath.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase))) {
                $owned[[int]$p.ProcessId] = $p.CreationDate
            }
        }
        return ,$all
    }
    $null = Remember-AppProcesses
    New-Item -ItemType File -Path $ready -Force | Out-Null
    # Give the app time to checkpoint and shut down voluntarily.
    $deadline = (Get-Date).AddSeconds(45)
    while ((Get-Process -Id $AppPid -ErrorAction SilentlyContinue) -and (Get-Date) -lt $deadline) { Start-Sleep -Milliseconds 250 }
    $all = Remember-AppProcesses
    foreach ($p in $all) {
        if ($owned.ContainsKey([int]$p.ProcessId) -and $owned[[int]$p.ProcessId] -eq $p.CreationDate) {
            $proc = Get-Process -Id $p.ProcessId -ErrorAction SilentlyContinue
            if ($proc) { $null = $proc.CloseMainWindow() }
        }
    }
    Start-Sleep -Seconds 2
    $deadline = (Get-Date).AddSeconds(15)
    do {
        $remaining = @(Get-CimInstance Win32_Process | Where-Object {
            $owned.ContainsKey([int]$_.ProcessId) -and $owned[[int]$_.ProcessId] -eq $_.CreationDate
        })
        foreach ($p in $remaining) { Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue }
        if ($remaining.Count -gt 0) { Start-Sleep -Milliseconds 250 }
    } while ($remaining.Count -gt 0 -and (Get-Date) -lt $deadline)
    if ($remaining.Count -gt 0) { throw 'Application processes did not stop within the time limit' }
    $closed = $true
    if ((Get-FileHash -LiteralPath $Installer -Algorithm SHA256).Hash -ne $ExpectedHash) { throw 'Installer changed before execution' }
    $msiLog = Join-Path $StateDirectory ('msi-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
    # No elevation request and no generic EXE /S guessing. Windows Installer owns transaction rollback.
    $arguments = '/i "' + $Installer + '" /qn /norestart REBOOT=ReallySuppress INSTALLDIR="' + $root + '" /L*v "' + $msiLog + '"'
    $process = Start-Process -FilePath "$env:SystemRoot\System32\msiexec.exe" -ArgumentList $arguments -PassThru
    if (-not $process.WaitForExit(600000)) { throw 'Installer exceeded the 10 minute limit; recovery requires checking Windows Installer' }
    if ($process.ExitCode -notin @(0,3010)) { throw ('MSI failed with code ' + $process.ExitCode) }
    Save-Result 'success' $process.ExitCode
    Write-UpdateLog 'Silent installation completed'
    Start-App
    Remove-Item -LiteralPath $backup -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $Installer -Force -ErrorAction SilentlyContinue
} catch {
    Write-UpdateLog $_.Exception.Message
    # Do not race an MSI transaction that is still running.
    $canRecover = -not $process -or $process.HasExited
    if ($closed -and $backedUp -and $canRecover) {
        try {
            foreach ($name in @('app','runtime')) {
                & "$env:SystemRoot\System32\robocopy.exe" (Join-Path $backup $name) (Join-Path $root $name) /MIR /R:2 /W:1 /NFL /NDL /NJH /NJS | Out-Null
                if ($LASTEXITCODE -ge 8) { throw 'Backup restoration failed' }
            }
            Copy-Item -LiteralPath (Join-Path $backup 'MaviMusic.exe') -Destination $Launcher -Force
            Save-Result 'recovered' 1
            Start-App
        } catch { Save-Result 'recovery-required' 1; Write-UpdateLog $_.Exception.Message }
    } else { Save-Result 'failed' 1 }
    exit 1
}
