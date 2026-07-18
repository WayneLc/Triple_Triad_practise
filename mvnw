#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# Maven Wrapper
# ----------------------------------------------------------------------------

basedir=$(dirname "$(echo "$0" | sed -e 's,\\,/,g')")
wrapperJar="$basedir/.mvn/wrapper/maven-wrapper.jar"
wrapperProps="$basedir/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$wrapperJar" ]; then
  echo "Maven Wrapper jar not found: $wrapperJar" >&2
  exit 1
fi

if [ -n "$JAVA_HOME" ]; then
  javaCmd="$JAVA_HOME/bin/java"
else
  javaCmd="java"
fi

exec "$javaCmd" -cp "$wrapperJar" org.apache.maven.wrapper.BootstrapMainStarter "$@"
