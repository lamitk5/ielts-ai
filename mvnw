#!/bin/sh
if [ -d "/c/Program Files/Java/jdk-23" ]; then
    export JAVA_HOME="/c/Program Files/Java/jdk-23"
elif [ -d "C:\\Program Files\\Java\\jdk-23" ]; then
    export JAVA_HOME="C:\\Program Files\\Java\\jdk-23"
fi
MAVEN_CMD="$TEMP/maven/apache-maven-3.9.9/bin/mvn"
if [ -f "$MAVEN_CMD" ]; then
    exec "$MAVEN_CMD" "$@"
else
    exec mvn "$@"
fi
