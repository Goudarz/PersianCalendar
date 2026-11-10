// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Compares ICU4C (called directly through dart:ffi), this repository's Dart implementation and the
// ground truth (data/years.csv) for every day of 1178-3000 AP, in both directions.
//
// ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table;
// for them the program checks that ICU matches the plain 33-year rule exactly instead.
//
// Usage: dart run bin/check.dart [folder with icuinNN.dll / libicui18n.so.NN]
import 'dart:ffi';
import 'dart:io';

import 'package:persian_calendar/persian_calendar.dart' as ref;

const ucalMonth = 2, ucalDate = 5, ucalExtendedYear = 19, ucalJulianDay = 20;

/// (i18n library, common library, major version)
(String, String, int) findIcu(String? folder) {
  final dirs = folder != null
      ? [folder]
      : [
          '/usr/lib/x86_64-linux-gnu', '/usr/lib/aarch64-linux-gnu',
          '/usr/lib64', '/usr/lib', //
          '/opt/homebrew/opt/icu4c/lib', '/usr/local/opt/icu4c/lib',
        ];
  final pattern = RegExp(
      r'^(?:icuin(\d+)\.dll|libicui18n\.so\.(\d+)|libicui18n\.(\d+)\.dylib)$');
  for (final dir in dirs.where((d) => Directory(d).existsSync())) {
    final files = Directory(dir).listSync().map((e) => e.path).toList()
      ..sort((a, b) => b.compareTo(a));
    for (final i18n in files) {
      final m = pattern.firstMatch(i18n.split(RegExp(r'[/\\]')).last);
      final uc = i18n
          .replaceFirst('icuin', 'icuuc')
          .replaceFirst('libicui18n', 'libicuuc');
      if (m != null && File(uc).existsSync()) {
        return (i18n, uc, int.parse(m.group(1) ?? m.group(2) ?? m.group(3)!));
      }
    }
  }
  stderr.writeln('ICU not found; pass the folder containing the ICU libraries');
  exit(2);
}

/// Nowruz JDN under the plain 33-year rule (what ICU 50-76 implements). Years here are positive.
int plainNowruz(int y) => 1948320 + 365 * (y - 1) + (8 * y + 21) ~/ 33;

ref.PersianDate monthDay(int y, int doy) => doy < 186
    ? ref.PersianDate(y, doy ~/ 31 + 1, doy % 31 + 1)
    : ref.PersianDate(y, (doy - 186) ~/ 30 + 7, (doy - 186) % 30 + 1);

ref.PersianDate plainFromJdn(int jdn) {
  var y = (jdn - 1948320) ~/ 366 + 1;
  while (jdn >= plainNowruz(y + 1)) {
    y++;
  }
  while (jdn < plainNowruz(y)) {
    y--;
  }
  return monthDay(y, jdn - plainNowruz(y));
}

void main(List<String> args) {
  final (i18nPath, ucPath, major) = findIcu(args.isNotEmpty ? args[0] : null);
  // Windows does not look for a DLL's dependencies in its own folder: load the data DLL (icudtNN) first.
  final data = ucPath.replaceFirstMapped(
      RegExp(r'icuuc(\d+)\.dll$'), (m) => 'icudt${m[1]}.dll');
  if (Platform.isWindows && File(data).existsSync()) DynamicLibrary.open(data);
  final uc = DynamicLibrary.open(ucPath), i18n = DynamicLibrary.open(i18nPath);
  // ICU usually suffixes its symbols with the major version (ucal_open_77).
  Pointer<NativeFunction<T>> sym<T extends Function>(
      DynamicLibrary lib, String name) {
    for (final s in ['${name}_$major', name]) {
      if (lib.providesSymbol(s)) return lib.lookup<NativeFunction<T>>(s);
    }
    throw ArgumentError('symbol $name not found');
  }

  final getVersion = sym<Void Function(Pointer<Uint8>)>(uc, 'u_getVersion')
      .asFunction<void Function(Pointer<Uint8>)>();
  final ucalOpen = sym<
          Pointer<Void> Function(Pointer<Void>, Int32, Pointer<Uint8>, Int32,
              Pointer<Int32>)>(i18n, 'ucal_open')
      .asFunction<
          Pointer<Void> Function(
              Pointer<Void>, int, Pointer<Uint8>, int, Pointer<Int32>)>();
  final ucalClear = sym<Void Function(Pointer<Void>)>(i18n, 'ucal_clear')
      .asFunction<void Function(Pointer<Void>)>();
  final ucalSet =
      sym<Void Function(Pointer<Void>, Int32, Int32)>(i18n, 'ucal_set')
          .asFunction<void Function(Pointer<Void>, int, int)>();
  final ucalGet = sym<Int32 Function(Pointer<Void>, Int32, Pointer<Int32>)>(
          i18n, 'ucal_get')
      .asFunction<int Function(Pointer<Void>, int, Pointer<Int32>)>();

  // One block of native memory: 4 bytes for the version, 4 for the status, then the locale string.
  final locale = 'fa_IR@calendar=persian'.codeUnits;
  final mem = _calloc(8 + locale.length + 1).cast<Uint8>();
  final version = mem;
  final status = (mem + 4).cast<Int32>();
  final localePtr = mem + 8;

  for (var i = 0; i < locale.length; i++) {
    localePtr[i] = locale[i];
  }
  localePtr[locale.length] = 0;
  getVersion(version);
  final versionText = [for (var i = 0; i < 4; i++) version[i]]
      .join('.')
      .replaceFirst(RegExp(r'(\.0)+$'), '');
  final cal = ucalOpen(nullptr, 0, localePtr, 0, status);
  if (status.value > 0 || cal == nullptr)
    throw StateError('ucal_open failed: ${status.value}');

  ref.PersianDate icuFromJdn(int jdn) {
    ucalClear(cal);
    ucalSet(cal, ucalJulianDay, jdn);
    return ref.PersianDate(ucalGet(cal, ucalExtendedYear, status),
        ucalGet(cal, ucalMonth, status) + 1, ucalGet(cal, ucalDate, status));
  }

  int icuToJdn(ref.PersianDate d) {
    ucalClear(cal);
    ucalSet(cal, ucalExtendedYear, d.year);
    ucalSet(cal, ucalMonth, d.month - 1); // ICU months are 0-based
    ucalSet(cal, ucalDate, d.day);
    return ucalGet(cal, ucalJulianDay, status);
  }

  final root =
      File.fromUri(Platform.script).parent.parent.parent.parent.parent.path;
  final years = File('$root/data/years.csv')
      .readAsLinesSync()
      .skip(1)
      .where((l) => l.isNotEmpty)
      .map((l) => l.split(','));
  final hasCorrections = major >= 77;
  var days = 0,
      referenceWrong = 0,
      toPersianWrong = 0,
      fromPersianWrong = 0,
      differs = 0;
  for (final r in years) {
    final year = int.parse(r[0]), nowruz = int.parse(r[4]);
    final length = r[1] == '1' ? 366 : 365;
    for (var doy = 0; doy < length; doy++, days++) {
      final jdn = nowruz + doy;
      final truth = monthDay(year, doy);
      if (ref.fromJdn(jdn) != truth ||
          ref.toJdn(year, truth.month, truth.day) != jdn) referenceWrong++;
      final expected = hasCorrections ? truth : plainFromJdn(jdn);
      final icu = icuFromJdn(jdn);
      if (icu != expected) toPersianWrong++;
      if (icu != truth) differs++;
      if (icuToJdn(expected) != jdn) fromPersianWrong++;
    }
  }
  final rule = hasCorrections ? 'the ground truth' : 'the plain 33-year rule';
  final ok = toPersianWrong + fromPersianWrong == 0
      ? 'yes (both directions)'
      : 'NO (G->P $toPersianWrong, P->G $fromPersianWrong)';
  print(
      'Dart ${Platform.version.split(' ').first} + ICU4C $versionText (dart:ffi): $days days (1178-3000 AP) | '
      'implementation wrong: $referenceWrong | ICU differs from ground truth on $differs days | ICU matches $rule: $ok');
  exit(referenceWrong + toPersianWrong + fromPersianWrong == 0 ? 0 : 1);
}

/// Zeroed native memory from the C runtime's calloc, so no package (package:ffi) is needed.
/// Allocated once and never freed: the program exits right after using it.
Pointer<Void> _calloc(int bytes) {
  final libc = Platform.isWindows
      ? DynamicLibrary.open('ucrtbase.dll')
      : DynamicLibrary.process();
  final calloc = libc.lookupFunction<Pointer<Void> Function(IntPtr, IntPtr),
      Pointer<Void> Function(int, int)>('calloc');
  final p = calloc(1, bytes);
  if (p == nullptr) throw StateError('out of memory');
  return p;
}
