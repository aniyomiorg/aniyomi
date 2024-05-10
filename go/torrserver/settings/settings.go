package settings

import (
	"os"
	"path/filepath"

	"server/log"
)

var (
	tdb      *TDB
	Path     string
	Port     string
	Ssl      bool
	SslPort  string
	ReadOnly bool
	HttpAuth bool
	SearchWA bool
	PubIPv4  string
	PubIPv6  string
	TorAddr  string
)

func InitSets(readOnly, searchWA bool) {
	ReadOnly = readOnly
	SearchWA = searchWA
	tdb = NewTDB()
	if tdb == nil {
		log.TLogln("Error open db:", filepath.Join(Path, "config.db"))
		os.Exit(1)
	}
	loadBTSets()
	Migrate()
}

func CloseDB() {
	tdb.CloseDB()
}
