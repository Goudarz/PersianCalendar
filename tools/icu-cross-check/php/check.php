<?php
// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Compares ICU4C (PHP intl), this repository's PHP implementation and the ground truth
// (data/years.csv) for every day of 1178-3000 AP, in both directions.
// ICU 77.1+ must match the ground truth exactly. ICU 76.1 and older have no correction table;
// for them the script checks that ICU matches the plain 33-year rule exactly instead.
// Run: php check.php
declare(strict_types=1);

require __DIR__ . '/../../../implementations/php/PersianCalendar.php';

use PersianCalendar\PersianCalendar as Ref;

/** Nowruz JDN under the plain 33-year rule (what ICU 50-76 implements). */
function plainNowruz(int $y): int
{
    return 1948320 + 365 * ($y - 1) + intdiv(8 * $y + 21 - (((8 * $y + 21) % 33) + 33) % 33, 33);
}

/** @return array{int, int, int} */
function monthDay(int $year, int $doy): array
{
    return $doy < 186 ? [$year, intdiv($doy, 31) + 1, $doy % 31 + 1] : [$year, intdiv($doy - 186, 30) + 7, ($doy - 186) % 30 + 1];
}

/** @return array{int, int, int} */
function plainFromJdn(int $jdn): array
{
    $y = intdiv($jdn - 1948320, 366) + 1;
    while ($jdn >= plainNowruz($y + 1)) {
        $y++;
    }
    while ($jdn < plainNowruz($y)) {
        $y--;
    }
    return monthDay($y, $jdn - plainNowruz($y));
}

$hasCorrections = (int) explode('.', INTL_ICU_VERSION)[0] >= 77;
$cal = IntlCalendar::createInstance('UTC', 'fa_IR@calendar=persian');
$days = $refWrong = $toPersianWrong = $fromPersianWrong = $differsFromTruth = 0;
$lines = file(__DIR__ . '/../../../data/years.csv', FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
foreach (array_slice($lines, 1) as $line) {
    [$year, $leap, , , $nowruz] = explode(',', $line);
    $year = (int) $year;
    $length = $leap === '1' ? 366 : 365;
    for ($doy = 0; $doy < $length; $doy++, $days++) {
        $jdn = (int) $nowruz + $doy;
        $truth = monthDay($year, $doy);
        if (Ref::fromJdn($jdn) !== $truth || Ref::toJdn(...$truth) !== $jdn) {
            $refWrong++;
        }
        $expected = $hasCorrections ? $truth : plainFromJdn($jdn);
        $epochDay = $jdn - 2440588;
        $cal->clear();
        $cal->setTime($epochDay * 86400000.0);
        $icu = [$cal->get(IntlCalendar::FIELD_EXTENDED_YEAR), $cal->get(IntlCalendar::FIELD_MONTH) + 1, $cal->get(IntlCalendar::FIELD_DAY_OF_MONTH)];
        if ($icu !== $expected) {
            $toPersianWrong++;
        }
        if ($icu !== $truth) {
            $differsFromTruth++;
        }
        $cal->clear();
        $cal->set(IntlCalendar::FIELD_EXTENDED_YEAR, $expected[0]);
        $cal->set(IntlCalendar::FIELD_MONTH, $expected[1] - 1);
        $cal->set(IntlCalendar::FIELD_DAY_OF_MONTH, $expected[2]);
        if ((int) floor($cal->getTime() / 86400000) !== $epochDay) {
            $fromPersianWrong++;
        }
    }
}
printf(
    "PHP %s intl, ICU %s: %d days (1178-3000 AP) | implementation wrong: %d | ICU differs from ground truth on %d days | ICU matches %s: %s\n",
    PHP_VERSION,
    INTL_ICU_VERSION,
    $days,
    $refWrong,
    $differsFromTruth,
    $hasCorrections ? 'the ground truth' : 'the plain 33-year rule',
    $toPersianWrong + $fromPersianWrong === 0 ? 'yes (both directions)' : "NO (G->P $toPersianWrong, P->G $fromPersianWrong)"
);
exit($refWrong + $toPersianWrong + $fromPersianWrong === 0 ? 0 : 1);
