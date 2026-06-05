# Instalador de Forja de ejercicios (Windows).
# Ejecutar en PowerShell:  Set-ExecutionPolicy -Scope Process Bypass; .\install.ps1
$ErrorActionPreference = "Stop"
$Raiz = $PSScriptRoot
Set-Location $Raiz

function Write-Info($msg)  { Write-Host "→ $msg" -ForegroundColor Cyan }
function Write-Ok($msg)    { Write-Host "✓ $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "! $msg" -ForegroundColor Yellow }
function Write-Err($msg)   { Write-Host "✗ $msg" -ForegroundColor Red }

function Preguntar-Si($texto) {
    $r = Read-Host "$texto [s/N]"
    return $r -match '^(s|si|sí|S|Si|Sí)$'
}

function Actualizar-Env($clave, $valor) {
    $envFile = Join-Path $Raiz ".env"
    $linea = "$clave=$valor"
    if (Test-Path $envFile) {
        $contenido = Get-Content $envFile -Raw
        if ($contenido -match "(?m)^$([regex]::Escape($clave))=") {
            $contenido = $contenido -replace "(?m)^$([regex]::Escape($clave))=.*", $linea
        } else {
            $contenido = $contenido.TrimEnd() + "`n$linea`n"
        }
        Set-Content -Path $envFile -Value $contenido -Encoding UTF8
    } else {
        Set-Content -Path $envFile -Value "$linea`n" -Encoding UTF8
    }
}

Write-Host ""
Write-Host "══════════════════════════════════════════════════════════"
Write-Host "  Forja de ejercicios — instalación (Windows)"
Write-Host "══════════════════════════════════════════════════════════"
Write-Host ""

# ── 1. Requisitos ─────────────────────────────────────────────
Write-Info "Comprobando requisitos…"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Err "No se encontró Java. Instala JDK 21 (Temurin, Oracle…)."
    exit 1
}
$javaVer = (java -version 2>&1 | Select-Object -First 1)
if ($javaVer -notmatch 'version "21') {
    Write-Warn "Se recomienda JDK 21. Detectado: $javaVer"
} else {
    Write-Ok "Java: $javaVer"
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Err "No se encontró Maven 3.8+. Instálalo (winget install Maven.Maven) y vuelve a ejecutar."
    exit 1
}
Write-Ok "Maven: $((mvn -version 2>&1 | Select-Object -First 1))"

$Python = $null
if (Get-Command python -ErrorAction SilentlyContinue) {
    $Python = "python"
} elseif (Get-Command python3 -ErrorAction SilentlyContinue) {
    $Python = "python3"
}
if (-not $Python) {
    Write-Err "No se encontró Python 3.10+. Instálalo desde python.org y vuelve a ejecutar."
    exit 1
}
$pyVer = & $Python --version 2>&1
Write-Ok "Python: $pyVer ($Python)"
Actualizar-Env "FORJAEXAMENES_PYTHON_INTERPRETE" $Python

# ── 2. Carpetas locales ───────────────────────────────────────
Write-Info "Preparando carpetas de datos…"
@(
    "datos", "datos\estadisticas", "datos-practica", "indice",
    "banco\aprobados", "banco\pendientes", "examenes\paquetes", "documentacion"
) | ForEach-Object {
    New-Item -ItemType Directory -Force -Path (Join-Path $Raiz $_) | Out-Null
}
Write-Ok "Carpetas listas"

# ── 3. Dependencias Python ────────────────────────────────────
Write-Host ""
Write-Info "Dependencias Python para apuntes PDF y Gemini"
if (Preguntar-Si "¿Instalar dependencias Python ahora?") {
    & $Python -m pip install -r requirements-docs.txt
    Write-Ok "Dependencias Python instaladas"
} else {
    Write-Warn "Omitido. Más tarde: $Python -m pip install -r requirements-docs.txt"
}

# ── 4. Compilar aplicación web ────────────────────────────────
Write-Host ""
Write-Info "Compilando la aplicación web (mvn package, puede tardar unos minutos)…"
Push-Location (Join-Path $Raiz "web")
& mvn -q -DskipTests package
Pop-Location
Write-Ok "JAR generado: web\target\forjaexamenes-web-1.0.0.jar"

# ── 5. Gemini (opcional) ──────────────────────────────────────
Write-Host ""
Write-Host "── Clave API de Gemini (opcional) ──"
Write-Host "  Sirve para ejercicios desde apuntes PDF (módulos docs_*)."
Write-Host "  Los módulos clásicos (POO, SQL, Docker…) funcionan sin ella."
Write-Host "  Crea una clave en: https://aistudio.google.com/apikey"
Write-Host "  También puedes configurarla después: Perfil → Clave API de Gemini"
Write-Host ""

$geminiOk = "no"
if (Preguntar-Si "¿Quieres guardar una clave Gemini ahora?") {
    $geminiPlain = Read-Host "Pega tu clave API (AIzaSy…)"
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
        $geminiOk = "sí"
    } else {
        Write-Warn "Clave vacía; puedes configurarla en Perfil más tarde."
    }
} else {
    Write-Warn "Gemini omitido. Configúralo en Perfil cuando quieras."
}

# ── 6. Docker (opcional) ──────────────────────────────────────
Write-Host ""
Write-Host "── Entorno de práctica en Docker (opcional) ──"
Write-Host "  Contenedor shell para ejercicios docker, redes, sistemas y git."
Write-Host "  Instala Docker Desktop: https://www.docker.com/products/docker-desktop/"
Write-Host "  En Windows suele requerir WSL2 habilitado."
Write-Host ""

$dockerOk = "no"
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Ok "Docker detectado: $((docker --version 2>&1 | Select-Object -First 1))"
    if (Preguntar-Si "¿Levantar el contenedor de práctica ahora (docker compose up)?") {
        try {
            docker compose up -d --build practica
            Write-Ok "Contenedor forjaexamenes-practica en marcha"
            Write-Host "  Entrar: docker exec -it forjaexamenes-practica bash"
            Write-Host "  Usuario: alumno / practica"
            $dockerOk = "sí"
        } catch {
            Write-Warn "No se pudo levantar el contenedor. Comprueba que Docker Desktop esté en marcha."
        }
    } else {
        Write-Warn "Docker listo. Levántalo más tarde:"
        Write-Host "    docker compose up -d --build practica"
    }
} else {
    Write-Warn "Docker no detectado. La web funcionará; la práctica en contenedor requerirá Docker Desktop."
}

# ── 7. Resumen ────────────────────────────────────────────────
Write-Host ""
Write-Host "══════════════════════════════════════════════════════════"
Write-Host "  Instalación completada"
Write-Host "══════════════════════════════════════════════════════════"
Write-Host ""
Write-Host "  Arrancar:     .\iniciar-forja.bat"
Write-Host "  Abrir web:    http://localhost:8080"
Write-Host "  Guía:         http://localhost:8080/como-funciona"
Write-Host ""
Write-Host "  Cuentas demo: alumno/practica · demo/demo · profesor/profesor"
Write-Host ""
Write-Host "  Resumen:"
Write-Host "    · Python:  $Python"
Write-Host "    · Gemini:  $geminiOk"
Write-Host "    · Docker:  $dockerOk"
Write-Host ""
if ($geminiOk -eq "no") {
    Write-Host "  → Gemini: Perfil → Clave API de Gemini (o datos\gemini.json)"
}
if ($dockerOk -eq "no") {
    Write-Host "  → Docker: docker compose up -d --build practica"
}
Write-Host ""

if (Preguntar-Si "¿Arrancar la aplicación ahora?") {
    & (Join-Path $Raiz "iniciar-forja.bat")
}
