// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Checks the implementation against /data. Run: dart test
import 'dart:io';

import 'package:persian_calendar/persian_calendar.dart';
import 'package:test/test.dart';

List<List<String>> csv(String name) =>
    File('../../data/$name')
        .readAsLinesSync()
        .skip(1)
        .where((l) => l.isNotEmpty)
        .map((l) => l.split(','))
        .toList();

String iso(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-'
    '${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

void main() {
  test('years.csv', () {
    for (final r in csv('years.csv')) {
      final y = int.parse(r[0]);
      final leap = r[1] == '1';
      expect(isLeapYear(y), leap, reason: '$y');
      expect(daysInMonth(y, 12), leap ? 30 : 29);
      expect(iso(toDateTimeUtc(y, 1, 1)), r[3]);
      expect(toJdn(y, 1, 1), int.parse(r[4]));
      expect(dayOfWeek(y, 1, 1), int.parse(r[5]));
      // every day of the year against the ground truth, both directions
      for (var doy = 0; doy < int.parse(r[2]); doy++) {
        final m = doy < 186 ? doy ~/ 31 + 1 : (doy - 186) ~/ 30 + 7;
        final d = doy < 186 ? doy % 31 + 1 : (doy - 186) % 30 + 1;
        final jdn = int.parse(r[4]) + doy;
        if (fromJdn(jdn) != PersianDate(y, m, d) || toJdn(y, m, d) != jdn) {
          fail('$y/$m/$d (JDN $jdn)');
        }
      }
    }
  }, testOn: 'vm'); // reads files with dart:io

  test('test-vectors.csv', () {
    for (final r in csv('test-vectors.csv')) {
      final v = r.map(int.parse).toList();
      final p = PersianDate(v[0], v[1], v[2]);
      final g = DateTime.utc(v[3], v[4], v[5]);
      expect(toDateTimeUtc(v[0], v[1], v[2]), g);
      expect(fromDateTime(g), p);
      expect(toJdn(v[0], v[1], v[2]), v[6]);
      expect(fromJdn(v[6]), p);
      expect(dayOfWeek(v[0], v[1], v[2]), v[7]);
    }
  }, testOn: 'vm');

  test('every day 1178..3000 round-trips', () {
    final first = toJdn(verifiedMinYear, 1, 1);
    final last = toJdn(verifiedMaxYear, 12, daysInMonth(verifiedMaxYear, 12));
    var prev = fromJdn(first - 1);
    for (var jdn = first; jdn <= last; jdn++) {
      final d = fromJdn(jdn);
      if (toJdn(d.year, d.month, d.day) != jdn) fail('round trip $jdn');
      final consecutive =
          (d.day == prev.day + 1 &&
              d.month == prev.month &&
              d.year == prev.year) ||
          (d.day == 1 &&
              ((d.month == prev.month + 1 && d.year == prev.year) ||
                  (d.month == 1 &&
                      prev.month == 12 &&
                      d.year == prev.year + 1)));
      if (!consecutive) fail('not consecutive at $jdn');
      prev = d;
    }
  });

  // Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
  test('extrapolated years', () {
    expect(toJdn(-1000, 1, 1), 1582713);
    expect(toJdn(0, 1, 1), 1947955);
    expect(toJdn(4000, 1, 1), 3408925);
    expect(fromJdn(0), const PersianDate(-5334, 9, 3));
    expect(dayOfWeek(-1000, 1, 1), 1);
    expect(isLeapYear(-3), isTrue);
    expect(isLeapYear(-1000), isFalse);
    for (final r in [
      [-1000, 1177],
      [3001, 4000],
    ]) {
      final first = toJdn(r[0], 1, 1);
      final last = toJdn(r[1], 12, daysInMonth(r[1], 12));
      var prev = fromJdn(first - 1);
      for (var jdn = first; jdn <= last; jdn++) {
        final d = fromJdn(jdn);
        if (toJdn(d.year, d.month, d.day) != jdn) fail('round trip $jdn');
        final consecutive =
            (d.day == prev.day + 1 &&
                d.month == prev.month &&
                d.year == prev.year) ||
            (d.day == 1 &&
                ((d.month == prev.month + 1 && d.year == prev.year) ||
                    (d.month == 1 &&
                        prev.month == 12 &&
                        d.year == prev.year + 1)));
        if (!consecutive) fail('not consecutive at $jdn');
        prev = d;
      }
    }
  });

  test('validation and known dates', () {
    expect(isValidDate(1403, 12, 30), isTrue);
    expect(isValidDate(1404, 12, 30), isFalse);
    expect(isValidDate(1404, 6, 31), isTrue);
    expect(isValidDate(1404, 7, 31), isFalse);
    expect(isValidDate(1404, 13, 1), isFalse);
    expect(isValidDate(1404, 0, 1), isFalse);
    expect(isValidDate(1404, 1, 0), isFalse);
    expect(() => toJdn(1404, 12, 30), throwsArgumentError);
    expect(
      fromDateTime(DateTime.utc(2025, 3, 21)),
      const PersianDate(1404, 1, 1),
    );
    expect(
      fromDateTime(DateTime.utc(1979, 2, 11)),
      const PersianDate(1357, 11, 22),
    );
    expect(dayOfYear(1404, 7, 1), 187);
    expect(weekdayNamesEn[dayOfWeek(1404, 1, 1)], 'Friday');
  });

  // Limits: exact boundaries; everything outside throws (no overflow, no hang).
  test('limits and bad input', () {
    expect(toJdn(minYear, 1, 1), minJdn);
    expect(toJdn(maxYear, 12, 29), maxJdn);
    expect(fromJdn(minJdn), const PersianDate(minYear, 1, 1));
    expect(fromJdn(maxJdn), const PersianDate(maxYear, 12, 29));
    expect(toDateTimeUtc(minYear, 1, 1), DateTime.utc(-99379, 3, 29));
    expect(toDateTimeUtc(maxYear, 12, 29), DateTime.utc(100622, 3, 13));
    for (final bad in [
      minJdn - 1,
      maxJdn + 1,
      9007199254740992,
      -9007199254740992,
    ]) {
      expect(() => fromJdn(bad), throwsRangeError);
    }
    for (final bad in [minYear - 1, maxYear + 1, 9007199254740992]) {
      expect(isValidDate(bad, 1, 1), isFalse);
      expect(() => toJdn(bad, 1, 1), throwsArgumentError);
      expect(() => isLeapYear(bad), throwsRangeError);
    }
    expect(() => fromDateTime(DateTime.utc(200000, 1, 1)), throwsRangeError);
    expect(() => monthNamesFa[0] = 'X', throwsUnsupportedError);
  });

  // Every year minYear..maxYear: years are contiguous, first and last day round-trip, and the leap
  // years sit where every other implementation (and ICU 77.1+) puts them (checksums).
  test('every year of the full range', () {
    var expected = minJdn, leaps = 0, nowruzSum = 0;
    for (var y = minYear; y <= maxYear; y++) {
      final nowruz = toJdn(y, 1, 1);
      final leap = isLeapYear(y) ? 1 : 0;
      if (nowruz != expected ||
          fromJdn(nowruz) != PersianDate(y, 1, 1) ||
          daysInMonth(y, 12) != 29 + leap ||
          fromJdn(nowruz + 364 + leap) != PersianDate(y, 12, 29 + leap)) {
        fail('year $y');
      }
      leaps += leap;
      nowruzSum += nowruz; // stays below 2^53, so exact on the web too
      expected = nowruz + 365 + leap;
    }
    expect([expected - 1, leaps, nowruzSum], [maxJdn, 48485, 389592978180]);
  });
}
