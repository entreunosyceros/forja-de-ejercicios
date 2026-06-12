# Desarrollado por entreunosyceros - 2026
# Instalador Forja de ejercicios (Windows / PowerShell 5.1+)
#
# Formas de ejecutarlo en Windows:
#   1) RECOMENDADA: doble clic en install.bat (o ".\install.bat" en cmd/PowerShell).
#      No hay que tocar la politica de ejecucion: el .bat ya aplica -ExecutionPolicy Bypass.
#   2) PowerShell directo, en una sola linea (sin cambiar la politica global):
#        powershell -NoProfile -ExecutionPolicy Bypass -File .\install.ps1
#   3) PowerShell directo con ".\install.ps1": ANTES hay que habilitar la ejecucion
#      en esa sesion, porque por defecto Windows bloquea los scripts .ps1:
#        Set-ExecutionPolicy -Scope Process Bypass
#        .\install.ps1
#
# Requisitos que el script intenta instalar automaticamente:
#   - JDK 21 (winget), Maven 3.9+ (descarga Apache / choco / winget), Python 3.10+ (winget)
# Opcionales: Docker Desktop, dependencias pip, clave Gemini

$ErrorActionPreference = "Continue"
$Raiz = $PSScriptRoot
Set-Location -LiteralPath $Raiz

# ---------------------------------------------------------------------------
# Utilidades (solo ASCII: evita errores de parseo en Windows PowerShell 5.1)
# ---------------------------------------------------------------------------

function Write-Info {
    param([string]$Message)
    Write-Host "[*] $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[!] $Message" -ForegroundColor Yellow
}

function Write-Err {
    param([string]$Message)
    Write-Host "[X] $Message" -ForegroundColor Red
}

function Refresh-SessionPath {
    $machine = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    $user = [System.Environment]::GetEnvironmentVariable("Path", "User")
    if ($machine -and $user) {
        $env:Path = "$machine;$user"
    }
}

function Format-ProcessArguments {
    param([string[]]$ArgumentList)
    $quoted = @()
    foreach ($arg in $ArgumentList) {
        if ($null -eq $arg) { continue }
        if ($arg -match '[\s"]') {
            $quoted += ('"' + ($arg -replace '"', '""') + '"')
        } else {
            $quoted += $arg
        }
    }
    return ($quoted -join " ")
}

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [string[]]$ArgumentList = @(),
        [string]$WorkingDirectory = $null
    )
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    if ($WorkingDirectory -and (Test-Path -LiteralPath $WorkingDirectory)) {
        $psi.WorkingDirectory = $WorkingDirectory
    }
    $ext = [System.IO.Path]::GetExtension($FilePath).ToLowerInvariant()
    if ($ext -eq ".cmd" -or $ext -eq ".bat") {
        # mvn.cmd debe ejecutarse via cmd.exe para heredar el directorio de trabajo
        $psi.FileName = $env:ComSpec
        $psi.Arguments = "/c `"$FilePath`" $(Format-ProcessArguments $ArgumentList)"
    } else {
        $psi.FileName = $FilePath
        $psi.Arguments = Format-ProcessArguments $ArgumentList
    }
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    $stdout = $proc.StandardOutput.ReadToEnd()
    $stderr = $proc.StandardError.ReadToEnd()
    $proc.WaitForExit()
    return [PSCustomObject]@{
        ExitCode = $proc.ExitCode
        Output   = ($stdout + $stderr).Trim()
        TimedOut = $false
    }
}

function Invoke-NativeWithTimeout {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [string[]]$ArgumentList = @(),
        [string]$WorkingDirectory = $null,
        [int]$TimeoutSeconds = 15
    )
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    if ($WorkingDirectory -and (Test-Path -LiteralPath $WorkingDirectory)) {
        $psi.WorkingDirectory = $WorkingDirectory
    }
    $ext = [System.IO.Path]::GetExtension($FilePath).ToLowerInvariant()
    if ($ext -eq ".cmd" -or $ext -eq ".bat") {
        $psi.FileName = $env:ComSpec
        $psi.Arguments = "/c `"$FilePath`" $(Format-ProcessArguments $ArgumentList)"
    } else {
        $psi.FileName = $FilePath
        $psi.Arguments = Format-ProcessArguments $ArgumentList
    }
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    $timedOut = -not $proc.WaitForExit([Math]::Max(1, $TimeoutSeconds) * 1000)
    if ($timedOut) {
        try { $proc.Kill() } catch { }
        return [PSCustomObject]@{
            ExitCode = -1
            Output   = "timeout tras ${TimeoutSeconds}s"
            TimedOut = $true
        }
    }
    $stdout = $proc.StandardOutput.ReadToEnd()
    $stderr = $proc.StandardError.ReadToEnd()
    return [PSCustomObject]@{
        ExitCode = $proc.ExitCode
        Output   = ($stdout + $stderr).Trim()
        TimedOut = $false
    }
}

function Get-ToolPath {
    param([string]$Name)
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }
    return $null
}

function Test-WingetAvailable {
    return ($null -ne (Get-ToolPath "winget"))
}

function Add-ToUserPath {
    param([string]$Directory)
    if (-not $Directory -or -not (Test-Path -LiteralPath $Directory)) {
        return
    }
    $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
    if (-not $userPath) { $userPath = "" }
    if ($userPath -notlike "*$Directory*") {
        [System.Environment]::SetEnvironmentVariable("Path", "$userPath;$Directory", "User")
    }
    if ($env:Path -notlike "*$Directory*") {
        $env:Path = "$env:Path;$Directory"
    }
}

function Resolve-MavenExecutable {
    $mvn = Get-ToolPath "mvn"
    if ($mvn) {
        return $mvn
    }
    $toolsDir = Join-Path $Raiz "tools"
    if (Test-Path -LiteralPath $toolsDir) {
        $found = Get-ChildItem -Path $toolsDir -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($found) {
            return $found.FullName
        }
    }
    return $null
}

function Install-MavenFromZip {
    $version = "3.9.16"
    $folderName = "apache-maven-$version"
    $toolsDir = Join-Path $Raiz "tools"
    $targetDir = Join-Path $toolsDir $folderName
    $mvnCmd = Join-Path $targetDir "bin\mvn.cmd"
    $binDir = Join-Path $targetDir "bin"

    if (Test-Path -LiteralPath $mvnCmd) {
        Add-ToUserPath $binDir
        return $mvnCmd
    }

    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $zipName = "$folderName-bin.zip"
    $zipFile = Join-Path $env:TEMP $zipName
    $urls = @(
        "https://dlcdn.apache.org/maven/maven-3/$version/binaries/$zipName",
        "https://archive.apache.org/dist/maven/maven-3/$version/binaries/$zipName"
    )

    $downloaded = $false
    foreach ($url in $urls) {
        Write-Info "Descargando Maven $version..."
        Write-Host "  $url"
        try {
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
            Invoke-WebRequest -Uri $url -OutFile $zipFile -UseBasicParsing
            $downloaded = $true
            break
        } catch {
            Write-Warn "No se pudo descargar desde este espejo."
        }
    }
    if (-not $downloaded) {
        return $null
    }

    try {
        if (Test-Path -LiteralPath $targetDir) {
            Remove-Item -LiteralPath $targetDir -Recurse -Force
        }
        Expand-Archive -LiteralPath $zipFile -DestinationPath $toolsDir -Force
    } catch {
        Write-Warn "No se pudo extraer Maven: $($_.Exception.Message)"
        return $null
    } finally {
        Remove-Item -LiteralPath $zipFile -Force -ErrorAction SilentlyContinue
    }

    if (Test-Path -LiteralPath $mvnCmd) {
        Add-ToUserPath $binDir
        Write-Ok "Maven instalado en tools\$folderName"
        return $mvnCmd
    }
    return $null
}

function Install-MavenViaChoco {
    $choco = Get-ToolPath "choco"
    if (-not $choco) {
        return $null
    }
    Write-Info "Instalando Maven con Chocolatey..."
    $result = Invoke-Native -FilePath $choco -ArgumentList @("install", "maven", "-y", "--no-progress")
    if ($result.ExitCode -eq 0) {
        Refresh-SessionPath
        return (Resolve-MavenExecutable)
    }
    Write-Warn "Chocolatey no pudo instalar Maven."
    return $null
}

function Install-MavenViaWinget {
    # winget a veces no tiene Apache Maven o falla en modo silencioso
    $ids = @("Apache.Maven", "Apache.maven")
    foreach ($id in $ids) {
        if (Install-WingetPackage -PackageId $id -DisplayName "Apache Maven ($id)") {
            $mvn = Resolve-MavenExecutable
            if ($mvn) {
                return $mvn
            }
        }
    }
    return $null
}

function Test-IsAdminSession {
    $principal = New-Object Security.Principal.WindowsPrincipal(
        [Security.Principal.WindowsIdentity]::GetCurrent()
    )
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Install-WingetPackage {
    param(
        [string]$PackageId,
        [string]$DisplayName,
        [switch]$IgnoreSecurityHash
    )
    if (-not (Test-WingetAvailable)) {
        Write-Warn "winget no esta disponible. No se puede instalar $DisplayName automaticamente."
        return $false
    }
    Write-Info "Instalando $DisplayName con winget ($PackageId)..."
    $args = @(
        "install", "--id", $PackageId,
        "--accept-package-agreements",
        "--accept-source-agreements",
        "--disable-interactivity",
        "--silent"
    )
    if ($IgnoreSecurityHash) {
        $args += "--ignore-security-hash"
    }
    $result = Invoke-Native -FilePath (Get-ToolPath "winget") -ArgumentList $args
    if ($result.ExitCode -eq 0 -or $result.ExitCode -eq -1978335189) {
        # 0 = ok; -1978335189 = ya instalado (winget)
        Write-Ok "$DisplayName instalado o ya presente."
        Refresh-SessionPath
        return $true
    }
    Write-Warn "winget no pudo instalar $DisplayName. Codigo: $($result.ExitCode)"
    if ($result.Output) {
        Write-Host $result.Output
    }
    return $false
}

function Install-DockerDesktop {
    if (-not (Test-WingetAvailable)) {
        return $false
    }
    if (Install-WingetPackage -PackageId "Docker.DockerDesktop" -DisplayName "Docker Desktop") {
        return $true
    }
    Write-Warn "Docker Desktop: winget fallo (a menudo por hash del instalador desactualizado)."
    Write-Host "  Descarga manual: https://www.docker.com/products/docker-desktop/"
    if (Test-IsAdminSession) {
        Write-Host "  PowerShell como admin no puede usar --ignore-security-hash."
        Write-Host "  Abre PowerShell normal y ejecuta:"
        Write-Host "    winget install Docker.DockerDesktop --ignore-security-hash"
    } elseif (Preguntar-Si "Reintentar instalacion ignorando hash? (solo si confias en la descarga)") {
        if (Install-WingetPackage -PackageId "Docker.DockerDesktop" -DisplayName "Docker Desktop" -IgnoreSecurityHash) {
            return $true
        }
    }
    if (Preguntar-Si "Abrir pagina de descarga de Docker en el navegador?") {
        Start-Process "https://www.docker.com/products/docker-desktop/"
    }
    return $false
}

function Start-ForjaApplication {
    param(
        [string]$JavaPath = $null,
        [string]$MavenPath = $null
    )
    $jar = Join-Path $Raiz "web\target\forjaexamenes-web-1.0.0.jar"
    if (Test-Path -LiteralPath $jar) {
        if (-not $JavaPath) {
            $JavaPath = Get-ToolPath "java"
        }
        if (-not $JavaPath) {
            Write-Err "Java no encontrado en PATH."
            return $false
        }
        Write-Info "Arrancando Forja de ejercicios..."
        Write-Host "  http://localhost:8080"
        Write-Host "  Detener: Ctrl+C"
        Write-Host ""
        $env:FORJAEXAMENES_RAIZ = $Raiz
        & $JavaPath -jar $jar
        return ($LASTEXITCODE -eq 0)
    }

    if (-not $MavenPath) {
        $MavenPath = Resolve-MavenExecutable
    }
    $webDir = Join-Path $Raiz "web"
    $pomFile = Join-Path $webDir "pom.xml"
    if (-not $MavenPath -or -not (Test-Path -LiteralPath $pomFile)) {
        Write-Err "No hay JAR compilado. Vuelve a ejecutar install.bat (o install.ps1)."
        return $false
    }
    Write-Info "Arrancando con Maven spring-boot:run..."
    Write-Host "  http://localhost:8080"
    Write-Host "  Detener: Ctrl+C"
    Write-Host ""
    $env:FORJAEXAMENES_RAIZ = $Raiz
    $run = Invoke-Native -FilePath $MavenPath -WorkingDirectory $webDir -ArgumentList @(
        "-f", $pomFile, "spring-boot:run"
    )
    if ($run.ExitCode -ne 0 -and $run.Output) {
        Write-Host $run.Output
    }
    return ($run.ExitCode -eq 0)
}

function Resolve-PythonExecutable {
    foreach ($name in @("python", "python3")) {
        $path = Get-ToolPath $name
        if (-not $path) { continue }
        $res = Invoke-Native -FilePath $path -ArgumentList @("--version")
        if ($res.Output -match "Python 3\.(1[0-9]|[2-9][0-9])") {
            return $path
        }
    }
    $pyLauncher = Get-ToolPath "py"
    if ($pyLauncher) {
        $res = Invoke-Native -FilePath $pyLauncher -ArgumentList @("-3", "-c", "import sys; print(sys.executable)")
        $lines = $res.Output -split "`n"
        foreach ($line in $lines) {
            $trim = $line.Trim()
            if ($trim -match "\\python" -and (Test-Path -LiteralPath $trim)) {
                return $trim
            }
        }
    }
    return $null
}

function Ensure-Java {
    param([System.Collections.ArrayList]$ManualSteps)
    $javaPath = Get-ToolPath "java"
    if ($javaPath) {
        $info = Invoke-Native -FilePath $javaPath -ArgumentList @("-version")
        if ($info.Output -match 'version "21') {
            Write-Ok "Java JDK 21: $($info.Output.Split([char]10)[0])"
            return $true
        }
        Write-Warn "Java detectado pero no es JDK 21. Se intentara instalar Temurin 21."
    }
    if (Preguntar-Si "Instalar JDK 21 automaticamente con winget?") {
        Install-WingetPackage -PackageId "EclipseAdoptium.Temurin.21.JDK" -DisplayName "JDK 21 (Temurin)" | Out-Null
        Refresh-SessionPath
    }
    $javaPath = Get-ToolPath "java"
    if ($javaPath) {
        $info = Invoke-Native -FilePath $javaPath -ArgumentList @("-version")
        if ($info.Output -match 'version "21') {
            Write-Ok "Java JDK 21 listo."
            return $true
        }
    }
    [void]$ManualSteps.Add(@"
[OBLIGATORIO] JDK 21
  - Descarga: https://adoptium.net/temurin/releases/?version=21
  - O en PowerShell (admin): winget install EclipseAdoptium.Temurin.21.JDK
  - Tras instalar, cierra y vuelve a abrir PowerShell.
"@)
    return $false
}

function Ensure-Maven {
    param([System.Collections.ArrayList]$ManualSteps)

    $mvnPath = Resolve-MavenExecutable
    if ($mvnPath) {
        $info = Invoke-Native -FilePath $mvnPath -ArgumentList @("-version")
        Write-Ok "Maven: $($info.Output.Split([char]10)[0])"
        return $mvnPath
    }

    if (Preguntar-Si "Instalar Maven automaticamente?") {
        Write-Info "Maven: descarga directa desde Apache (metodo mas fiable en Windows)..."
        $mvnPath = Install-MavenFromZip
        if (-not $mvnPath) {
            Write-Info "Maven: probando winget..."
            $mvnPath = Install-MavenViaWinget
        }
        if (-not $mvnPath) {
            Write-Info "Maven: probando Chocolatey..."
            $mvnPath = Install-MavenViaChoco
        }
        if ($mvnPath) {
            $info = Invoke-Native -FilePath $mvnPath -ArgumentList @("-version")
            Write-Ok "Maven listo: $($info.Output.Split([char]10)[0])"
            return $mvnPath
        }
        Write-Warn "La instalacion automatica de Maven no termino correctamente."
    }

    [void]$ManualSteps.Add(@"
[OBLIGATORIO] Apache Maven 3.8+
  Opcion A (recomendada): vuelve a ejecutar install.bat y acepta instalar Maven.
           Se descargara en examenforge\tools\apache-maven-3.9.16\
  Opcion B: choco install maven
  Opcion C: https://maven.apache.org/download.cgi
           Descomprime y anade la carpeta bin al PATH del usuario.
"@)
    return $null
}

function Ensure-Python {
    param([System.Collections.ArrayList]$ManualSteps)
    $pythonExe = Resolve-PythonExecutable
    if ($pythonExe) {
        $info = Invoke-Native -FilePath $pythonExe -ArgumentList @("--version")
        Write-Ok "Python: $($info.Output) ($pythonExe)"
        return $pythonExe
    }
    if (Preguntar-Si "Instalar Python 3.12 automaticamente con winget?") {
        Install-WingetPackage -PackageId "Python.Python.3.12" -DisplayName "Python 3.12" | Out-Null
        Refresh-SessionPath
    }
    $pythonExe = Resolve-PythonExecutable
    if ($pythonExe) {
        Write-Ok "Python listo: $pythonExe"
        return $pythonExe
    }
    [void]$ManualSteps.Add(@"
[OBLIGATORIO] Python 3.10 o superior
  - Descarga: https://www.python.org/downloads/
  - O: winget install Python.Python.3.12
  - IMPORTANTE: marca "Add python.exe to PATH" en el instalador.
"@)
    return $null
}

function Preguntar-Si {
    param([string]$Texto)
    $r = Read-Host "$Texto [s/N]"
    return ($r -match "^(s|si|S|Si|y|Y|yes|Yes)$")
}

function Format-EnvValue {
    param([string]$Valor)
    if ($null -eq $Valor) { return "" }
    # En .env las barras invertidas se interpretan como escape; usar / en rutas Windows
    if ($Valor -match '^[A-Za-z]:\\') {
        $Valor = $Valor -replace '\\', '/'
    }
    if ($Valor -match '[\s#=]') {
        return '"' + ($Valor -replace '"', '\"') + '"'
    }
    return $Valor
}

function Actualizar-Env {
    param([string]$Clave, [string]$Valor)
    $envFile = Join-Path $Raiz ".env"
    $linea = "$Clave=$(Format-EnvValue $Valor)"
    if (Test-Path -LiteralPath $envFile) {
        $contenido = Get-Content -LiteralPath $envFile -Raw -Encoding UTF8
        $escaped = [regex]::Escape($Clave)
        if ($contenido -match "(?m)^$escaped=") {
            $contenido = $contenido -replace "(?m)^$escaped=.*", $linea
        } else {
            if (-not $contenido.EndsWith("`n")) { $contenido += "`n" }
            $contenido += "$linea`n"
        }
        $utf8 = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($envFile, $contenido, $utf8)
    } else {
        $utf8 = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($envFile, "$linea`n", $utf8)
    }
}

function Get-WslExecutable {
    $wsl = Get-ToolPath "wsl.exe"
    if ($wsl) {
        return $wsl
    }
    $sistema = Join-Path $env:SystemRoot "System32\wsl.exe"
    if (Test-Path -LiteralPath $sistema) {
        return $sistema
    }
    return $null
}

function Test-WslRegistryHint {
    $lxss = "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Lxss"
    if (-not (Test-Path -LiteralPath $lxss)) {
        return $false
    }
    $subs = Get-ChildItem -LiteralPath $lxss -ErrorAction SilentlyContinue
    return ($null -ne $subs -and $subs.Count -gt 0)
}

function Test-WslInstalled {
    $wsl = Get-WslExecutable
    if (-not $wsl) {
        return $false
    }
    if (Test-WslRegistryHint) {
        return $true
    }
    Write-Info "Comprobando WSL (max. 12 s; en equipos sin WSL puede tardar un poco)..."
    $res = Invoke-NativeWithTimeout -FilePath $wsl -ArgumentList @("--version") -TimeoutSeconds 12
    if ($res.TimedOut) {
        Write-Warn "WSL no respondio a tiempo (normal si no esta instalado)."
        return $false
    }
    return ($res.ExitCode -eq 0)
}

function Show-WslInstallGuide {
    Write-Host ""
    Write-Host "  Docker Desktop en Windows necesita WSL 2."
    Write-Host "  Pasos:"
    Write-Host "    1. PowerShell como ADMINISTRADOR"
    Write-Host "    2. wsl --install"
    Write-Host "    3. Reinicia el PC"
    Write-Host "    4. Abre Docker Desktop y espera a que este listo"
    Write-Host "  Guia: https://aka.ms/wslinstall"
    Write-Host ""
}

function Test-DockerDaemonReady {
    param(
        [string]$DockerPath,
        [int]$TimeoutSeconds = 10,
        [switch]$Quiet
    )
    if (-not $DockerPath) {
        return $false
    }
    if (-not $Quiet) {
        Write-Info "Comprobando motor Docker (max. ${TimeoutSeconds} s)..."
    }
    $res = Invoke-NativeWithTimeout -FilePath $DockerPath -ArgumentList @("info") -TimeoutSeconds $TimeoutSeconds
    if ($res.TimedOut -and -not $Quiet) {
        Write-Warn "El motor Docker no respondio a tiempo (Docker Desktop parado o arrancando)."
    }
    return ($res.ExitCode -eq 0)
}

function Start-DockerDesktopApp {
    $candidatos = @(
        "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe",
        "$env:LOCALAPPDATA\Programs\Docker\Docker\Docker Desktop.exe",
        "$env:LOCALAPPDATA\Docker\Docker Desktop.exe"
    )
    foreach ($ruta in $candidatos) {
        if (Test-Path -LiteralPath $ruta) {
            Start-Process -FilePath $ruta | Out-Null
            return $true
        }
    }
    return $false
}

function Wait-DockerDaemonReady {
    param(
        [string]$DockerPath,
        [int]$MaxSeconds = 120
    )
    $transcurrido = 0
    while ($transcurrido -lt $MaxSeconds) {
        $pct = [int](100 * $transcurrido / $MaxSeconds)
        Write-Progress -Activity "Esperando Docker Desktop" `
            -Status "Comprobando motor... ($transcurrido s / $MaxSeconds s)" `
            -PercentComplete $pct
        if (Test-DockerDaemonReady -DockerPath $DockerPath -TimeoutSeconds 5 -Quiet) {
            Write-Progress -Activity "Esperando Docker Desktop" -Completed
            return $true
        }
        Start-Sleep -Seconds 3
        $transcurrido += 3
    }
    Write-Progress -Activity "Esperando Docker Desktop" -Completed
    return $false
}

function Ensure-DockerDaemonReady {
    param([string]$DockerPath)
    if (Test-DockerDaemonReady -DockerPath $DockerPath) {
        return $true
    }
    Write-Warn "Docker CLI instalado, pero el motor no responde (Docker Desktop parado o aun arrancando)."
    Write-Host "  En Windows hace falta Docker Desktop en marcha antes de docker compose."
    if (-not (Preguntar-Si "Intentar abrir Docker Desktop y esperar hasta 2 minutos?")) {
        return $false
    }
    if (-not (Start-DockerDesktopApp)) {
        Write-Warn "No se encontro Docker Desktop. Abrelo desde el menu Inicio."
        return $false
    }
    Write-Info "Esperando motor Docker..."
    if (Wait-DockerDaemonReady -DockerPath $DockerPath) {
        Write-Ok "Motor Docker listo."
        return $true
    }
    Write-Warn "Docker Desktop aun no responde. Puedes levantar el contenedor mas tarde."
    return $false
}

function Invoke-DockerCompose {
    param([string[]]$ComposeArgs)
    $docker = Get-ToolPath "docker"
    if (-not $docker) {
        throw "Docker no encontrado"
    }
    if (-not (Test-DockerDaemonReady -DockerPath $docker)) {
        throw "El motor Docker no esta en marcha. Abre Docker Desktop y espera a que este listo."
    }
    Write-Info "Construyendo y levantando contenedor (primera vez: varios minutos; se muestra la salida de Docker)..."
    Write-Host ""
    Push-Location -LiteralPath $Raiz
    try {
        $composeOk = $false
        $composeArgsFlat = @("compose") + $ComposeArgs
        & $docker @composeArgsFlat 2>&1 | ForEach-Object {
            $linea = $_.ToString()
            if ($linea) {
                Write-Host "  $linea"
            }
        }
        if ($LASTEXITCODE -eq 0) {
            $composeOk = $true
        }
        if (-not $composeOk) {
            $legacy = Get-ToolPath "docker-compose"
            if ($legacy) {
                Write-Info "Reintentando con docker-compose..."
                & $legacy @ComposeArgs 2>&1 | ForEach-Object {
                    $linea = $_.ToString()
                    if ($linea) {
                        Write-Host "  $linea"
                    }
                }
                $composeOk = ($LASTEXITCODE -eq 0)
            }
        }
        if (-not $composeOk) {
            throw "docker compose fallo (codigo $LASTEXITCODE)"
        }
    } finally {
        Pop-Location
    }
    Write-Host ""
}

function Show-ManualChecklist {
    param([System.Collections.ArrayList]$Steps, [string[]]$OptionalSteps)
    Write-Host ""
    Write-Host "=========================================================="
    Write-Host "  FALTA INSTALAR MANUALMENTE"
    Write-Host "=========================================================="
    Write-Host ""
    foreach ($step in $Steps) {
        Write-Host $step
        Write-Host ""
    }
    if ($OptionalSteps.Count -gt 0) {
        Write-Host "-- Opcional (la web funciona sin esto) --"
        Write-Host ""
        foreach ($step in $OptionalSteps) {
            Write-Host $step
            Write-Host ""
        }
    }
    Write-Host "Cuando termines, vuelve a ejecutar: .\install.bat"
    Write-Host ""
}

# ---------------------------------------------------------------------------
# Inicio
# ---------------------------------------------------------------------------

Write-Host ""
Write-Host "=========================================================="
Write-Host "  Forja de ejercicios - instalacion (Windows)"
Write-Host "=========================================================="
Write-Host ""

if (-not (Test-WingetAvailable)) {
    Write-Warn "winget no detectado. Instalacion automatica limitada."
    Write-Host "  Instala 'App Installer' desde Microsoft Store para usar winget."
    Write-Host ""
}

$manualRequired = New-Object System.Collections.ArrayList
$manualOptional = New-Object System.Collections.ArrayList

Write-Info "Comprobando e instalando requisitos obligatorios..."

$javaOk = Ensure-Java -ManualSteps $manualRequired
$mvnPath = Ensure-Maven -ManualSteps $manualRequired
$pythonExe = Ensure-Python -ManualSteps $manualRequired

if (-not $javaOk -or -not $mvnPath -or -not $pythonExe) {
    Show-ManualChecklist -Steps $manualRequired -OptionalSteps @()
    exit 1
}

Actualizar-Env "FORJAEXAMENES_PYTHON_INTERPRETE" $pythonExe

# Carpetas locales
Write-Info "Preparando carpetas de datos..."
$dirs = @(
    "datos", "datos\estadisticas", "datos\historial", "datos\entregas",
    "datos-practica", "indice", "banco\aprobados", "banco\pendientes",
    "examenes\paquetes", "documentacion"
)
foreach ($d in $dirs) {
    New-Item -ItemType Directory -Force -Path (Join-Path $Raiz $d) | Out-Null
}
Write-Ok "Carpetas listas."

# Dependencias Python (pip)
Write-Host ""
Write-Info "Dependencias Python (PDF + Gemini): requirements-docs.txt"
if (Preguntar-Si "Instalar dependencias Python ahora?") {
    $pipRes = Invoke-Native -FilePath $pythonExe -ArgumentList @(
        "-m", "pip", "install", "--upgrade", "pip"
    )
    $pipRes = Invoke-Native -FilePath $pythonExe -ArgumentList @(
        "-m", "pip", "install", "-r", "requirements-docs.txt"
    )
    if ($pipRes.ExitCode -ne 0) {
        Write-Err "pip fallo."
        Write-Host $pipRes.Output
        [void]$manualOptional.Add(@"
[RECOMENDADO] Dependencias Python
  Ejecuta: "$pythonExe" -m pip install -r requirements-docs.txt
  (Necesario para ejercicios desde apuntes PDF y Gemini)
"@)
    } else {
        Write-Ok "Dependencias Python instaladas."
    }
} else {
    Write-Warn "Omitido. Mas tarde: $pythonExe -m pip install -r requirements-docs.txt"
}

# Compilar JAR
Write-Host ""
Write-Info "Compilando aplicacion web (mvn package, puede tardar varios minutos)..."
$webDir = Join-Path $Raiz "web"
$pomFile = Join-Path $webDir "pom.xml"
if (-not (Test-Path -LiteralPath $webDir)) {
    Write-Err "No existe la carpeta web\"
    exit 1
}
if (-not (Test-Path -LiteralPath $pomFile)) {
    Write-Err "No se encontro pom.xml en web\"
    Write-Host "  Ruta esperada: $pomFile"
    exit 1
}
Write-Info "Proyecto Maven: $pomFile"
$mvnBuild = Invoke-Native -FilePath $mvnPath -WorkingDirectory $webDir -ArgumentList @(
    "-q", "-DskipTests", "-f", $pomFile, "package"
)
if ($mvnBuild.ExitCode -ne 0) {
    Write-Err "Maven fallo al compilar."
    Write-Host $mvnBuild.Output
    Write-Host ""
    Write-Host "Directorio de trabajo: $webDir"
    Write-Host "Comprueba que JDK 21 y Maven esten instalados correctamente."
    exit 1
}
$jar = Join-Path $Raiz "web\target\forjaexamenes-web-1.0.0.jar"
if (-not (Test-Path -LiteralPath $jar)) {
    Write-Err "No se genero web\target\forjaexamenes-web-1.0.0.jar"
    exit 1
}
Write-Ok "JAR generado."

# Gemini (opcional)
Write-Host ""
Write-Host "-- Clave API Gemini (opcional, ejercicios docs_*) --"
Write-Host "  https://aistudio.google.com/apikey"
Write-Host ""

$geminiOk = "no"
if (Preguntar-Si "Guardar clave Gemini ahora?") {
    $geminiPlain = Read-Host "Pega tu clave API Gemini"
    if ($geminiPlain) {
        $geminiModel = Read-Host "Modelo [gemini-2.5-flash]"
        if (-not $geminiModel) { $geminiModel = "gemini-2.5-flash" }
        $cfg = @{
            apiKey         = $geminiPlain
            model          = $geminiModel
            actualizadoPor = "instalador"
            actualizadoEn  = (Get-Date).ToString("o")
        }
        $cfg | ConvertTo-Json | Set-Content -Path (Join-Path $Raiz "datos\gemini.json") -Encoding UTF8
        Write-Ok "Clave guardada en datos\gemini.json"
        $geminiOk = "si"
    }
}

# Docker (opcional)
Write-Host ""
Write-Host "-- Docker / practica en contenedor (opcional) --"
Write-Host "  En Windows, Docker Desktop requiere WSL 2 (Subsistema de Windows para Linux)."
Write-Host "  (Comprobaciones rapidas; no bloquea la instalacion si Docker no esta listo.)"
Write-Host ""

$dockerOk = "no"
$wslOk = Test-WslInstalled
if ($wslOk) {
    Write-Ok "WSL detectado."
} else {
    Write-Warn "WSL no instalado (Docker Desktop en Windows lo requiere)."
    Show-WslInstallGuide
    $dockerOk = "requiere WSL"
    Write-Host "  No instalamos WSL desde aqui (tarda, pide admin y reinicio; el script pareceria colgado)."
    Write-Host "  La web funciona sin WSL. Para practica en contenedor: wsl --install en PowerShell admin, reinicia, abre Docker Desktop."
}

$dockerPath = Get-ToolPath "docker"
if (-not $dockerPath) {
    if ($wslOk -and (Preguntar-Si "Instalar Docker Desktop con winget? (opcional, tarda)")) {
        Install-DockerDesktop | Out-Null
        Refresh-SessionPath
        $dockerPath = Get-ToolPath "docker"
    }
}
if ($dockerPath) {
    Write-Info "Comprobando Docker CLI..."
    $dockerVer = Invoke-Native -FilePath $dockerPath -ArgumentList @("--version")
    Write-Ok "Docker CLI: $($dockerVer.Output)"
    if ($wslOk) {
        $motorListo = Test-DockerDaemonReady -DockerPath $dockerPath
        if ($motorListo) {
            Write-Ok "Motor Docker en marcha."
        } else {
            $dockerOk = "cli (motor parado)"
            Write-Warn "Docker instalado pero el motor no responde."
            Write-Host "  Abre Docker Desktop y espera a que este listo cuando quieras practicar en contenedor."
        }
        if (Preguntar-Si "Levantar contenedor de practica ahora?") {
            if (-not $motorListo) {
                $motorListo = Ensure-DockerDaemonReady -DockerPath $dockerPath
            }
            if ($motorListo) {
                try {
                    Invoke-DockerCompose -ComposeArgs @("up", "-d", "--build", "practica")
                    Write-Ok "Contenedor forjaexamenes-practica en marcha."
                    Write-Host "  Entrar: docker exec -it forjaexamenes-practica bash"
                    Write-Host "  O usa el boton «Abrir consola de practica» en la portada."
                    $dockerOk = "si"
                } catch {
                    Write-Warn "No se pudo levantar el contenedor."
                    Write-Host $_.Exception.Message
                    Write-Host "  Abre Docker Desktop, espera a que este listo y ejecuta:"
                    Write-Host "    docker compose up -d --build practica"
                }
            } else {
                Write-Host "  Cuando Docker Desktop este en marcha:"
                Write-Host "    docker compose up -d --build practica"
                Write-Host "  O el boton en http://localhost:8080 (seccion Entorno Docker)."
            }
        } elseif (-not $motorListo) {
            Write-Host "  Cuando Docker Desktop este en marcha:"
            Write-Host "    docker compose up -d --build practica"
            Write-Host "  O el boton en http://localhost:8080 (seccion Entorno Docker)."
        }
    } else {
        Write-Host "  Docker CLI detectado; hace falta WSL (ver pasos arriba) antes de usarlo."
    }
} elseif (-not $wslOk) {
    # Sin WSL ni Docker CLI: la guia WSL ya se mostro arriba
} else {
    [void]$manualOptional.Add(@"
[OPCIONAL] Docker Desktop (modulos docker, redes, sistemas, git)
  - Windows: primero WSL 2 (PowerShell admin: wsl --install, reiniciar PC)
    https://aka.ms/wslinstall
  - Luego Docker Desktop: https://www.docker.com/products/docker-desktop/
  - Si winget falla por hash: winget install Docker.DockerDesktop --ignore-security-hash
  - Cuando Docker este listo: docker compose up -d --build practica
"@)
    Write-Warn "Docker no instalado. La web funciona; la practica en contenedor no."
}

# Resumen
Write-Host ""
Write-Host "=========================================================="
Write-Host "  Instalacion completada"
Write-Host "=========================================================="
Write-Host ""
Write-Host "  Arrancar:  .\iniciar-forja.bat"
Write-Host "  Web:       http://localhost:8080"
Write-Host "  Guia:      http://localhost:8080/como-funciona"
Write-Host ""
Write-Host "  Cuentas:   alumno/practica  demo/demo  profesor/profesor"
Write-Host ""
Write-Host "  Python:    $pythonExe"
Write-Host "  Gemini:    $geminiOk"
Write-Host "  Docker:    $dockerOk"
Write-Host ""

if ($manualOptional.Count -gt 0) {
    Show-ManualChecklist -Steps (New-Object System.Collections.ArrayList) -OptionalSteps $manualOptional.ToArray()
}

if (Preguntar-Si "Arrancar la aplicacion ahora?") {
    Start-ForjaApplication -JavaPath (Get-ToolPath "java") -MavenPath $mvnPath | Out-Null
}

exit 0
