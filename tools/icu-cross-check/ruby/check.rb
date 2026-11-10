# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT
# frozen_string_literal: true

# Compares ICU4C (called directly through Fiddle), this repository's Ruby implementation and the
# ground truth (data/years.csv) for every day of 1178-3000 AP, in both directions.
#
# ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table; for
# them the script checks that ICU matches the plain 33-year rule exactly instead.
#
# Usage:
#   ruby check.rb                     # finds the system ICU (Linux, macOS/Homebrew)
#   ruby check.rb /path/to/icu/lib    # folder with libicui18n.so.NN / icuinNN.dll (Windows)
#
# No gems needed: ICU's C API is called with Fiddle (Ruby standard library), integers only.

require 'fiddle'
require_relative '../../../implementations/ruby/persian_calendar'

ROOT = File.expand_path('../../..', __dir__)
UCAL_MONTH = 2
UCAL_DATE = 5
UCAL_EXTENDED_YEAR = 19
UCAL_JULIAN_DAY = 20

def find_icu(folder)
  dirs = folder ? [folder] : %w[/usr/lib/x86_64-linux-gnu /usr/lib/aarch64-linux-gnu /usr/lib64 /usr/lib
                                /opt/homebrew/opt/icu4c/lib /usr/local/opt/icu4c/lib]
  dirs.each do |dir|
    candidates = Dir.glob(File.join(dir, 'icuin[0-9]*.dll')) + Dir.glob(File.join(dir, 'libicui18n.so.[0-9]*')) +
                 Dir.glob(File.join(dir, 'libicui18n.[0-9]*.dylib'))
    candidates.sort.reverse_each do |i18n|
      major = File.basename(i18n)[/(?:icuin|so\.|i18n\.)(\d+)/, 1].to_i
      uc = i18n.sub('icuin', 'icuuc').sub('libicui18n', 'libicuuc')
      return [i18n, uc, major] if File.exist?(uc)
    end
  end
  abort 'ICU not found; pass the folder containing the ICU libraries'
end

i18n_path, uc_path, major = find_icu(ARGV[0])
if Gem.win_platform?
  dir = File.dirname(File.expand_path(i18n_path))
  ENV['PATH'] = "#{dir.tr('/', '\\')};#{ENV.fetch('PATH', '')}"
  RubyInstaller::Runtime.add_dll_directory(dir) if defined?(RubyInstaller::Runtime)
end
uc = Fiddle.dlopen(uc_path)
i18n = Fiddle.dlopen(i18n_path)

# ICU usually suffixes its symbols with the major version (ucal_open_77).
def icu_function(lib, name, major, args, ret)
  address = ["#{name}_#{major}", name].each do |symbol|
    break lib[symbol]
  rescue Fiddle::DLError
    next
  end
  abort "symbol #{name} not found" unless address.is_a?(Integer)
  Fiddle::Function.new(address, args, ret)
end

T = Fiddle
get_version = icu_function(uc, 'u_getVersion', major, [T::TYPE_VOIDP], T::TYPE_VOID)
ucal_open = icu_function(i18n, 'ucal_open', major, [T::TYPE_VOIDP, T::TYPE_INT, T::TYPE_VOIDP, T::TYPE_INT, T::TYPE_VOIDP], T::TYPE_VOIDP)
ucal_clear = icu_function(i18n, 'ucal_clear', major, [T::TYPE_VOIDP], T::TYPE_VOID)
ucal_set = icu_function(i18n, 'ucal_set', major, [T::TYPE_VOIDP, T::TYPE_INT, T::TYPE_INT], T::TYPE_VOID)
ucal_get = icu_function(i18n, 'ucal_get', major, [T::TYPE_VOIDP, T::TYPE_INT, T::TYPE_VOIDP], T::TYPE_INT)

version_buffer = Fiddle::Pointer.malloc(4, Fiddle::RUBY_FREE)
get_version.call(version_buffer)
version = version_buffer[0, 4].bytes.join('.').sub(/(\.0)+\z/, '')
status = Fiddle::Pointer.malloc(4, Fiddle::RUBY_FREE)
status[0, 4] = [0].pack('l')
cal = ucal_open.call(nil, 0, "fa_IR@calendar=persian\0", 0, status)
abort "ucal_open failed: #{status[0, 4].unpack1('l')}" if status[0, 4].unpack1('l').positive?

icu_from_jdn = lambda do |jdn|
  ucal_clear.call(cal)
  ucal_set.call(cal, UCAL_JULIAN_DAY, jdn)
  y, m, d = [UCAL_EXTENDED_YEAR, UCAL_MONTH, UCAL_DATE].map { |f| ucal_get.call(cal, f, status) }
  [y, m + 1, d] # ICU months are 0-based
end
icu_to_jdn = lambda do |y, m, d|
  ucal_clear.call(cal)
  ucal_set.call(cal, UCAL_EXTENDED_YEAR, y)
  ucal_set.call(cal, UCAL_MONTH, m - 1)
  ucal_set.call(cal, UCAL_DATE, d)
  ucal_get.call(cal, UCAL_JULIAN_DAY, status)
end

# The plain 33-year rule (what ICU 50-76 implements).
plain_nowruz = ->(y) { 1_948_320 + 365 * (y - 1) + (8 * y + 21) / 33 } # Ruby's / is floor division
month_day = ->(y, doy) { doy < 186 ? [y, doy / 31 + 1, doy % 31 + 1] : [y, (doy - 186) / 30 + 7, (doy - 186) % 30 + 1] }
plain_from_jdn = lambda do |jdn|
  y = (jdn - 1_948_320) / 366 + 1
  y += 1 while jdn >= plain_nowruz.call(y + 1)
  y -= 1 while jdn < plain_nowruz.call(y)
  month_day.call(y, jdn - plain_nowruz.call(y))
end

has_corrections = major >= 77
days = ref_wrong = to_persian_wrong = from_persian_wrong = differs = 0
File.readlines(File.join(ROOT, 'data', 'years.csv'), chomp: true).drop(1).each do |line|
  year, leap, _, _, nowruz, = line.split(',')
  year = year.to_i
  nowruz = nowruz.to_i
  (leap == '1' ? 366 : 365).times do |doy|
    days += 1
    jdn = nowruz + doy
    truth = month_day.call(year, doy)
    p = PersianCalendar.from_jdn(jdn)
    ref_wrong += 1 unless [p.year, p.month, p.day] == truth && PersianCalendar.to_jdn(*truth) == jdn
    expected = has_corrections ? truth : plain_from_jdn.call(jdn)
    icu = icu_from_jdn.call(jdn)
    to_persian_wrong += 1 unless icu == expected
    differs += 1 unless icu == truth
    from_persian_wrong += 1 unless icu_to_jdn.call(*expected) == jdn
  end
end

rule = has_corrections ? 'the ground truth' : 'the plain 33-year rule'
ok = to_persian_wrong + from_persian_wrong == 0 ? 'yes (both directions)' : "NO (G->P #{to_persian_wrong}, P->G #{from_persian_wrong})"
puts "Ruby #{RUBY_VERSION} + ICU4C #{version} (Fiddle): #{days} days (1178-3000 AP) | implementation wrong: #{ref_wrong} | " \
     "ICU differs from ground truth on #{differs} days | ICU matches #{rule}: #{ok}"
exit(ref_wrong + to_persian_wrong + from_persian_wrong == 0 ? 0 : 1)
