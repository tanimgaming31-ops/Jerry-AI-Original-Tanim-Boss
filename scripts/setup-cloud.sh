#!/usr/bin/env bash
# Copyright (c) 2026 Tanim Boss. See LICENSE.
# Managed Debian/Ubuntu x86_64 setup. No sudo; no application or lockfile changes.
set -euo pipefail
[[ "$(uname -m)" == x86_64 ]] || { echo 'This cloud bootstrap requires Linux x86_64.' >&2; exit 1; }
jerry_script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
export JERRY_TOOLS_DIR="${JERRY_TOOLS_DIR:-/workspace/.jerry-tools}"
mkdir -p "$JERRY_TOOLS_DIR/downloads"
fetch_verified() {
    local url="$1" checksum="$2" destination="$3"
    if [[ ! -f "$destination" ]]; then
        curl --fail --location --retry 2 "$url" -o "$destination.download"
        printf '%s  %s\n' "$checksum" "$destination.download" | sha256sum --check --status
        mv "$destination.download" "$destination"
    fi
    printf '%s  %s\n' "$checksum" "$destination" | sha256sum --check
}
fetch_verified 'https://packages.microsoft.com/debian/12/prod/pool/main/m/msopenjdk-17/msopenjdk-17_17.0.20.1-1_amd64.deb' \
    '264b078dae839511d08b285957667d18c5bec5e3b17b79e34b7f0bcf4853a855' "$JERRY_TOOLS_DIR/downloads/jdk17.deb"
if [[ ! -x "$JERRY_TOOLS_DIR/jdk/usr/lib/jvm/msopenjdk-17-amd64/bin/java" ]]; then
    mkdir -p "$JERRY_TOOLS_DIR/jdk"
    dpkg-deb -x "$JERRY_TOOLS_DIR/downloads/jdk17.deb" "$JERRY_TOOLS_DIR/jdk"
fi
fetch_verified 'https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip' \
    '0877a1d048fe4a24efe2eff536ca4223f7adeb58648bb81909d33c446918cfa8' "$JERRY_TOOLS_DIR/downloads/cmdline.zip"
if [[ ! -x "$JERRY_TOOLS_DIR/android-sdk/cmdline-tools/latest/bin/sdkmanager" ]]; then
    jerry_extract_dir="$(mktemp -d "$JERRY_TOOLS_DIR/sdk-extract.XXXXXX")"
    unzip -q "$JERRY_TOOLS_DIR/downloads/cmdline.zip" -d "$jerry_extract_dir"
    mkdir -p "$JERRY_TOOLS_DIR/android-sdk/cmdline-tools"
    mv "$jerry_extract_dir/cmdline-tools" "$JERRY_TOOLS_DIR/android-sdk/cmdline-tools/latest"
    rmdir "$jerry_extract_dir"
fi
source "$jerry_script_dir/cloud-env.sh"
java -version
# Noninteractive acceptance of SDK terms for this authorized development setup.
jerry_answers="$(mktemp)"
trap 'rm -f "$jerry_answers"' EXIT
python3 -c 'print("y\n" * 100)' > "$jerry_answers"
sdkmanager --licenses < "$jerry_answers"
sdkmanager 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'
echo "Setup complete. Source scripts/cloud-env.sh before running ./gradlew."
