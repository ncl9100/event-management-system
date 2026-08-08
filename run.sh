#!/usr/bin/env bash
# ---------------------------------------------------------------
#  Event Management System - compile and run  (macOS / Linux)
#
#    bash run.sh          starts the command line interface
#    bash run.sh gui      starts the windowed interface
#
#  Needs a JDK 8 or newer. If javac is not on your PATH this script
#  will look in JAVA_HOME and in the usual install folders.
# ---------------------------------------------------------------
cd "$(dirname "$0")" || exit 1

# ---- find javac -------------------------------------------------
JAVAC=""
JAVACMD=""

if command -v javac >/dev/null 2>&1; then
    JAVAC="javac"
    JAVACMD="java"
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
    JAVACMD="$JAVA_HOME/bin/java"
elif [ -x /usr/libexec/java_home ] && /usr/libexec/java_home >/dev/null 2>&1; then
    # macOS keeps its JDKs here
    MAC_HOME="$(/usr/libexec/java_home)"
    JAVAC="$MAC_HOME/bin/javac"
    JAVACMD="$MAC_HOME/bin/java"
else
    for candidate in /usr/lib/jvm/*/bin/javac /Library/Java/JavaVirtualMachines/*/Contents/Home/bin/javac; do
        if [ -x "$candidate" ]; then
            JAVAC="$candidate"
            JAVACMD="${candidate%javac}java"
            break
        fi
    done
fi

if [ -z "$JAVAC" ]; then
    echo
    echo "  Could not find javac (the Java compiler)."
    echo
    echo "  A plain \"Java runtime\" is not enough - you need the JDK."
    echo "  Install one and run this again:"
    echo
    echo "      macOS    brew install --cask temurin"
    echo "      Ubuntu   sudo apt install default-jdk"
    echo
    echo "  or download it from https://adoptium.net"
    echo
    exit 1
fi

echo "Using: $JAVAC"
mkdir -p build

echo "Compiling..."
find src -name '*.java' > build/sources.txt
"$JAVAC" -d build @build/sources.txt || {
    echo
    echo "Compilation failed."
    exit 1
}

echo "Starting..."
if [ "$1" = "gui" ]; then
    "$JAVACMD" -cp build app.EventRegistrationApplication --gui
else
    "$JAVACMD" -cp build app.EventRegistrationApplication --cli
fi
