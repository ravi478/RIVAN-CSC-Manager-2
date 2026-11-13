#!/usr/bin/env sh
# RIVAN bootstrap launcher: downloads the pinned Gradle distribution if needed.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION="8.11.1"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/rivan-wrapper/gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle"
ZIP_FILE="$GRADLE_USER_HOME/rivan-wrapper/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_USER_HOME/rivan-wrapper"
  echo "Downloading Gradle $GRADLE_VERSION..."
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ZIP_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP_FILE" "$URL"
  else
    echo "Error: curl or wget is required to download Gradle." >&2
    exit 1
  fi
  mkdir -p "$DIST_DIR"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP_FILE" -d "$DIST_DIR"
  else
    echo "Error: unzip is required to extract Gradle." >&2
    exit 1
  fi
fi
exec "$GRADLE_BIN" -p "$APP_HOME" "$@"
