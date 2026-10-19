$version = '3.10.0'
$root = Join-Path $env:LOCALAPPDATA 'Programs\Apache\Maven'
$zip = Join-Path $env:TEMP "apache-maven-$version-bin.zip"
$url = "https://dlcdn.apache.org/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip"

New-Item -ItemType Directory -Force -Path $root | Out-Null
Invoke-WebRequest -Uri $url -OutFile $zip
Expand-Archive -Path $zip -DestinationPath $root -Force

$bin = Join-Path $root "apache-maven-$version\bin"
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
if (($userPath -split ';') -notcontains $bin) {
    [Environment]::SetEnvironmentVariable('Path', "$userPath;$bin", 'User')
}
$env:Path = "$bin;$env:Path"

mvn -v