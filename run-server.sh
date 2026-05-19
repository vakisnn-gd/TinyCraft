#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"

if [ ! -f out/TinyCraftServer.class ]; then
    echo "Compiled server files were not found."
    echo "Trying to compile TinyCraft..."
    mkdir -p out
    javac -encoding UTF-8 --release 8 -cp "lib/*" -d out *.java
fi

exec java -Dfile.encoding=UTF-8 -cp "out:lib/*" TinyCraftServer "$@"
