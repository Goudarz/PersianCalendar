// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

/// Solar Hijri (Persian, Jalali, Shamsi) calendar.
///
/// Algorithm: 33-year arithmetic rule + 78-year correction table,
/// the same algorithm as ICU 77.1+ and ICU4X.
/// Matches the astronomical calendar (52.5 E meridian) for 1178-3000 AP.
/// Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
///
/// Inputs outside [minYear]..[maxYear] / [minJdn]..[maxJdn] throw [RangeError];
/// invalid dates throw [ArgumentError]. Within that range every intermediate
/// value fits in 32 bits, so results are exact on the web (JavaScript numbers) too.
library;

class PersianDate {
  final int year;

  /// 1 = Farvardin ... 12 = Esfand
  final int month;
  final int day;

  const PersianDate(this.year, this.month, this.day);

  @override
  bool operator ==(Object other) =>
      other is PersianDate &&
      other.year == year &&
      other.month == month &&
      other.day == day;

  @override
  int get hashCode => Object.hash(year, month, day);

  @override
  String toString() => 'PersianDate($year, $month, $day)';
}

/// Years checked against the astronomical calendar.
const int verifiedMinYear = 1178;
const int verifiedMaxYear = 3000;

/// Accepted input range.
const int minYear = -100000;
const int maxYear = 100000;

/// 1 Farvardin -100000 (Gregorian -99379-03-29).
const int minJdn = -34576287;

/// 29 Esfand 100000 (Gregorian 100622-03-13).
const int maxJdn = 38472562;

const int _epochJdn = 1948320;
const int _unixEpochJdn = 2440588; // JDN of 1970-01-01

/// Years the 33-year rule calls leap but are not; the next year is leap instead.
const Set<int> nonLeapCorrection = {
  1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931, //
  1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
  2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
  2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
  2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
  2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
  2946, 2950, 2954, 2979, 2983, 2987,
};

const List<String> monthNamesFa = [
  'فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور', //
  'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند',
];
const List<String> monthNamesEn = [
  'Farvardin', 'Ordibehesht', 'Khordad', 'Tir', 'Mordad', //
  'Shahrivar', 'Mehr', 'Aban', 'Azar', 'Dey', 'Bahman', 'Esfand',
];
const List<String> weekdayNamesFa = [
  'شنبه', 'یکشنبه', 'دوشنبه', 'سه‌شنبه', //
  'چهارشنبه', 'پنجشنبه', 'جمعه',
];
const List<String> weekdayNamesEn = [
  'Saturday', 'Sunday', 'Monday', 'Tuesday', //
  'Wednesday', 'Thursday', 'Friday',
];

// Dart's ~/ truncates toward zero; % is already non-negative for b > 0.
int _fdiv(int a, int b) => (a - a % b) ~/ b;

void _checkYear(int year) =>
    RangeError.checkValueInInterval(year, minYear, maxYear, 'year');

void _checkJdn(int jdn) =>
    RangeError.checkValueInInterval(jdn, minJdn, maxJdn, 'jdn');

bool isLeapYear(int year) {
  _checkYear(year);
  if (nonLeapCorrection.contains(year)) return false;
  if (nonLeapCorrection.contains(year - 1)) return true;
  return (25 * year + 11) % 33 < 8;
}

int daysInMonth(int year, int month) {
  _checkYear(year);
  if (month < 1 || month > 12) throw RangeError.range(month, 1, 12, 'month');
  if (month <= 6) return 31;
  if (month <= 11) return 30;
  return isLeapYear(year) ? 30 : 29;
}

/// True for a real Persian date in the accepted range; never throws.
bool isValidDate(int year, int month, int day) =>
    year >= minYear &&
    year <= maxYear &&
    month >= 1 &&
    month <= 12 &&
    day >= 1 &&
    day <= daysInMonth(year, month);

int _nowruzJdn(int year) {
  var jdn = _epochJdn + 365 * (year - 1) + _fdiv(8 * year + 21, 33);
  if (nonLeapCorrection.contains(year - 1)) jdn -= 1;
  return jdn;
}

int _daysBeforeMonth(int month) =>
    month <= 7 ? 31 * (month - 1) : 30 * (month - 1) + 6;

/// Julian Day Number of a Persian date.
int toJdn(int year, int month, int day) {
  if (!isValidDate(year, month, day)) {
    throw ArgumentError('invalid Persian date: $year/$month/$day');
  }
  return _nowruzJdn(year) + _daysBeforeMonth(month) + day - 1;
}

PersianDate fromJdn(int jdn) {
  _checkJdn(jdn);
  var year = 1 + _fdiv(33 * (jdn - _epochJdn) + 3, 12053);
  while (jdn >= _nowruzJdn(year + 1)) {
    year++;
  }
  while (jdn < _nowruzJdn(year)) {
    year--;
  }
  var doy = jdn - _nowruzJdn(year); // 0-based
  if (doy < 186) return PersianDate(year, doy ~/ 31 + 1, doy % 31 + 1);
  doy -= 186;
  return PersianDate(year, doy ~/ 30 + 7, doy % 30 + 1);
}

/// Persian date of the calendar day of [date] (its own year/month/day fields,
/// so convert with toLocal()/toUtc() first to pick the time zone).
PersianDate fromDateTime(DateTime date) {
  final utcMidnight = DateTime.utc(date.year, date.month, date.day);
  // Exact: midnight UTC is a whole number of days since the epoch.
  return fromJdn(
    utcMidnight.millisecondsSinceEpoch ~/ 86400000 + _unixEpochJdn,
  );
}

/// Midnight UTC of the Persian date's Gregorian day.
DateTime toDateTimeUtc(int year, int month, int day) =>
    DateTime.fromMillisecondsSinceEpoch(
      (toJdn(year, month, day) - _unixEpochJdn) * 86400000,
      isUtc: true,
    );

/// 0 = Saturday (شنبه) ... 6 = Friday (جمعه).
int dayOfWeek(int year, int month, int day) =>
    (toJdn(year, month, day) + 2) % 7;

/// 1 ... 365/366.
int dayOfYear(int year, int month, int day) {
  if (!isValidDate(year, month, day)) {
    throw ArgumentError('invalid Persian date: $year/$month/$day');
  }
  return _daysBeforeMonth(month) + day;
}
