#!/usr/bin/env bash
# Carga en GitHub la llave de firma de Nokia Launcher como Repository secrets, para el Action de release.
# Se ejecuta una sola vez, desde tu computadora:  ./scripts/setup_github_secrets.sh
# Necesita GitHub CLI con sesión iniciada (brew install gh && gh auth login) y el repositorio ya creado.
# La contraseña se pide aquí; no se guarda en el disco ni aparece en la lista de procesos.
# OJO: la llave y su contraseña salen de tu computadora hacia GitHub (como secretos cifrados).
# Variables opcionales: KEYSTORE (ruta al .jks), REPO (usuario/repositorio).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE="${KEYSTORE:-$ROOT/../nokia.jks}"

command -v gh >/dev/null || { echo "Falta GitHub CLI: brew install gh && gh auth login"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "Inicia sesión: gh auth login"; exit 1; }
[ -f "$KEYSTORE" ] || { echo "No existe el keystore: $KEYSTORE"; exit 1; }
REPO="${REPO:-$(cd "$ROOT" && gh repo view --json nameWithOwner -q .nameWithOwner)}"

JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [ ! -x "${JAVA_HOME:-}/bin/keytool" ] && [ -d "$JBR" ]; then JAVA_HOME="$JBR"; fi
KEYTOOL="${JAVA_HOME:+$JAVA_HOME/bin/}keytool"

echo "Se enviará $(basename "$KEYSTORE") y su contraseña al repositorio $REPO"
echo "como secretos KEYSTORE_BASE64 y KEYSTORE_PASSWORD."
read -r -p "Escribe SI para continuar: " answer
[ "$answer" = "SI" ] || { echo "Cancelado; no se envió nada."; exit 1; }

read -r -s -p "Contraseña del keystore: " KEYSTORE_PASS; echo
export KEYSTORE_PASS
# Se comprueba antes de enviar, para no guardar una contraseña equivocada.
"$KEYTOOL" -list -keystore "$KEYSTORE" -storepass:env KEYSTORE_PASS >/dev/null 2>&1 \
  || { echo "La contraseña no abre el keystore; no se envió nada."; exit 1; }

# Los valores viajan por la entrada estándar, nunca como argumentos.
base64 < "$KEYSTORE" | tr -d '\n' | gh secret set KEYSTORE_BASE64 --repo "$REPO"
printf '%s' "$KEYSTORE_PASS" | gh secret set KEYSTORE_PASSWORD --repo "$REPO"
echo "✔ Secretos de $(basename "$KEYSTORE") guardados en $REPO"
