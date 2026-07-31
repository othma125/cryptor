#!/usr/bin/env bash
# Author: Othmane

set -e

cd "$(dirname "$0")"
./compile.sh

java -cp out Main "$@"
