#!/bin/bash
set -e
if [ -d "/app/applet/.tools/gradle-9.8.1" ]; then
    ln -sf /app/applet/.tools/gradle-9.8.1/bin/gradle /usr/local/bin/gradle
    ln -sf /app/applet/.tools/gradle-9.8.1/bin/gradle /opt/gradle/gradle-9.3.1/bin/gradle
fi
if [ -d "/app/applet/.tools/android-37.0" ] && [ ! -d "/opt/android/sdk/platforms/android-37.0" ]; then
    mkdir -p /opt/android/sdk/platforms
    ln -sf /app/applet/.tools/android-37.0 /opt/android/sdk/platforms/android-37.0
fi
