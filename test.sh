#!/usr/bin/env bash
# Author: Othmane

# Compiles and runs a test class from test/ (default: RoundTripTest).
# Must run from the repo root so the InputParameters file is found.

set -e

cd "$(dirname "$0")"
./compile.sh

javac -encoding UTF-8 -d out -cp out -sourcepath src test/*.java

test="${1:-RoundTripTest}"

# Memoize the PBKDF2 stretch: the suites re-derive the same key from a fixed
# password and salt hundreds of times. Off for BenchmarkTest, whose decrypt pass
# reuses the encrypt pass's salt and would report a cache hit as throughput.
[ "$test" = BenchmarkTest ] || kdf=-Dcryptor.kdf.cache=true

java -ea $kdf -cp out "$test"
