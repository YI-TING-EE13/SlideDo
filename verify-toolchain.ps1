[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$failures = [System.Collections.Generic.List[string]]::new()

function Read-RepositoryFile {
    param([Parameter(Mandatory = $true)][string]$RelativePath)

    $path = Join-Path $root $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $failures.Add("Missing required toolchain file: $RelativePath")
        return ""
    }
    return Get-Content -Raw -LiteralPath $path
}

function Require-Pattern {
    param(
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Content,
        [Parameter(Mandatory = $true)][string]$Pattern,
        [Parameter(Mandatory = $true)][string]$Failure
    )

    if (-not [regex]::IsMatch($Content, $Pattern, [Text.RegularExpressions.RegexOptions]::Multiline)) {
        $failures.Add($Failure)
    }
}

$wrapper = Read-RepositoryFile "android/gradle/wrapper/gradle-wrapper.properties"
$androidBuild = Read-RepositoryFile "android/build.gradle"
$appBuild = Read-RepositoryFile "android/app/build.gradle"
$workflow = Read-RepositoryFile ".github/workflows/ci.yml"
$dependabot = Read-RepositoryFile ".github/dependabot.yml"

Require-Pattern $wrapper '^distributionUrl=https\\://services\.gradle\.org/distributions/gradle-8\.14\.5-all\.zip$' `
    "Gradle wrapper must remain on the supported 8.14.5 all distribution."
Require-Pattern $wrapper '^distributionSha256Sum=62c3769155d7d17ea05084ad498067824c1804568a408a6faa78a5ef95ed67a8$' `
    "Gradle 8.14.5 must use the official distribution SHA-256."
Require-Pattern $androidBuild 'id\s+"com\.android\.application"\s+version\s+"8\.13\.2"' `
    "Android Gradle Plugin must remain on the supported 8.13.2 baseline."
Require-Pattern $appBuild 'compileSdk\s*=\s*36' "Android compileSdk must be 36."
Require-Pattern $appBuild 'targetSdk\s*=\s*36' "Android targetSdk must be 36."
Require-Pattern $appBuild 'JavaVersion\.VERSION_17' "Android Java compatibility must remain 17."
Require-Pattern $appBuild 'androidx\.test:runner:1\.7\.0' "AndroidX Test Runner must be 1.7.0."
Require-Pattern $appBuild 'androidx\.test\.ext:junit:1\.3\.0' "AndroidX Test JUnit must be 1.3.0."
Require-Pattern $appBuild 'androidx\.test\.uiautomator:uiautomator:2\.4\.0' `
    "AndroidX Test UiAutomator must be 2.4.0."
Require-Pattern $appBuild 'warningsAsErrors\s*=\s*true' `
    "Android lint warnings must fail the verification gate."
Require-Pattern $workflow 'java-version:\s*"17"' "GitHub CI must run the same JDK 17 baseline."
Require-Pattern $workflow 'cmdline-tools-version:\s*"14742923"' `
    "GitHub CI must pin Android command-line tools 20.0 (14742923)."
Require-Pattern $workflow 'accept-android-sdk-licenses:\s*"true"' `
    "GitHub CI must pass a valid boolean when accepting Android SDK licenses."
Require-Pattern $workflow 'sdkmanager --install "platforms;android-36" "build-tools;36\.0\.0"' `
    "GitHub CI must install the exact Android platform and build-tools packages."
Require-Pattern $dependabot 'package-ecosystem:\s*"github-actions"' `
    "Dependabot must monitor GitHub Actions."
Require-Pattern $dependabot 'package-ecosystem:\s*"gradle"' `
    "Dependabot must monitor Android Gradle dependencies."

$actionReferences = [regex]::Matches($workflow, '(?m)^\s*uses:\s+([^\s#]+)')
if ($actionReferences.Count -eq 0) {
    $failures.Add("GitHub CI must declare at least one action reference.")
}
foreach ($match in $actionReferences) {
    $reference = $match.Groups[1].Value
    if ($reference -notmatch '@[0-9a-f]{40}$') {
        $failures.Add("GitHub Action is not pinned to a full commit SHA: $reference")
    }
}

if ($failures.Count -gt 0) {
    foreach ($failure in $failures) {
        [Console]::Error.WriteLine("ERROR: $failure")
    }
    exit 1
}

Write-Output "Toolchain contract verified:"
Write-Output "  AGP 8.13.2 / Gradle 8.14.5 / JDK 17"
Write-Output "  Android compile/target SDK 36 / build-tools 36.0.0"
Write-Output "  AndroidX Test Runner 1.7.0 / JUnit 1.3.0 / UiAutomator 2.4.0"
Write-Output "  Android lint warnings are treated as errors"
Write-Output "  Gradle distribution checksum and GitHub Action SHAs pinned"
Write-Output "  Dependabot monitors GitHub Actions and Android Gradle dependencies"
