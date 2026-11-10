<?php
// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT
// Evaluates PHP intl (ICU) and morilog/jalali. Run: composer install && php run.php (needs ext-intl)
require __DIR__ . '/vendor/autoload.php';

$intlName = 'PHP ' . PHP_VERSION . ' intl (ICU ' . INTL_ICU_VERSION . ')';
$morName = 'morilog/jalali ' . \Composer\InstalledVersions::getPrettyVersion('morilog/jalali');
$out1 = "# $intlName\n";
$out2 = "# $morName\n";
$cal = IntlCalendar::createInstance('UTC', 'fa_IR@calendar=persian');
$greg = IntlCalendar::createInstance('UTC', 'en_US@calendar=gregorian');
$greg->setGregorianChange(-1e20); // proleptic Gregorian
for ($y = 1100; $y <= 3300; $y++) {
    $cal->clear();
    $cal->setDate($y, 0, 1);
    $greg->setTime($cal->getTime());
    $out1 .= sprintf("%d %04d-%02d-%02d\n", $y, $greg->get(IntlCalendar::FIELD_YEAR),
        $greg->get(IntlCalendar::FIELD_MONTH) + 1, $greg->get(IntlCalendar::FIELD_DAY_OF_MONTH));
    try {
        $g = (new \Morilog\Jalali\Jalalian($y, 1, 1))->toCarbon();
        $out2 .= $y . ' ' . $g->format('Y-m-d') . "\n";
    } catch (\Throwable $e) {
    }
}
$file = fn ($n) => __DIR__ . '/../results/' . preg_replace('/[^\w.+-]+/', '_', $n) . '.txt';
file_put_contents($file($intlName), $out1);
file_put_contents($file($morName), $out2);
echo "$intlName\n$morName\n";
