#!/usr/bin/env bash
# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT
# Evaluates persian-calendar/calendar (Kotlin), built from source at the pinned tag.
# Needs git, kotlinc and java. Run: bash run.sh
set -euo pipefail
cd "$(dirname "$0")"
TAG=1.4.0
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
git -c advice.detachedHead=false clone -q --depth 1 --branch "$TAG" https://github.com/persian-calendar/calendar.git "$WORK/src"
kotlinc $(find "$WORK/src/src/main" -name '*.kt') ../../../implementations/kotlin/PersianCalendar.kt Eval.kt \
  -include-runtime -d "$WORK/eval.jar" 2>/dev/null
java -jar "$WORK/eval.jar" "$TAG" ../results
