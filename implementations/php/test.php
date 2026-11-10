<?php
// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Checks PersianCalendar.php against /data. Run: php implementations/php/test.php
declare(strict_types=1);

require __DIR__ . '/PersianCalendar.php';

use PersianCalendar\PersianCalendar as PC;

$checks = 0;
function check(bool $ok, string $context): void
{
    global $checks;
    $checks++;
    if (!$ok) {
        fwrite(STDERR, "failed: $context\n");
        exit(1);
    }
}

function csv(string $name): array
{
    $lines = file(__DIR__ . "/../../data/$name", FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    return array_map(fn ($l) => explode(',', $l), array_slice($lines, 1));
}

foreach (csv('years.csv') as $r) {
    $y = (int) $r[0];
    $leap = $r[1] === '1';
    check(PC::isLeapYear($y) === $leap, "leap $y");
    check(PC::daysInMonth($y, 12) === ($leap ? 30 : 29), "esfand $y");
    check(vsprintf('%04d-%02d-%02d', PC::toGregorian($y, 1, 1)) === $r[3], "nowruz $y");
    check(PC::toJdn($y, 1, 1) === (int) $r[4], "jdn $y");
    check(PC::dayOfWeek($y, 1, 1) === (int) $r[5], "weekday $y");
    // every day of the year against the ground truth, both directions
    for ($doy = 0; $doy < (int) $r[2]; $doy++) {
        $p = $doy < 186 ? [$y, intdiv($doy, 31) + 1, $doy % 31 + 1] : [$y, intdiv($doy - 186, 30) + 7, ($doy - 186) % 30 + 1];
        $jdn = (int) $r[4] + $doy;
        check(PC::fromJdn($jdn) === $p && PC::toJdn(...$p) === $jdn, implode('/', $p));
    }
}

foreach (csv('test-vectors.csv') as $r) {
    $v = array_map('intval', $r);
    $p = [$v[0], $v[1], $v[2]];
    $g = [$v[3], $v[4], $v[5]];
    $ctx = implode('/', $p);
    check(PC::toGregorian(...$p) === $g, $ctx);
    check(PC::fromGregorian(...$g) === $p, $ctx);
    check(PC::toJdn(...$p) === $v[6], $ctx);
    check(PC::fromJdn($v[6]) === $p, $ctx);
    check(PC::dayOfWeek(...$p) === $v[7], $ctx);
}

$first = PC::toJdn(PC::VERIFIED_MIN_YEAR, 1, 1);
$last = PC::toJdn(PC::VERIFIED_MAX_YEAR, 12, PC::daysInMonth(PC::VERIFIED_MAX_YEAR, 12));
$prev = PC::fromJdn($first - 1);
for ($jdn = $first; $jdn <= $last; $jdn++) {
    $d = PC::fromJdn($jdn);
    check(PC::toJdn(...$d) === $jdn, "round trip $jdn");
    $consecutive = ($d[2] === $prev[2] + 1 && $d[1] === $prev[1] && $d[0] === $prev[0])
        || ($d[2] === 1 && (($d[1] === $prev[1] + 1 && $d[0] === $prev[0])
            || ($d[1] === 1 && $prev[1] === 12 && $d[0] === $prev[0] + 1)));
    check($consecutive, "consecutive $jdn");
    $prev = $d;
}

// Extrapolated years: years <= 0 make intermediate values negative (floor division matters).
check(PC::toJdn(-1000, 1, 1) === 1582713 && PC::toJdn(0, 1, 1) === 1947955 && PC::toJdn(4000, 1, 1) === 3408925, 'fixed jdn');
check(PC::fromJdn(0) === [-5334, 9, 3] && PC::dayOfWeek(-1000, 1, 1) === 1, 'jdn 0 / weekday');
check(PC::isLeapYear(-3) && !PC::isLeapYear(-1000), 'negative leap years');
foreach ([[-1000, 1177], [3001, 4000]] as [$firstYear, $lastYear]) {
    $a = PC::toJdn($firstYear, 1, 1);
    $b = PC::toJdn($lastYear, 12, PC::daysInMonth($lastYear, 12));
    $p = PC::fromJdn($a - 1);
    for ($jdn = $a; $jdn <= $b; $jdn++) {
        $d = PC::fromJdn($jdn);
        check(PC::toJdn(...$d) === $jdn && PC::gregorianToJdn(...PC::jdnToGregorian($jdn)) === $jdn, "round trip $jdn");
        $consecutive = ($d[2] === $p[2] + 1 && $d[1] === $p[1] && $d[0] === $p[0])
            || ($d[2] === 1 && (($d[1] === $p[1] + 1 && $d[0] === $p[0])
                || ($d[1] === 1 && $p[1] === 12 && $d[0] === $p[0] + 1)));
        check($consecutive, "consecutive $jdn");
        $p = $d;
    }
}
check(PC::isValidDate(1403, 12, 30), '1403/12/30');
check(!PC::isValidDate(1404, 12, 30), '1404/12/30');
check(PC::isValidDate(1404, 6, 31), '1404/6/31');
check(!PC::isValidDate(1404, 7, 31), '1404/7/31');
check(!PC::isValidDate(1404, 13, 1), '1404/13/1');
check(!PC::isValidDate(1404, 0, 1), '1404/0/1');
check(!PC::isValidDate(1404, 1, 0), '1404/1/0');
try {
    PC::toJdn(1404, 12, 30);
    check(false, 'should throw');
} catch (InvalidArgumentException) {
    $checks++;
}
check(PC::fromGregorian(2025, 3, 21) === [1404, 1, 1], '2025-03-21');
check(PC::fromGregorian(1979, 2, 11) === [1357, 11, 22], '1979-02-11');
$instant = new DateTimeImmutable('2025-03-20T21:00:00Z');
check(PC::fromDateTime($instant->setTimezone(new DateTimeZone('Asia/Tehran'))) === [1404, 1, 1], 'Tehran');
check(PC::fromDateTime($instant) === [1403, 12, 30], 'UTC');
check(PC::dayOfYear(1404, 7, 1) === 187, 'doy');
check(PC::WEEKDAY_NAMES_EN[PC::dayOfWeek(1404, 1, 1)] === 'Friday', 'friday');

// Limits: exact boundaries; everything outside throws (no overflow to float, no hang).
function throws(callable $f, string $class = InvalidArgumentException::class): bool
{
    try {
        $f();
        return false;
    } catch (Throwable $e) {
        return $e instanceof $class;
    }
}
check(PC::toJdn(PC::MIN_YEAR, 1, 1) === PC::MIN_JDN && PC::toJdn(PC::MAX_YEAR, 12, 29) === PC::MAX_JDN, 'limits');
check(PC::fromJdn(PC::MIN_JDN) === [PC::MIN_YEAR, 1, 1] && PC::fromJdn(PC::MAX_JDN) === [PC::MAX_YEAR, 12, 29], 'limit dates');
check(PC::jdnToGregorian(PC::MIN_JDN) === [-99379, 3, 29] && PC::jdnToGregorian(PC::MAX_JDN) === [100622, 3, 13], 'limit gregorian');
foreach ([PC::MIN_JDN - 1, PC::MAX_JDN + 1, PHP_INT_MAX, PHP_INT_MIN, 2 ** 53] as $bad) {
    check(throws(fn () => PC::fromJdn($bad)) && throws(fn () => PC::jdnToGregorian($bad)), "jdn $bad");
}
foreach ([PC::MIN_YEAR - 1, PC::MAX_YEAR + 1, PHP_INT_MAX, PHP_INT_MIN] as $bad) {
    check(!PC::isValidDate($bad, 1, 1), "isValidDate $bad");
    check(throws(fn () => PC::toJdn($bad, 1, 1)) && throws(fn () => PC::isLeapYear($bad)), "year $bad");
}
foreach ([[2025, 2, 29], [2025, 2, 30], [2025, 13, 1], [2025, 0, 1], [2025, 4, 31], [2025, 1, 0],
          [100622, 3, 14], [PHP_INT_MAX, 1, 1]] as $bad) {
    check(throws(fn () => PC::gregorianToJdn(...$bad)) && throws(fn () => PC::fromGregorian(...$bad)), implode('-', $bad));
}
check(PC::gregorianToJdn(2024, 2, 29) === 2460370 && PC::gregorianToJdn(2000, 2, 29) === 2451604, 'Gregorian leap days');
check(throws(fn () => PC::gregorianToJdn(1900, 2, 29)), '1900 is not a Gregorian leap year');
check(throws(fn () => PC::fromJdn(1.5), TypeError::class), 'non-integer');
check(throws(fn () => PC::fromDateTime((new DateTimeImmutable())->setDate(200000, 1, 1))), 'far DateTime');
check(PC::fromDateTime((new DateTimeImmutable('2025-03-21T00:00:00Z'))->setDate(-5, 1, 1)) === PC::fromGregorian(-5, 1, 1), 'negative-year DateTime');
// Every year MIN_YEAR..MAX_YEAR: years are contiguous, first and last day round-trip, and the leap
// years sit where every other implementation (and ICU 77.1+) puts them (checksums).
$expected = PC::MIN_JDN;
$leaps = 0;
$nowruzSum = 0;
for ($y = PC::MIN_YEAR; $y <= PC::MAX_YEAR; $y++) {
    $nowruz = PC::toJdn($y, 1, 1);
    $leap = PC::isLeapYear($y) ? 1 : 0;
    if ($nowruz !== $expected || PC::fromJdn($nowruz) !== [$y, 1, 1] || PC::daysInMonth($y, 12) !== 29 + $leap
        || PC::fromJdn($nowruz + 364 + $leap) !== [$y, 12, 29 + $leap]) {
        check(false, "year $y");
    }
    $leaps += $leap;
    $nowruzSum += $nowruz;
    $expected = $nowruz + 365 + $leap;
}
check($expected - 1 === PC::MAX_JDN && $leaps === 48485 && $nowruzSum === 389592978180, 'full-range checksums');
echo "OK: $checks checks passed\n";
