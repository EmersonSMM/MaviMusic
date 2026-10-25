param([Parameter(Mandatory=$true)][string]$Msi)
$ErrorActionPreference = 'Stop'
# jpackage 21 normally removes the previous product before its transaction begins.
# Move only that standard action inside the transaction, before copying new files.
$installer = New-Object -ComObject WindowsInstaller.Installer
$db = $installer.GetType().InvokeMember('OpenDatabase', 'InvokeMethod', $null, $installer, @((Resolve-Path -LiteralPath $Msi).Path, 1))
function Query-Sequence($action) {
    $view = $db.GetType().InvokeMember('OpenView', 'InvokeMethod', $null, $db, @("SELECT Sequence FROM InstallExecuteSequence WHERE Action='$action'"))
    $view.GetType().InvokeMember('Execute', 'InvokeMethod', $null, $view, $null) | Out-Null
    $record = $view.GetType().InvokeMember('Fetch', 'InvokeMethod', $null, $view, $null)
    if (-not $record) { throw "Missing MSI action $action" }
    return [int]$record.GetType().InvokeMember('IntegerData', 'GetProperty', $null, $record, @(1))
}
$initialize = Query-Sequence 'InstallInitialize'
$sequence = $initialize + 1
$view = $db.GetType().InvokeMember('OpenView', 'InvokeMethod', $null, $db, @("UPDATE InstallExecuteSequence SET Sequence=$sequence WHERE Action='RemoveExistingProducts'"))
$view.GetType().InvokeMember('Execute', 'InvokeMethod', $null, $view, $null) | Out-Null
$db.GetType().InvokeMember('Commit', 'InvokeMethod', $null, $db, $null) | Out-Null
if ((Query-Sequence 'RemoveExistingProducts') -le $initialize -or $sequence -ge (Query-Sequence 'InstallFiles')) { throw 'Unsafe MSI rollback sequence' }
Write-Output 'MSI upgrade removal is inside the rollback transaction.'
