$ErrorActionPreference = 'Stop'
$propsFile = Join-Path $PSScriptRoot 'maven-wrapper.properties'
$line = Get-Content $propsFile | Where-Object { $_ -match '^distributionUrl=' } | Select-Object -First 1
if (-not $line) { Write-Error "mvnw: distributionUrl missing in $propsFile"; exit 1 }
$url = ($line -replace '^distributionUrl=', '').Trim()
$name = [IO.Path]::GetFileName($url) -replace '-bin\.zip$', ''
$userHome = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { Join-Path $env:USERPROFILE '.m2' }
$root = Join-Path $userHome 'wrapper\dists'
$mvnHome = Join-Path $root $name
$mvn = Join-Path $mvnHome 'bin\mvn.cmd'
if (-not (Test-Path $mvn)) {
  New-Item -ItemType Directory -Force -Path $root | Out-Null
  $tmp = Join-Path ([IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString())
  New-Item -ItemType Directory -Path $tmp | Out-Null
  try {
    Write-Host "mvnw: downloading $url"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $url -OutFile (Join-Path $tmp 'maven.zip') -UseBasicParsing
    Expand-Archive -Path (Join-Path $tmp 'maven.zip') -DestinationPath (Join-Path $tmp 'x')
    if (Test-Path $mvnHome) { Remove-Item -Recurse -Force $mvnHome }
    Move-Item (Join-Path $tmp "x\$name") $mvnHome
  } finally {
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
  }
}
& $mvn @args
exit $LASTEXITCODE
