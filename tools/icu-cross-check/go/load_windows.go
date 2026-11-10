// Copyright (C) 2026 Goudarz Jafari <hi@goudarzjafari.com>
// SPDX-License-Identifier: MIT

package main

import (
	"os"
	"path/filepath"
	"syscall"
)

func openLibrary(path string) (uintptr, error) {
	// ICU's DLLs load each other from their own folder.
	os.Setenv("PATH", filepath.Dir(path)+";"+os.Getenv("PATH"))
	h, err := syscall.LoadLibrary(path)
	return uintptr(h), err
}

func symbol(lib uintptr, name string) (uintptr, error) {
	return syscall.GetProcAddress(syscall.Handle(lib), name)
}
