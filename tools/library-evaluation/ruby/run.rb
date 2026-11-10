# Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
# SPDX-License-Identifier: MIT

# Evaluates Ruby's parsi-date. Run: bundle install && ruby run.rb
require 'date'
require 'json'
require 'parsi-date'
require_relative '../../../implementations/ruby/persian_calendar'

name = "parsi-date #{Gem.loaded_specs['parsi-date'].version}"
file = File.join(__dir__, '..', 'results', name.gsub(/[^\w.+-]+/, '_'))
lines = ["# #{name}"]
(1100..3300).each do |y|
  lines << "#{y} #{Parsi::Date.new(y, 1, 1).to_gregorian.strftime('%Y-%m-%d')}"
rescue StandardError
  next
end
File.write("#{file}.txt", lines.join("\n") + "\n")

ref = PersianCalendar
days = g2p = p2g = 0
(ref.to_jdn(1300, 1, 1)..ref.to_jdn(1500, 12, 29)).each do |jdn|
  g = Date.jd(jdn, Date::GREGORIAN)
  p = ref.from_jdn(jdn)
  days += 1
  q = g.to_parsi
  g2p += 1 unless [q.year, q.month, q.day] == [p.year, p.month, p.day]
  begin
    p2g += 1 unless Parsi::Date.new(p.year, p.month, p.day).to_gregorian == g
  rescue StandardError
    p2g += 1
  end
end
File.write("#{file}.everyday.json", JSON.generate(library: name, days: days, g2pWrong: g2p, p2gWrong: p2g) + "\n")
puts name
