# Monta el ZIP de la entrega, que funciona con doble clic en arrancar.bat sin instalar nada en el otro PC:
#   1. compila target\wrapper-legacy.jar (mvn clean package, con los tests),
#   2. crea jre\: un Java reducido con jlink, solo con los modulos que usa el jar,
#   3. copia lo que hay que entregar en dist\SL-P3-07 y lo comprime en dist\SL-P3-07.zip.
# Necesita un JDK 17 o superior y Maven (maven.ps1) desde el ordenador en el que se ejecuta. Uso, con el servidor cerrado:
#   powershell -ExecutionPolicy Bypass -File .\empaquetar.ps1
param([string]$grupo = '07')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# ---- JDK con jlink (el java del PATH suele ser el atajo de Oracle, sin jlink al lado) ----
$candidatos = @()
if ($env:JAVA_HOME) { $candidatos += Join-Path $env:JAVA_HOME 'bin\jlink.exe' }
foreach ($dir in 'Java', 'Eclipse Adoptium', 'Microsoft', 'OpenJDK', 'Zulu', 'Amazon Corretto', 'BellSoft') {
    $candidatos += Get-ChildItem (Join-Path $env:ProgramFiles $dir) -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin\jlink.exe' }
}
$enPath = Get-Command jlink.exe -ErrorAction SilentlyContinue
if ($enPath) { $candidatos += $enPath.Source }
$jlink = $null
foreach ($c in $candidatos) {
    if (-not (Test-Path $c)) { continue }
    $version = (& $c --version).Trim()
    if ([int]($version -split '\.')[0] -ge 17) { $jlink = $c; break }
}
if (-not $jlink) { throw 'No se encuentra un JDK 17 o superior (hace falta jlink). Instala uno o define JAVA_HOME.' }
$jdkBin = Split-Path $jlink
Write-Host "JDK $version : $jdkBin"

# ---- 1. Jar ----
& mvn -q -B clean package
if ($LASTEXITCODE -ne 0) { throw 'mvn clean package ha fallado (cierra el servidor si esta abierto).' }

# ---- 2. Java reducido ----
# Modulos: jdeps --print-module-deps --ignore-missing-deps --multi-release 17 target\wrapper-legacy.jar
# mas jdk.localedata (es, en) para que los numeros salgan con coma decimal como con un Java completo.
$modulos = 'java.base,java.desktop,java.logging,java.prefs,jdk.httpserver,jdk.localedata'
$compresion = if ([int]($version -split '\.')[0] -ge 21) { 'zip-6' } else { '2' }
if (Test-Path jre) { Remove-Item jre -Recurse -Force }
& $jlink --add-modules $modulos --include-locales=es,en --strip-debug --no-header-files --no-man-pages `
    "--compress=$compresion" --output jre
if ($LASTEXITCODE -ne 0) { throw 'jlink ha fallado.' }
# Las DLL de Tesseract necesitan el runtime de Visual C++. El JDK lo trae en bin; se copia si jlink no lo ha copiado,
# porque Windows busca las DLL primero en la carpeta de java.exe y un PC limpio puede no tenerlo instalado.
foreach ($dll in 'msvcp140.dll', 'vcruntime140.dll', 'vcruntime140_1.dll') {
    if (-not (Test-Path "jre\bin\$dll") -and (Test-Path "$jdkBin\$dll")) { Copy-Item "$jdkBin\$dll" 'jre\bin\' }
}
& 'jre\bin\java.exe' -version

# ---- 3. Carpeta y ZIP ----
$nombre = "SL-P3-$grupo"
$destino = Join-Path 'dist' $nombre
if (Test-Path dist) { Remove-Item dist -Recurse -Force }
New-Item -ItemType Directory -Force -Path (Join-Path $destino 'target') | Out-Null
$memoria = (Get-ChildItem -Directory -Filter 'memoria y v*deo').Name  # sin la tilde: PowerShell 5 lee el .ps1 como ANSI
$incluir = @('arrancar.bat', 'config.properties', 'dosbox-wrapper.conf', 'pom.xml', 'maven.ps1', 'empaquetar.ps1',
    'README.md', 'ocr-resultados.md', 'src', 'web', 'tessdata', 'entrenamiento', 'Database-MSDOS', 'capturas', 'jre',
    $memoria)
foreach ($f in $incluir) { Copy-Item $f -Destination $destino -Recurse }
Copy-Item 'target\wrapper-legacy.jar' (Join-Path $destino 'target')
# Lo que se genera al usar la practica y no se entrega
Remove-Item (Join-Path $destino 'entrenamiento\datos') -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $destino 'Database-MSDOS\stdout.txt'), (Join-Path $destino 'Database-MSDOS\stderr.txt') `
    -Force -ErrorAction SilentlyContinue
if (-not (Get-ChildItem (Join-Path $destino $memoria) -Include *.docx, *.pdf -Recurse)) {
    Write-Warning "La carpeta '$memoria' no tiene la memoria (.docx o .pdf): copiala ahi y vuelve a empaquetar."
}

# Entradas con '/' y nombres en UTF-8 (Compress-Archive de PowerShell 5 usa '\', que otros descompresores no entienden).
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::Open((Join-Path $PWD "dist\$nombre.zip"), 'Create', [Text.Encoding]::UTF8)
try {
    $base = (Resolve-Path 'dist').Path.Length + 1
    foreach ($file in Get-ChildItem $destino -Recurse -File) {
        $entrada = $file.FullName.Substring($base).Replace('\', '/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $entrada, 'Optimal') | Out-Null
    }
} finally {
    $zip.Dispose()
}
$mb = [math]::Round((Get-Item "dist\$nombre.zip").Length / 1MB, 1)
Write-Host "Creado dist\$nombre.zip ($mb MB). Se descomprime y se hace doble clic en $nombre\arrancar.bat."
