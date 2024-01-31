//go:build windows
// +build windows

package main

import (
	"runtime"
	"syscall"
	"time"

	"server/torr"
	"server/torr/state"
)

const (
	EsSystemRequired   = 0x00000001
	EsAwaymodeRequired = 0x00000040 // Added for future improvements
	EsContinuous       = 0x80000000
)

var (
	pulseTime        = 60 * time.Second
	clearFlagTimeout = 3 * 60 * time.Second
)

func Preconfig(kill bool) {
	go func() {
		// need work on one thread because SetThreadExecutionState sets flag to thread. We need set and clear flag for same thread.
		runtime.LockOSThread()
		// don't sleep/hibernate windows
		kernel32 := syscall.NewLazyDLL("kernel32.dll")
		setThreadExecStateProc := kernel32.NewProc("SetThreadExecutionState")
		currentExecState := uintptr(EsContinuous)
		normalExecutionState := uintptr(EsContinuous)
		systemRequireState := uintptr(EsSystemRequired | EsContinuous)
		pulse := time.NewTicker(pulseTime)
		var clearFlagTime int64 = -1
		for {
			select {
			case <-pulse.C:
				{
					systemRequired := false
					for _, torrent := range torr.ListTorrent() {
						if torrent.Stat != state.TorrentInDB {
							systemRequired = true
							break
						}
					}
					if systemRequired && currentExecState != systemRequireState {
						// Looks like sending just EsSystemRequired to clear timer is broken in Win11.
						// Enable system required to avoid the system to idle to sleep.
						currentExecState = systemRequireState
						setThreadExecStateProc.Call(systemRequireState)
					}

					if !systemRequired && currentExecState != normalExecutionState {
						// Clear EXECUTION_STATE flags to disable away mode and allow the system to idle to sleep normally.

						// Avoid clear flag immediately to add time to start next episode
						if clearFlagTime == -1 {
							clearFlagTime = time.Now().Unix() + int64(clearFlagTimeout.Seconds())
						}

						if clearFlagTime >= time.Now().Unix() {
							clearFlagTime = -1
							currentExecState = normalExecutionState
							setThreadExecStateProc.Call(normalExecutionState)
						}
					}
				}
			}
		}
	}()
}
