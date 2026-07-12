#!/usr/bin/env sh

# Compact Gradle wrapper launcher. The wrapper JAR and configuration live in
# gradle/wrapper, so no system-wide Gradle installation is required.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd) || exit 1

if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD=java
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1 && [ ! -x "$JAVACMD" ]; then
  echo "ERROR: Java was not found. Install JDK 17 or set JAVA_HOME." >&2
  exit 1
fi

exec "$JAVACMD" -Xmx64m -Xms64m \
  -Dorg.gradle.appname=gradlew \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
