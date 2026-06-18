#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

rm -rf out
mkdir -p out/test

mapfile -t production_sources < <(
    find . -maxdepth 1 -type f -name '*.java' -print | sort
)

if ((${#production_sources[@]} == 0)); then
    echo "No production sources found." >&2
    exit 1
fi

javac -encoding UTF-8 --release 8 -cp "lib/*" -d out "${production_sources[@]}"

mapfile -t test_sources < <(
    find tests -maxdepth 1 -type f -name '*Test.java' -print | sort
)

if ((${#test_sources[@]} == 0)); then
    echo "No test classes found." >&2
    exit 1
fi

test_classes=()
for source in "${test_sources[@]}"; do
    test_classes+=("$(basename "$source" .java)")
done

echo "Found ${#test_classes[@]} test classes:"
printf '  %s\n' "${test_classes[@]}"

javac -encoding UTF-8 --release 8 -cp "out:lib/*" -d out/test "${test_sources[@]}"
java -cp "out/test:out:lib/*" org.junit.runner.JUnitCore "${test_classes[@]}"
