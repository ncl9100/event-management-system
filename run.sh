#!/usr/bin/env bash
# ---------------------------------------------------------------
#  Event Management System - compile and run  (macOS / Linux)
#
#    ./run.sh          starts the command line interface
#    ./run.sh gui      starts the windowed interface
#
#  Needs a JDK 8 or newer on the PATH. Nothing else to install.
# ---------------------------------------------------------------
set -e
cd "$(dirname "$0")"

mkdir -p build

echo "Compiling..."
javac -encoding UTF-8 -d build $(find src -name '*.java')

echo "Starting..."
if [ "$1" = "gui" ]; then
    java -cp build app.EventRegistrationApplication --gui
else
    java -cp build app.EventRegistrationApplication --cli
fi
