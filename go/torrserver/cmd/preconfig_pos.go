//go:build !windows
// +build !windows

package main

import (
	"os"
	"os/signal"
	"syscall"

	"server/log"
	"server/settings"
)

func Preconfig(dkill bool) {
	if dkill {
		sigc := make(chan os.Signal, 1)
		signal.Notify(sigc,
			syscall.SIGHUP,
			syscall.SIGINT,
			syscall.SIGPIPE,
			syscall.SIGTERM,
			syscall.SIGQUIT)
		go func() {
			for s := range sigc {
				if dkill {
					if settings.BTsets.EnableDebug || s != syscall.SIGPIPE {
						log.TLogln("Signal catched:", s)
						log.TLogln("To stop server, close it from web / api")
					}
				}
			}
		}()
	}
}
