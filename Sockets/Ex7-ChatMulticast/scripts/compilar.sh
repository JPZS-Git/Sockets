#!/bin/sh
cd "$(dirname "$0")/.." || exit 1
mkdir -p out
javac -encoding UTF-8 -d out src/comum/*.java src/cliente/*.java && echo "Compilado em out/"
