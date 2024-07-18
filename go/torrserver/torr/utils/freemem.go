package utils

import (
	"runtime"
	"runtime/debug"
)

func FreeOSMem() {
	debug.FreeOSMemory()
}

func FreeOSMemGC() {
	runtime.GC()
	debug.FreeOSMemory()
}
