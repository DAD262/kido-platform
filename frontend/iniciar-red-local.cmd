@echo off
cd /d "%~dp0"
if not exist "node_modules\.bin\ng.cmd" (
  echo No se encontraron dependencias instaladas. Ejecuta npm.cmd install primero.
  pause
  exit /b 1
)
call npx.cmd ng serve --host 0.0.0.0 --port 4200 --proxy-config proxy.conf.json
