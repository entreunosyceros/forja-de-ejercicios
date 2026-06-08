# Desarrollado por entreunosyceros - 2026
# Instalador Forja de ejercicios (Windows / PowerShell 5.1+)
#
# Abrir PowerShell en la carpeta examenforge y ejecutar:
#   Set-ExecutionPolicy -Scope Process Bypass
#   .\install.ps1
#
# Requisitos que el script intenta instalar solo (con winget):
#   - JDK 21, Maven 3.8+, Python 3.10+
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

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [string[]]$ArgumentList = @()
    )
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $FilePath
    $psi.Arguments = [string]::Join(" ", $ArgumentList)
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

function Install-WingetPackage {
    param(
        [string]$PackageId,
        [string]$DisplayName
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
    $mvnPath = Get-ToolPath "mvn"
    if ($mvnPath) {
        $info = Invoke-Native -FilePath $mvnPath -ArgumentList @("-version")
        Write-Ok "Maven: $($info.Output.Split([char]10)[0])"
        return $true
    }
    if (Preguntar-Si "Instalar Maven automaticamente con winget?") {
        Install-WingetPackage -PackageId "Apache.Maven" -DisplayName "Apache Maven" | Out-Null
        Refresh-SessionPath
    }
    $mvnPath = Get-ToolPath "mvn"
    if ($mvnPath) {
        Write-Ok "Maven listo."
        return $true
    }
    [void]$ManualSteps.Add(@"
[OBLIGATORIO] Apache Maven 3.8+
  - Descarga: https://maven.apache.org/download.cgi
  - O: winget install Apache.Maven
  - Anade Maven al PATH (MAVEN_HOME / bin).
"@)
    return $false
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

function Actualizar-Env {
    param([string]$Clave, [string]$Valor)
    $envFile = Join-Path $Raiz ".env"
    $linea = "$Clave=$Valor"
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

function Invoke-DockerCompose {
    param([string[]]$ComposeArgs)
    $docker = Get-ToolPath "docker"
    if (-not $docker) {
        throw "Docker no encontrado"
    }
    $res = Invoke-Native -FilePath $docker -ArgumentList (@("compose") + $ComposeArgs)
    if ($res.ExitCode -ne 0) {
        $legacy = Get-ToolPath "docker-compose"
        if ($legacy) {
            $res = Invoke-Native -FilePath $legacy -ArgumentList $ComposeArgs
        }
    }
    if ($res.ExitCode -ne 0) {
        throw $res.Output
    }
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
    Write-Host "Cuando termines, vuelve a ejecutar: .\install.ps1"
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
$mavenOk = Ensure-Maven -ManualSteps $manualRequired
$pythonExe = Ensure-Python -ManualSteps $manualRequired

if (-not $javaOk -or -not $mavenOk -or -not $pythonExe) {
    Show-ManualChecklist -Steps $manualRequired -OptionalSteps @()
    exit 1
}

Actualizar-Env "FORJAEXAMENES_PYTHON_INTERPRETE" $pythonExe
$mvnPath = Get-ToolPath "mvn"

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
if (-not (Test-Path -LiteralPath $webDir)) {
    Write-Err "No existe la carpeta web\"
    exit 1
}
Push-Location -LiteralPath $webDir
$mvnBuild = Invoke-Native -FilePath $mvnPath -ArgumentList @("-q", "-DskipTests", "package")
Pop-Location
if ($mvnBuild.ExitCode -ne 0) {
    Write-Err "Maven fallo al compilar."
    Write-Host $mvnBuild.Output
    Write-Host ""
    Write-Host "Comprueba que JDK 21 y Maven esten en el PATH."
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
    $geminiPlain = Read-Host "Pega tu clave API (AIzaSy...)"
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
Write-Host ""

$dockerOk = "no"
$dockerPath = Get-ToolPath "docker"
if (-not $dockerPath) {
    if (Preguntar-Si "Instalar Docker Desktop con winget? (opcional, tarda)") {
        Install-WingetPackage -PackageId "Docker.DockerDesktop" -DisplayName "Docker Desktop" | Out-Null
        Refresh-SessionPath
        $dockerPath = Get-ToolPath "docker"
    }
}
if ($dockerPath) {
    $dockerVer = Invoke-Native -FilePath $dockerPath -ArgumentList @("--version")
    Write-Ok "Docker: $($dockerVer.Output)"
    if (Preguntar-Si "Levantar contenedor de practica ahora?") {
        try {
            Invoke-DockerCompose -ComposeArgs @("up", "-d", "--build", "practica")
            Write-Ok "Contenedor forjaexamenes-practica en marcha."
            $dockerOk = "si"
        } catch {
            Write-Warn "No se pudo levantar el contenedor. Abre Docker Desktop y reintenta."
            Write-Host $_.Exception.Message
        }
    }
} else {
    [void]$manualOptional.Add(@"
[OPCIONAL] Docker Desktop (modulos docker, redes, sistemas, git)
  - https://www.docker.com/products/docker-desktop/
  - O: winget install Docker.DockerDesktop
  - Luego: docker compose up -d --build practica
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
    $bat = Join-Path $Raiz "iniciar-forja.bat"
    if (Test-Path -LiteralPath $bat) {
        & cmd.exe /c "`"$bat`""
    }
}

exit 0
