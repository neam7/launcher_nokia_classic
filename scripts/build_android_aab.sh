#!/usr/bin/env bash
# Release firmado de Nokia Launcher en releases/: nokialauncher_<versionName>.aab y .apk
# Uso:  ./scripts/build_android_aab.sh          (con --apk: solo el APK)
# El keystore se busca fuera del repositorio: ../nokia.jks
# La contraseña se pide al ejecutar; no se guarda en ningún sitio.
# Variables opcionales: KEYSTORE_PASS, KEY_ALIAS, KEY_PASS (si difiere de la del keystore),
#                       KEYSTORE (ruta al .jks), OUT (carpeta de salida), JAVA_HOME,
#                       FORCE=1 (reemplazar un release que ya existe),
#                       UNSIGNED (carpeta con app-release-unsigned.apk y app-release.aab ya
#                                 compilados: solo firma. Lo usa GitHub Actions).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT"; NAME="nokialauncher"
APK_ONLY=""
case "${1:-}" in
  "") ;;
  --apk) APK_ONLY=1 ;;
  *) echo "Opción desconocida: $1"; exit 1 ;;
esac
KEYSTORE="${KEYSTORE:-$ROOT/../nokia.jks}"
OUT="${OUT:-$ROOT/releases}"

# JDK: el que venga en JAVA_HOME; si no, el JBR de Android Studio (el que se usó para compilar este proyecto).
JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
# Un JAVA_HOME sin jlink (como el JRE de VS Code) no compila Android.
if [ ! -x "${JAVA_HOME:-}/bin/jlink" ] && [ -d "$JBR" ]; then JAVA_HOME="$JBR"; fi
[ -n "${JAVA_HOME:-}" ] && export JAVA_HOME
KEYTOOL="${JAVA_HOME:+$JAVA_HOME/bin/}keytool"
JARSIGNER="${JAVA_HOME:+$JAVA_HOME/bin/}jarsigner"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
APKSIGNER="$(ls -d "$SDK"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)"
[ -n "$APKSIGNER" ] || { echo "No encuentro apksigner en $SDK/build-tools"; exit 1; }
[ -f "$KEYSTORE" ] || { echo "No existe el keystore: $KEYSTORE"; exit 1; }

VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' "$APP/app/build.gradle.kts")"
CODE="$(sed -n 's/.*versionCode = \([0-9]*\).*/\1/p' "$APP/app/build.gradle.kts")"
BASE="$OUT/${NAME}_${VERSION}"
echo "→ $NAME, versión $VERSION (code $CODE), firma $(basename "$KEYSTORE")"
if [ -z "$APK_ONLY" ] && [ -z "${FORCE:-}" ] && { [ -e "$BASE.aab" ] || [ -e "$BASE.apk" ]; }; then
  echo "Ya existe el release $VERSION en $OUT."
  echo "Sube versionCode y versionName en app/build.gradle.kts, o usa FORCE=1 para reemplazarlo."
  exit 1
fi

if [ -z "${KEYSTORE_PASS:-}" ]; then read -r -s -p "Contraseña del keystore: " KEYSTORE_PASS; echo; fi
# Las herramientas leen las contraseñas del entorno para que no aparezcan en la lista de procesos.
export KEYSTORE_PASS
if [ -z "${KEY_ALIAS:-}" ]; then
  ALIASES="$("$KEYTOOL" -list -keystore "$KEYSTORE" -storepass:env KEYSTORE_PASS 2>/dev/null | grep "PrivateKeyEntry" | cut -d, -f1 || true)"
  [ -n "$ALIASES" ] || { echo "No pude leer el keystore (¿contraseña incorrecta?)"; exit 1; }
  if [ "$(echo "$ALIASES" | wc -l | tr -d ' ')" = "1" ]; then KEY_ALIAS="$ALIASES"; echo "→ Alias: $KEY_ALIAS"
  else echo "Alias disponibles:"; echo "$ALIASES"; read -r -p "Alias: " KEY_ALIAS; fi
fi
export KEY_PASS="${KEY_PASS:-$KEYSTORE_PASS}"

TASKS="assembleRelease"; [ -n "$APK_ONLY" ] || TASKS="assembleRelease bundleRelease"
if [ -z "${UNSIGNED:-}" ]; then
  # La compilación no necesita las contraseñas.
  ( cd "$APP" && env -u KEYSTORE_PASS -u KEY_PASS ./gradlew $TASKS -q )
  IN_APK="$APP/app/build/outputs/apk/release/app-release-unsigned.apk"
  IN_AAB="$APP/app/build/outputs/bundle/release/app-release.aab"
else
  IN_APK="$UNSIGNED/app-release-unsigned.apk"; IN_AAB="$UNSIGNED/app-release.aab"
fi

mkdir -p "$OUT"
# Se firma en una carpeta temporal: si algo falla, no queda un release a medias.
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

"$APKSIGNER" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
  --ks-pass env:KEYSTORE_PASS --key-pass env:KEY_PASS \
  --out "$TMP/app.apk" "$IN_APK"
"$APKSIGNER" verify "$TMP/app.apk"

if [ -z "$APK_ONLY" ]; then
  cp "$IN_AAB" "$TMP/app.aab"
  "$JARSIGNER" -sigalg SHA256withRSA -digestalg SHA-256 -keystore "$KEYSTORE" \
    -storepass:env KEYSTORE_PASS -keypass:env KEY_PASS "$TMP/app.aab" "$KEY_ALIAS" >/dev/null
  "$JARSIGNER" -verify "$TMP/app.aab" >/dev/null
  mv "$TMP/app.aab" "$BASE.aab"
  echo "✔ AAB firmado: $BASE.aab"
fi
mv "$TMP/app.apk" "$BASE.apk"
echo "✔ APK firmado: $BASE.apk"
