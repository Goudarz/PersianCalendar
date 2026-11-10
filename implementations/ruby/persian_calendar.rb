# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT

# frozen_string_literal: true

# Solar Hijri (Persian, Jalali, Shamsi) calendar.
#
# Algorithm: 33-year arithmetic rule + 78-year correction table,
# the same algorithm as ICU 77.1+ and ICU4X. Matches the astronomical calendar (52.5 E meridian)
# for 1178-3000 AP. Months are 1-based; weekdays 0 = Saturday ... 6 = Friday.
#
# Inputs outside MIN_YEAR..MAX_YEAR / MIN_JDN..MAX_JDN raise RangeError; non-Integers
# raise TypeError; invalid dates raise ArgumentError.
#
# Ruby's Integer#/ and #% already use floor semantics, which the formulas need.

require 'date'
require 'set'

module PersianCalendar
  PersianDate = Struct.new(:year, :month, :day)

  # Years checked against the astronomical calendar.
  VERIFIED_MIN_YEAR = 1178
  VERIFIED_MAX_YEAR = 3000

  # Accepted input range.
  MIN_YEAR = -100_000
  MAX_YEAR = 100_000
  MIN_JDN = -34_576_287 # 1 Farvardin -100000 (Gregorian -99379-03-29)
  MAX_JDN = 38_472_562  # 29 Esfand 100000 (Gregorian 100622-03-13)

  EPOCH_JDN = 1_948_320
  private_constant :EPOCH_JDN

  # Years the 33-year rule calls leap but are not; the next year is leap instead.
  NON_LEAP_CORRECTION = Set[
    1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898, 1931,
    1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191, 2195, 2224,
    2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360, 2389, 2393, 2422,
    2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554, 2558, 2587, 2591, 2620,
    2624, 2653, 2657, 2686, 2690, 2719, 2723, 2748, 2752, 2756, 2781, 2785,
    2789, 2818, 2822, 2847, 2851, 2855, 2880, 2884, 2888, 2913, 2917, 2921,
    2946, 2950, 2954, 2979, 2983, 2987
  ].freeze

  MONTH_NAMES_FA = %w[فروردین اردیبهشت خرداد تیر مرداد شهریور مهر آبان آذر دی بهمن اسفند].freeze
  MONTH_NAMES_EN = %w[Farvardin Ordibehesht Khordad Tir Mordad Shahrivar Mehr Aban Azar Dey Bahman Esfand].freeze
  WEEKDAY_NAMES_FA = ['شنبه', 'یکشنبه', 'دوشنبه', "سه‌شنبه", 'چهارشنبه', 'پنجشنبه', 'جمعه'].freeze
  WEEKDAY_NAMES_EN = %w[Saturday Sunday Monday Tuesday Wednesday Thursday Friday].freeze

  module_function

  def check_int(name, value)
    raise TypeError, "#{name} must be an Integer, not #{value.class}" unless value.is_a?(Integer)
  end
  private_class_method :check_int

  def check_year(year)
    check_int('year', year)
    raise RangeError, "year out of range [#{MIN_YEAR}, #{MAX_YEAR}]: #{year}" unless (MIN_YEAR..MAX_YEAR).cover?(year)
  end
  private_class_method :check_year

  def check_jdn(jdn)
    check_int('jdn', jdn)
    raise RangeError, "jdn out of range [#{MIN_JDN}, #{MAX_JDN}]: #{jdn}" unless (MIN_JDN..MAX_JDN).cover?(jdn)
  end
  private_class_method :check_jdn

  def leap_year?(year)
    check_year(year)
    return false if NON_LEAP_CORRECTION.include?(year)
    return true if NON_LEAP_CORRECTION.include?(year - 1)

    (25 * year + 11) % 33 < 8
  end

  def days_in_month(year, month)
    check_year(year)
    check_int('month', month)
    raise ArgumentError, "month out of range: #{month}" unless (1..12).cover?(month)
    return 31 if month <= 6
    return 30 if month <= 11

    leap_year?(year) ? 30 : 29
  end

  # True for a real Persian date in the accepted range; never raises.
  def valid_date?(year, month, day)
    return false unless [year, month, day].all?(Integer)

    (MIN_YEAR..MAX_YEAR).cover?(year) && (1..12).cover?(month) && day >= 1 && day <= days_in_month(year, month)
  end

  def nowruz_jdn(year)
    jdn = EPOCH_JDN + 365 * (year - 1) + (8 * year + 21) / 33
    NON_LEAP_CORRECTION.include?(year - 1) ? jdn - 1 : jdn
  end
  private_class_method :nowruz_jdn

  def days_before_month(month)
    month <= 7 ? 31 * (month - 1) : 30 * (month - 1) + 6
  end
  private_class_method :days_before_month

  # Julian Day Number of a Persian date.
  def check_date(year, month, day)
    return if valid_date?(year, month, day)

    check_int('year', year)
    check_int('month', month)
    check_int('day', day)
    raise ArgumentError, "invalid Persian date: #{year}/#{month}/#{day}"
  end
  private_class_method :check_date

  def to_jdn(year, month, day)
    check_date(year, month, day)

    nowruz_jdn(year) + days_before_month(month) + day - 1
  end

  def from_jdn(jdn)
    check_jdn(jdn)
    year = 1 + (33 * (jdn - EPOCH_JDN) + 3) / 12_053
    year += 1 while jdn >= nowruz_jdn(year + 1)
    year -= 1 while jdn < nowruz_jdn(year)
    doy = jdn - nowruz_jdn(year) # 0-based
    return PersianDate.new(year, doy / 31 + 1, doy % 31 + 1) if doy < 186

    doy -= 186
    PersianDate.new(year, doy / 30 + 7, doy % 30 + 1)
  end

  # Date#jd is the Julian Day Number; Date::GREGORIAN makes the date proleptic Gregorian.
  def from_date(date)
    raise TypeError, "expected a Date, not #{date.class}" unless date.is_a?(Date)

    from_jdn(date.jd)
  end

  def to_date(year, month, day)
    Date.jd(to_jdn(year, month, day), Date::GREGORIAN)
  end

  # 0 = Saturday (شنبه) ... 6 = Friday (جمعه).
  def day_of_week(year, month, day)
    (to_jdn(year, month, day) + 2) % 7
  end

  # 1 ... 365/366.
  def day_of_year(year, month, day)
    check_date(year, month, day)

    days_before_month(month) + day
  end
end
