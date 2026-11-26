@echo off
rem Maven Wrapper do AtlasRoute: baixa o Maven na primeira execucao
rem (em %USERPROFILE%\.m2\wrapper\dists) e repassa os argumentos pra ele.
setlocal
set "MAVEN_VERSION=3.9.11"
set "MVN_DIST=apache-maven-%MAVEN_VERSION%"
set "MVN_BASE=%USERPROFILE%\.m2\wrapper\dists\%MVN_DIST%"
set "MVN_HOME=%MVN_BASE%\%MVN_DIST%"
set "MVN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/%MVN_DIST%-bin.zip"

if exist "%MVN_HOME%\bin\mvn.cmd" goto run

echo [mvnw] Baixando Maven %MAVEN_VERSION%, so na primeira vez...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; New-Item -ItemType Directory -Force -Path $env:MVN_BASE | Out-Null; $zip=Join-Path $env:MVN_BASE 'maven.zip'; Invoke-WebRequest -Uri $env:MVN_URL -OutFile $zip -UseBasicParsing; Expand-Archive -Path $zip -DestinationPath $env:MVN_BASE -Force; Remove-Item $zip"
if errorlevel 1 (
  echo [mvnw] Falha ao baixar o Maven.
  exit /b 1
)

:run
call "%MVN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
