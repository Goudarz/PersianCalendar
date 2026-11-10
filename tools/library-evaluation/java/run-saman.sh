#!/usr/bin/env bash
# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT
# Evaluates samanzamani/PersianDate, built from source at the pinned tag. Needs git and a JDK 17+.
set -euo pipefail
cd "$(dirname "$0")"
TAG=1.7.1
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
git -c advice.detachedHead=false clone -q --depth 1 --branch "$TAG" https://github.com/samanzamani/PersianDate.git "$WORK/src"
javac -encoding UTF-8 -nowarn -d "$WORK/out" "$WORK"/src/persiandate/src/main/java/saman/zamani/persiandate/*.java \
  ../../../implementations/java/PersianCalendar.java Saman.java
java -cp "$WORK/out" Saman "$TAG" ../results
