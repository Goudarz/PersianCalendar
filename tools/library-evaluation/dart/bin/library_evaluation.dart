// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Evaluates shamsi_date. Run: dart pub get && dart run
import 'dart:io';

import 'package:shamsi_date/shamsi_date.dart';

void main() {
  const name = 'shamsi_date 1.1.1';
  final sb = StringBuffer('# $name\n');
  for (var y = 1100; y <= 3300; y++) {
    try {
      final g = Jalali(y, 1, 1).toGregorian();
      sb.writeln('$y ${g.year.toString().padLeft(4, '0')}-'
          '${g.month.toString().padLeft(2, '0')}-${g.day.toString().padLeft(2, '0')}');
    } catch (_) {}
  }
  File('../results/${name.replaceAll(' ', '_')}.txt').writeAsStringSync(sb.toString());
  print(name);
}
