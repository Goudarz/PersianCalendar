#!/usr/bin/env bash
# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT
# Runs every test in the repository for which a toolchain is installed.
# Usage: bash tools/run-all-tests.sh
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IMPL="$ROOT/implementations"
BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT
pass=() fail=() skip=()

run() { # name, required command, directory, shell command
  local name="$1" tool="$2" dir="$3" cmd="$4"
  if ! command -v "$tool" >/dev/null 2>&1; then skip+=("$name"); return; fi
  echo "=== $name"
  if (cd "$dir" && eval "$cmd"); then pass+=("$name"); else fail+=("$name"); fi
}

run data       cargo  "$ROOT/tools/calendar-data" "cargo run -q --release -- --check && cargo test -q --release && rm -rf target Cargo.lock"
run python     python "$IMPL/python"     "python -m unittest -q"
run javascript node   "$IMPL/javascript" "node --test"
run typescript node   "$IMPL/typescript" "node --test persian-calendar.test.ts"
run java       javac  "$IMPL/java"       "javac -encoding UTF-8 -d '$BUILD/java' *.java && java -cp '$BUILD/java' PersianCalendarTest"
run kotlin     kotlinc "$IMPL/kotlin"    "kotlinc PersianCalendar.kt PersianCalendarTest.kt -include-runtime -d '$BUILD/kt.jar' 2>/dev/null && java -jar '$BUILD/kt.jar' ../../data"
run scala      scala  "$IMPL/scala"      "scala run . --main-class persiancalendar.PersianCalendarTest -- ../../data && rm -rf .scala-build .bsp"
run csharp     dotnet "$IMPL/csharp/tests" "DATA_DIR='$ROOT/data' dotnet run -c Release && rm -rf bin obj"
run go         go     "$IMPL/go"         "go vet ./... && go test ./... && GOARCH=386 go test ./..."
run rust       cargo  "$IMPL/rust"       "cargo test --release -q && rm -rf target Cargo.lock"
run php        php    "$IMPL/php"        "php test.php"
run dart       dart   "$IMPL/dart"       "dart pub get >/dev/null && dart analyze --fatal-infos && dart test && rm -rf .dart_tool pubspec.lock"
run c          cc     "$IMPL/c"          "cc -std=c99 -Wall -Wextra -O2 persian_calendar.c test.c -o '$BUILD/ctest' && '$BUILD/ctest' ../../data"
run ruby       ruby   "$IMPL/ruby"       "ruby test_persian_calendar.rb"
run postgresql npm    "$IMPL/sql"        "npm install --silent >/dev/null && npm test --silent"
run icu4x      cargo  "$ROOT/tools/icu-cross-check/rust" "cargo run -q --release && rm -rf target Cargo.lock"
run intl       node   "$ROOT/tools/icu-cross-check/javascript" "node check.mjs && node check.mjs typescript"
run sql-icu    npm    "$ROOT/tools/icu-cross-check/postgresql" "npm install --silent >/dev/null && node check.mjs"
# The ICU4C C-API checks use the system ICU (Linux, macOS/Homebrew); pass ICU_DIR to use another one.
run icu4c-py   python "$ROOT/tools/icu-cross-check/python" "python check.py ${ICU_DIR:+"'$ICU_DIR'"}"
run icu4c-go   go     "$ROOT/tools/icu-cross-check/go"     "go run . ${ICU_DIR:+"'$ICU_DIR'"}"
run icu4c-cs   dotnet "$ROOT/tools/icu-cross-check/csharp" "dotnet run -c Release -- ${ICU_DIR:+"'$ICU_DIR'"} && rm -rf bin obj"
run icu4c-dart dart   "$ROOT/tools/icu-cross-check/dart"   "dart pub get >/dev/null && dart run bin/check.dart ${ICU_DIR:+"'$ICU_DIR'"} && rm -rf .dart_tool pubspec.lock"

echo
echo "passed:  ${pass[*]:-none}"
echo "failed:  ${fail[*]:-none}"
echo "skipped (toolchain not found): ${skip[*]:-none}"
[ ${#fail[@]} -eq 0 ]
