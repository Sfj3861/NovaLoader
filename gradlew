#!/usr/bin/env sh
set -eu

GRADLE_VERSION="8.9"
GRADLE_ROOT="$HOME/.gradle/novaloader"
GRADLE_HOME="$GRADLE_ROOT/gradle-$GRADLE_VERSION"
ARCHIVE="$GRADLE_ROOT/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
    mkdir -p "$GRADLE_ROOT"
    if [ ! -f "$ARCHIVE" ]; then
        if command -v curl >/dev/null 2>&1; then
            curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
        elif command -v wget >/dev/null 2>&1; then
            wget "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$ARCHIVE"
        else
            echo "Install curl or wget to download Gradle $GRADLE_VERSION." >&2
            exit 1
        fi
    fi
    if ! command -v unzip >/dev/null 2>&1; then
        echo "Install unzip to unpack the Gradle distribution." >&2
        exit 1
    fi
    unzip -oq "$ARCHIVE" -d "$GRADLE_ROOT"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
