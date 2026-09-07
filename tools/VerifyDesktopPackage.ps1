param(
    [Parameter(Mandatory = $true)]
    [string]$PackageDir,
    [string]$ZipPath
)

$ErrorActionPreference = 'Stop'
$allowed = @('SlideDo.jar', 'SlideDo.bat', 'README.txt', 'RELEASE_NOTES.md')
$package = (Resolve-Path -LiteralPath $PackageDir).Path
$items = @(Get-ChildItem -LiteralPath $package -Force)
$files = @($items | Where-Object { -not $_.PSIsContainer })
$directories = @($items | Where-Object { $_.PSIsContainer })

if ($directories.Count -ne 0) {
    throw "Desktop package contains unexpected directories: $($directories.Name -join ', ')"
}
$actual = @($files.Name | Sort-Object)
$expected = @($allowed | Sort-Object)
if (($actual -join "`n") -ne ($expected -join "`n")) {
    throw "Desktop package file whitelist mismatch. Expected: $($expected -join ', '); actual: $($actual -join ', ')"
}

if ($ZipPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ZipPath).Path)
    try {
        $entries = @($zip.Entries | Where-Object { -not $_.FullName.EndsWith('/') })
        $entryNames = @($entries.FullName | Sort-Object)
        if (($entryNames -join "`n") -ne ($expected -join "`n")) {
            throw "Desktop ZIP file whitelist mismatch. Expected: $($expected -join ', '); actual: $($entryNames -join ', ')"
        }
        if (@($zip.Entries | Where-Object { $_.FullName.EndsWith('/') }).Count -ne 0) {
            throw 'Desktop ZIP contains an unexpected directory entry.'
        }
    } finally {
        $zip.Dispose()
    }
}

Write-Output "Desktop package exact whitelist passed: $($expected -join ', ')"
