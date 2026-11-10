// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

// Evaluates Go libraries. Run: go mod tidy && go run .
package main

import (
	"fmt"
	"os"
	"strings"

	jalaali "github.com/jalaali/go-jalaali"
	ptime "github.com/yaa110/go-persian-calendar"
)

func main() {
	a := strings.Builder{}
	b := strings.Builder{}
	a.WriteString("# go-jalaali v0.1.0\n")
	b.WriteString("# go-persian-calendar v1.3.0\n")
	for y := 1100; y <= 3300; y++ {
		if gy, gm, gd, err := jalaali.ToGregorian(y, jalaali.Farvardin, 1); err == nil {
			fmt.Fprintf(&a, "%d %04d-%02d-%02d\n", y, gy, int(gm), gd)
		}
		func() {
			defer func() { recover() }()
			t := ptime.Date(y, ptime.Farvardin, 1, 12, 0, 0, 0, ptime.Iran()).Time()
			fmt.Fprintf(&b, "%d %s\n", y, t.Format("2006-01-02"))
		}()
	}
	os.WriteFile("../results/go-jalaali_v0.1.0.txt", []byte(a.String()), 0o644)
	os.WriteFile("../results/go-persian-calendar_v1.3.0.txt", []byte(b.String()), 0o644)
}
