// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

//! Position of the Sun, time scales, and apparent solar time.
//!
//! Moments are fractional R.D. days (R.D. 1.0 = 0:00 on 1 January 1 CE), in Universal Time
//! unless noted. The formulas are the standard published ones, as used in Reingold & Dershowitz,
//! *Calendrical Calculations* (4th ed., 2016):
//!
//! * apparent solar longitude: the 49-term series of Bretagnon & Simon, *Planetary Programs and
//!   Tables from -4000 to +2800* (1986), plus nutation and aberration;
//! * equation of time and obliquity: Meeus, *Astronomical Algorithms* (2nd ed., 1998);
//! * ΔT (TT − UT): Meeus (1600–1986) and the Espenak & Meeus polynomials (NASA) elsewhere.
//!
//! The operations are written in the same order as in those sources so that results are
//! reproducible to the last bit across implementations.

use crate::fixed;
use std::f64::consts::PI;

/// Mean tropical year in days.
pub const MEAN_TROPICAL_YEAR: f64 = 365.242189;

/// Evaluates a0 + x·(a1 + x·(a2 + …)).
fn poly(x: f64, coefficients: &[f64]) -> f64 {
    coefficients.iter().rev().fold(0.0, |acc, &a| a + x * acc)
}

/// Degrees + arcminutes + arcseconds.
fn angle(degrees: f64, minutes: f64, seconds: f64) -> f64 {
    degrees + (minutes + seconds / 60.0) / 60.0
}

fn radians(degrees: f64) -> f64 {
    degrees.rem_euclid(360.0) * PI / 180.0
}

fn sin_deg(degrees: f64) -> f64 {
    radians(degrees).sin()
}

fn cos_deg(degrees: f64) -> f64 {
    radians(degrees).cos()
}

fn tan_deg(degrees: f64) -> f64 {
    radians(degrees).tan()
}

/// `x` shifted into the range [a, b).
pub fn mod_range(x: f64, a: f64, b: f64) -> f64 {
    a + (x - a).rem_euclid(b - a)
}

/// Noon, 1 January 2000 (J2000.0).
fn j2000() -> f64 {
    0.5 + fixed::from_gregorian(2000, 1, 1) as f64
}

/// ΔT = Dynamical Time − Universal Time, in days, for moment `t`.
pub fn delta_t(t: f64) -> f64 {
    let year = fixed::gregorian_year(t.floor() as i64);
    let yf = year as f64;
    let c =
        (fixed::from_gregorian(year, 7, 1) - fixed::from_gregorian(1900, 1, 1)) as f64 / 36525.0;
    let seconds = |s: f64| s / 86400.0;
    match year {
        2051..=2150 => {
            seconds(-20.0 + 32.0 * ((yf - 1820.0) / 100.0).powi(2) + 0.5628 * (2150.0 - yf))
        }
        2006..=2050 => seconds(poly(yf - 2000.0, &[62.92, 0.32217, 0.005589])),
        1987..=2005 => seconds(poly(
            yf - 2000.0,
            &[
                63.86,
                0.3345,
                -0.060374,
                0.0017275,
                0.000651814,
                0.00002373599,
            ],
        )),
        1900..=1986 => poly(
            c,
            &[
                -0.00002, 0.000297, 0.025184, -0.181133, 0.553040, -0.861938, 0.677066, -0.212591,
            ],
        ),
        1800..=1899 => poly(
            c,
            &[
                -0.000009, 0.003844, 0.083563, 0.865736, 4.867575, 15.845535, 31.332267, 38.291999,
                28.316289, 11.636204, 2.043794,
            ],
        ),
        1700..=1799 => seconds(poly(
            yf - 1700.0,
            &[8.118780842, -0.005092142, 0.003336121, -0.0000266484],
        )),
        1600..=1699 => seconds(poly(
            yf - 1600.0,
            &[120.0, -0.9808, -0.01532, 0.000140272128],
        )),
        500..=1599 => seconds(poly(
            (yf - 1000.0) / 100.0,
            &[
                1574.2,
                -556.01,
                71.23472,
                0.319781,
                -0.8503463,
                -0.005050998,
                0.0083572073,
            ],
        )),
        -499..=499 => seconds(poly(
            yf / 100.0,
            &[
                10583.6,
                -1014.41,
                33.78311,
                -5.952053,
                -0.1798452,
                0.022174192,
                0.0090316521,
            ],
        )),
        _ => seconds(poly((yf - 1820.0) / 100.0, &[-20.0, 0.0, 32.0])),
    }
}

/// Julian centuries of Dynamical Time since J2000.0.
fn julian_centuries(t: f64) -> f64 {
    (t + delta_t(t) - j2000()) / 36525.0
}

/// Obliquity of the ecliptic, in degrees.
fn obliquity(t: f64) -> f64 {
    let c = julian_centuries(t);
    angle(23.0, 26.0, 21.448)
        + poly(
            c,
            &[
                0.0,
                angle(0.0, 0.0, -46.8150),
                angle(0.0, 0.0, -0.00059),
                angle(0.0, 0.0, 0.001813),
            ],
        )
}

/// Equation of time (apparent − mean solar time) as a fraction of a day, Meeus p. 185.
pub fn equation_of_time(t: f64) -> f64 {
    let c = julian_centuries(t);
    let lambda = poly(c, &[280.46645, 36000.76983, 0.0003032]);
    let anomaly = poly(c, &[357.52910, 35999.05030, -0.0001559, -0.00000048]);
    let e = poly(c, &[0.016708617, -0.000042037, -0.0000001236]);
    let y = tan_deg(obliquity(t) / 2.0).powi(2);
    let equation = (1.0 / 2.0 / PI)
        * (y * sin_deg(2.0 * lambda) - 2.0 * e * sin_deg(anomaly)
            + 4.0 * e * y * sin_deg(anomaly) * cos_deg(2.0 * lambda)
            - 0.5 * y * y * sin_deg(4.0 * lambda)
            - 1.25 * e * e * sin_deg(2.0 * anomaly));
    equation.signum() * equation.abs().min(0.5)
}

// Bretagnon & Simon: sum of x·sin(y + z·c), longitude in degrees.
const SOLAR_X: [f64; 49] = [
    403406.0, 195207.0, 119433.0, 112392.0, 3891.0, 2819.0, 1721.0, 660.0, 350.0, 334.0, 314.0,
    268.0, 242.0, 234.0, 158.0, 132.0, 129.0, 114.0, 99.0, 93.0, 86.0, 78.0, 72.0, 68.0, 64.0,
    46.0, 38.0, 37.0, 32.0, 29.0, 28.0, 27.0, 27.0, 25.0, 24.0, 21.0, 21.0, 20.0, 18.0, 17.0, 14.0,
    13.0, 13.0, 13.0, 12.0, 10.0, 10.0, 10.0, 10.0,
];
const SOLAR_Y: [f64; 49] = [
    270.54861, 340.19128, 63.91854, 331.26220, 317.843, 86.631, 240.052, 310.26, 247.23, 260.87,
    297.82, 343.14, 166.79, 81.53, 3.50, 132.75, 182.95, 162.03, 29.8, 266.4, 249.2, 157.6, 257.8,
    185.1, 69.9, 8.0, 197.1, 250.4, 65.3, 162.7, 341.5, 291.6, 98.5, 146.7, 110.0, 5.2, 342.6,
    230.9, 256.1, 45.3, 242.9, 115.2, 151.8, 285.3, 53.3, 126.6, 205.7, 85.9, 146.1,
];
const SOLAR_Z: [f64; 49] = [
    0.9287892,
    35999.1376958,
    35999.4089666,
    35998.7287385,
    71998.20261,
    71998.4403,
    36000.35726,
    71997.4812,
    32964.4678,
    -19.4410,
    445267.1117,
    45036.8840,
    3.1008,
    22518.4434,
    -19.9739,
    65928.9345,
    9038.0293,
    3034.7684,
    33718.148,
    3034.448,
    -2280.773,
    29929.992,
    31556.493,
    149.588,
    9037.750,
    107997.405,
    -4444.176,
    151.771,
    67555.316,
    31556.080,
    -4561.540,
    107996.706,
    1221.655,
    62894.167,
    31437.369,
    14578.298,
    -31931.757,
    34777.243,
    1221.999,
    62894.511,
    -4442.039,
    107997.909,
    119.066,
    16859.071,
    -4.578,
    26895.292,
    -39.127,
    12297.536,
    90073.778,
];

fn nutation(c: f64) -> f64 {
    let a = poly(c, &[124.90, -1934.134, 0.002063]);
    let b = poly(c, &[201.11, 72001.5377, 0.00057]);
    -0.004778 * sin_deg(a) - 0.0003667 * sin_deg(b)
}

fn aberration(c: f64) -> f64 {
    0.0000974 * cos_deg(177.63 + 35999.01848 * c) - 0.005575
}

/// Apparent geocentric longitude of the Sun at moment `t`, in degrees [0, 360).
pub fn solar_longitude(t: f64) -> f64 {
    let c = julian_centuries(t);
    let sum = (0..49).fold(0.0, |acc, i| {
        acc + SOLAR_X[i] * sin_deg(SOLAR_Y[i] + SOLAR_Z[i] * c)
    });
    let lambda = 282.7771834 + 36000.76953744 * c + 0.000005729577951308232 * sum;
    (lambda + aberration(c) + nutation(c)).rem_euclid(360.0)
}

/// Approximate moment at or before `t` when the solar longitude was last `lambda` degrees.
pub fn estimate_prior_solar_longitude(lambda: f64, t: f64) -> f64 {
    let rate = MEAN_TROPICAL_YEAR / 360.0;
    let tau = t - rate * (solar_longitude(t) - lambda).rem_euclid(360.0);
    let delta = mod_range(solar_longitude(tau) - lambda, -180.0, 180.0);
    t.min(tau - rate * delta)
}

/// Universal Time of true (apparent) noon on fixed date `date` at east longitude `longitude`.
pub fn true_noon(date: i64, longitude: f64) -> f64 {
    let zone = longitude / 360.0;
    let apparent = date as f64 + 0.5; // local apparent time
    let local_mean = apparent - equation_of_time(apparent - zone);
    local_mean - zone
}

/// The moment (UT) of the March equinox in Gregorian year `year`, found by bisection.
pub fn march_equinox(year: i64) -> f64 {
    let guess = fixed::from_gregorian(year, 3, 20) as f64;
    let (mut lo, mut hi) = (guess - 5.0, guess + 5.0);
    // On [lo, hi] the longitude goes from ~355° through 360°/0° to ~5°.
    for _ in 0..60 {
        let mid = (lo + hi) / 2.0;
        if solar_longitude(mid) < 180.0 {
            hi = mid;
        } else {
            lo = mid;
        }
    }
    hi
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn poly_is_horner() {
        assert_eq!(poly(2.0, &[1.0, 2.0, 3.0]), 1.0 + 2.0 * 2.0 + 3.0 * 4.0);
        assert_eq!(poly(5.0, &[]), 0.0);
    }

    #[test]
    fn equinox_2025() {
        // Officially announced moment of the 1404 new year: 2025-03-20 12:31:30 Iran time.
        let irst = march_equinox(2025) + 3.5 / 24.0;
        let date = irst.floor() as i64;
        let minutes = (irst - date as f64) * 24.0 * 60.0;
        assert_eq!(fixed::to_gregorian(date), (2025, 3, 20));
        assert!((minutes - (12.0 * 60.0 + 31.5)).abs() < 1.0, "{minutes}");
    }
}
