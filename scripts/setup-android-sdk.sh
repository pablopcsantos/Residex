#!/usr/bin/env bash
set -euo pipefail

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${HOME}/android-sdk}"
export ANDROID_SDK_ROOT
CMDLINE_TOOLS_VERSION="13114758"
CMDLINE_TOOLS_DIR="${ANDROID_SDK_ROOT}/cmdline-tools/latest"
ARCHIVE="${TMPDIR:-/tmp}/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools"
if [[ ! -x "${CMDLINE_TOOLS_DIR}/bin/sdkmanager" ]]; then
    curl --fail --location --silent --show-error \
        "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
        --output "${ARCHIVE}"
    rm -rf "${ANDROID_SDK_ROOT}/cmdline-tools/tmp"
    mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools/tmp"
    unzip -q -o "${ARCHIVE}" -d "${ANDROID_SDK_ROOT}/cmdline-tools/tmp"
    rm -rf "${CMDLINE_TOOLS_DIR}"
    mv "${ANDROID_SDK_ROOT}/cmdline-tools/tmp/cmdline-tools" "${CMDLINE_TOOLS_DIR}"
    rm -rf "${ANDROID_SDK_ROOT}/cmdline-tools/tmp"
fi

export ANDROID_HOME="${ANDROID_SDK_ROOT}"
export PATH="${CMDLINE_TOOLS_DIR}/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

yes | sdkmanager --licenses >/dev/null || true
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

echo "Android SDK configured at ${ANDROID_SDK_ROOT}"
sdkmanager --list_installed | sed -n '1,80p'