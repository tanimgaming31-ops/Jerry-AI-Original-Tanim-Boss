#!/usr/bin/env bash
# Source only in the managed Linux cloud environment. No credential values stored.
export JERRY_TOOLS_DIR="${JERRY_TOOLS_DIR:-/workspace/.jerry-tools}"
export JAVA_HOME="$JERRY_TOOLS_DIR/jdk/usr/lib/jvm/msopenjdk-17-amd64"
export ANDROID_HOME="$JERRY_TOOLS_DIR/android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
if [[ -n "${HTTPS_PROXY:-}" && -z "${JERRY_JVM_PROXY_READY:-}" ]]; then
    # The session sidecar uses an HTTP proxy without client credentials.
    jerry_proxy_host="$(python3 -c 'import os,urllib.parse; print(urllib.parse.urlparse(os.environ["HTTPS_PROXY"]).hostname or "")')"
    jerry_proxy_port="$(python3 -c 'import os,urllib.parse; print(urllib.parse.urlparse(os.environ["HTTPS_PROXY"]).port or 80)')"
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dhttps.proxyHost=$jerry_proxy_host -Dhttps.proxyPort=$jerry_proxy_port -Dhttp.proxyHost=$jerry_proxy_host -Dhttp.proxyPort=$jerry_proxy_port -Dhttp.nonProxyHosts=localhost|127.*"
    if [[ -f /etc/ssl/certs/java/cacerts ]]; then
        export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
    fi
    export JERRY_JVM_PROXY_READY=1
    unset jerry_proxy_host jerry_proxy_port
fi
