#!/usr/bin/env bash
# Author: Othmane

# Compiles and runs a test class from test/ (default: RoundTripTest).
# Must run from the repo root so the InputParameters file is found.

set -e

cd "$(dirname "$0")"
./compile.sh

javac -encoding UTF-8 -d out -cp out -sourcepath src test/*.java

java -ea -cp out "${1:-RoundTripTest}"
