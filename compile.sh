#!/bin/bash

# Compile all Java files in the project
rm -rf out
mkdir -p out
javac -encoding UTF-8 -d out -sourcepath src \
    $(find src -name '*.java')
