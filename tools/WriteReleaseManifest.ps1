param(
    [Parameter(Mandatory = $true)]
    [string] $RepoRoot,

    [Parameter(Mandatory = $true)]
    [string] $VersionName,

    [Parameter(Mandatory = $true)]
    [string] $VersionCode,

    [Parameter(Mandatory = $true)]
    [string] $OutputPath
)

$ErrorActionPreference = "Stop"

function Add-ArtifactLine {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Label,

        [Parameter(Mandatory = $true)]
        [string] $RelativePath
    )

    $fullPath = Join-Path $RepoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
        throw "Missing $Label artifact: $fullPath"
    }

    $item = Get-Item -LiteralPath $fullPath
    $stream = [System.IO.File]::OpenRead($fullPath)
    try {
        $sha256 = [System.Security.Cryptography.SHA256]::Create()
        try {
            $hashBytes = $sha256.ComputeHash($stream)
        }
        finally {
            $sha256.Dispose()
        }
    }
    finally {
        $stream.Dispose()
    }

    $hash = [System.BitConverter]::ToString($hashBytes).Replace("-", "").ToLowerInvariant()
    "{0}`t{1}`t{2}`t{3}" -f $Label, $RelativePath.Replace("\", "/"), $item.Length, $hash
}

$outputDirectory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("SlideDo release artifact manifest")
$lines.Add("Version: $VersionName")
$lines.Add("Version code: $VersionCode")
$lines.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')")
$lines.Add("")
$lines.Add("Columns: label, relative_path, bytes, sha256")
$lines.Add((Add-ArtifactLine "android-apk" "android\app\build\outputs\apk\release\app-release.apk"))
$lines.Add((Add-ArtifactLine "android-aab" "android\app\build\outputs\bundle\release\app-release.aab"))
$lines.Add((Add-ArtifactLine "desktop-zip" "dist\desktop\SlideDo-$VersionName.zip"))
$lines.Add((Add-ArtifactLine "feature-graphic" "dist\store-assets\android\$VersionName\feature-graphic-1024x500.png"))
$lines.Add((Add-ArtifactLine "release-notes" "release-notes\$VersionName.md"))
$lines.Add("")
$lines.Add("Note: local CI/release verification may use the temporary signing key.")
$lines.Add("Use only artifacts signed with the real Play upload key for store submission.")

Set-Content -LiteralPath $OutputPath -Value $lines -Encoding ASCII
Write-Host "Release artifact manifest:"
Write-Host "  $OutputPath"
