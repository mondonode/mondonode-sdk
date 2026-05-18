#!/bin/sh
# Gradle wrapper launcher — fetches Gradle if not cached.
# Run: gradle wrapper --gradle-version=8.9 to regenerate gradle-wrapper.jar

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

DIRNAME=$(dirname "$0")
cd "$DIRNAME" || exit

JAVA_HOME="${JAVA_HOME:-}"
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

eval exec '"$JAVACMD"' \
  $DEFAULT_JVM_OPTS \
  $JAVA_OPTS \
  $GRADLE_OPTS \
  '"-Dorg.gradle.appname=$APP_BASE_NAME"' \
  -classpath '"gradle/wrapper/gradle-wrapper.jar"' \
  org.gradle.wrapper.GradleWrapperMain \
  '"$@"'
