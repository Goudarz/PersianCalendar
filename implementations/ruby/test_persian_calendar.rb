# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT

# frozen_string_literal: true

# Checks persian_calendar.rb against /data. Run: ruby implementations/ruby/test_persian_calendar.rb
require 'minitest/autorun'
require_relative 'persian_calendar'

class TestPersianCalendar < Minitest::Test
  PC = PersianCalendar
  DATA = File.expand_path('../../data', __dir__)

  def csv(name)
    File.readlines(File.join(DATA, name), chomp: true).drop(1).reject(&:empty?).map { |l| l.split(',') }
  end

  def test_years
    csv('years.csv').each do |r|
      y = r[0].to_i
      leap = r[1] == '1'
      assert_equal leap, PC.leap_year?(y), "leap #{y}"
      assert_equal leap ? 30 : 29, PC.days_in_month(y, 12)
      assert_equal Date.iso8601(r[3]), PC.to_date(y, 1, 1)
      assert_equal r[4].to_i, PC.to_jdn(y, 1, 1)
      assert_equal r[5].to_i, PC.day_of_week(y, 1, 1)
      # every day of the year against the ground truth, both directions
      r[2].to_i.times do |doy|
        m, d = doy < 186 ? [doy / 31 + 1, doy % 31 + 1] : [(doy - 186) / 30 + 7, (doy - 186) % 30 + 1]
        jdn = r[4].to_i + doy
        raise "#{y}/#{m}/#{d}" unless PC.from_jdn(jdn) == PC::PersianDate.new(y, m, d) && PC.to_jdn(y, m, d) == jdn
      end
    end
  end

  def test_vectors
    csv('test-vectors.csv').each do |r|
      jy, jm, jd, gy, gm, gd, jdn, wd = r.map(&:to_i)
      p = PC::PersianDate.new(jy, jm, jd)
      g = Date.new(gy, gm, gd, Date::GREGORIAN)
      assert_equal g, PC.to_date(jy, jm, jd)
      assert_equal p, PC.from_date(g)
      assert_equal jdn, PC.to_jdn(jy, jm, jd)
      assert_equal p, PC.from_jdn(jdn)
      assert_equal wd, PC.day_of_week(jy, jm, jd)
    end
  end

  def test_every_day_round_trips
    first = PC.to_jdn(PC::VERIFIED_MIN_YEAR, 1, 1)
    last = PC.to_jdn(PC::VERIFIED_MAX_YEAR, 12, PC.days_in_month(PC::VERIFIED_MAX_YEAR, 12))
    prev = PC.from_jdn(first - 1)
    (first..last).each do |jdn|
      d = PC.from_jdn(jdn)
      raise "round trip #{jdn}" unless PC.to_jdn(d.year, d.month, d.day) == jdn

      consecutive = (d.day == prev.day + 1 && d.month == prev.month && d.year == prev.year) ||
                    (d.day == 1 && ((d.month == prev.month + 1 && d.year == prev.year) ||
                                    (d.month == 1 && prev.month == 12 && d.year == prev.year + 1)))
      raise "not consecutive at #{jdn}" unless consecutive

      prev = d
    end
  end

  # Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
  def test_extrapolated_years
    assert_equal 1_582_713, PC.to_jdn(-1000, 1, 1)
    assert_equal 1_947_955, PC.to_jdn(0, 1, 1)
    assert_equal 3_408_925, PC.to_jdn(4000, 1, 1)
    assert_equal PC::PersianDate.new(-5334, 9, 3), PC.from_jdn(0)
    assert_equal 1, PC.day_of_week(-1000, 1, 1)
    assert PC.leap_year?(-3)
    refute PC.leap_year?(-1000)
    [[-1000, 1177], [3001, 4000]].each do |first_year, last_year|
      first = PC.to_jdn(first_year, 1, 1)
      last = PC.to_jdn(last_year, 12, PC.days_in_month(last_year, 12))
      prev = PC.from_jdn(first - 1)
      (first..last).each do |jdn|
        d = PC.from_jdn(jdn)
        raise "round trip #{jdn}" unless PC.to_jdn(d.year, d.month, d.day) == jdn

        consecutive = (d.day == prev.day + 1 && d.month == prev.month && d.year == prev.year) ||
                      (d.day == 1 && ((d.month == prev.month + 1 && d.year == prev.year) ||
                                      (d.month == 1 && prev.month == 12 && d.year == prev.year + 1)))
        raise "not consecutive at #{jdn}" unless consecutive

        prev = d
      end
    end
  end

  def test_validation_and_known_dates
    assert PC.valid_date?(1403, 12, 30)
    refute PC.valid_date?(1404, 12, 30)
    assert PC.valid_date?(1404, 6, 31)
    refute PC.valid_date?(1404, 7, 31)
    refute PC.valid_date?(1404, 13, 1)
    refute PC.valid_date?(1404, 0, 1)
    refute PC.valid_date?(1404, 1, 0)
    assert_raises(ArgumentError) { PC.to_jdn(1404, 12, 30) }
    refute PC.valid_date?(1404, 1.5, 1) # not an integer
    assert_equal PC::PersianDate.new(1404, 1, 1), PC.from_date(Date.new(2025, 3, 21))
    assert_equal PC::PersianDate.new(1357, 11, 22), PC.from_date(Date.new(1979, 2, 11))
    assert_equal 187, PC.day_of_year(1404, 7, 1)
    assert_equal 'Friday', PC::WEEKDAY_NAMES_EN[PC.day_of_week(1404, 1, 1)]
  end

  # Limits: exact boundaries; everything outside raises (no huge Bignum loops, no Float results).
  def test_limits_and_bad_input
    assert_equal PC::MIN_JDN, PC.to_jdn(PC::MIN_YEAR, 1, 1)
    assert_equal PC::MAX_JDN, PC.to_jdn(PC::MAX_YEAR, 12, 29)
    assert_equal PC::PersianDate.new(PC::MIN_YEAR, 1, 1), PC.from_jdn(PC::MIN_JDN)
    assert_equal PC::PersianDate.new(PC::MAX_YEAR, 12, 29), PC.from_jdn(PC::MAX_JDN)
    [PC::MIN_JDN - 1, PC::MAX_JDN + 1, 2**64, -2**64].each do |bad|
      assert_raises(RangeError) { PC.from_jdn(bad) }
    end
    [PC::MIN_YEAR - 1, PC::MAX_YEAR + 1, 10**100].each do |bad|
      refute PC.valid_date?(bad, 1, 1)
      assert_raises(ArgumentError) { PC.to_jdn(bad, 1, 1) }
      assert_raises(RangeError) { PC.leap_year?(bad) }
    end
    assert_raises(TypeError) { PC.from_jdn(2_460_756.5) }
    assert_raises(TypeError) { PC.from_jdn('2460756') }
    assert_raises(TypeError) { PC.leap_year?(1404.0) }
    assert_raises(TypeError) { PC.to_jdn(1404, 1.5, 1) }
    assert_raises(TypeError) { PC.from_date('2025-03-21') }
    assert_raises(RangeError) { PC.from_date(Date.new(200_000, 1, 1)) }
    assert PC::MONTH_NAMES_FA.frozen? && PC::NON_LEAP_CORRECTION.frozen?
    assert_raises(FrozenError) { PC::MONTH_NAMES_FA[0] = 'X' }
  end

  # Every year MIN_YEAR..MAX_YEAR: years are contiguous, first and last day round-trip, and the leap
  # years sit where every other implementation (and ICU 77.1+) puts them (checksums).
  def test_every_year_of_the_full_range
    expected = PC::MIN_JDN
    leaps = 0
    nowruz_sum = 0
    (PC::MIN_YEAR..PC::MAX_YEAR).each do |y|
      nowruz = PC.to_jdn(y, 1, 1)
      leap = PC.leap_year?(y) ? 1 : 0
      unless nowruz == expected && PC.from_jdn(nowruz) == PC::PersianDate.new(y, 1, 1) &&
             PC.days_in_month(y, 12) == 29 + leap && PC.from_jdn(nowruz + 364 + leap) == PC::PersianDate.new(y, 12, 29 + leap)
        flunk "year #{y}"
      end
      leaps += leap
      nowruz_sum += nowruz
      expected = nowruz + 365 + leap
    end
    assert_equal [PC::MAX_JDN, 48_485, 389_592_978_180], [expected - 1, leaps, nowruz_sum]
  end
end
