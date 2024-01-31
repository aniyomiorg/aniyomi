package models

import (
	"strings"
	"time"
)

const (
	CatMovie         = "Movie"
	CatSeries        = "Series"
	CatDocMovie      = "DocMovie"
	CatDocSeries     = "DocSeries"
	CatCartoonMovie  = "CartoonMovie"
	CatCartoonSeries = "CartoonSeries"
	CatTVShow        = "TVShow"
	CatAnime         = "Anime"

	Q_LOWER           = 0
	Q_WEBDL_720       = 100
	Q_BDRIP_720       = 101
	Q_BDRIP_HEVC_720  = 102
	Q_WEBDL_1080      = 200
	Q_BDRIP_1080      = 201
	Q_BDRIP_HEVC_1080 = 202
	Q_BDREMUX_1080    = 203
	Q_WEBDL_SDR_2160  = 300
	Q_WEBDL_HDR_2160  = 301
	Q_WEBDL_DV_2160   = 302
	Q_BDRIP_SDR_2160  = 303
	Q_BDRIP_HDR_2160  = 304
	Q_BDRIP_DV_2160   = 305
	Q_UHD_BDREMUX_SDR = 306
	Q_UHD_BDREMUX_HDR = 307
	Q_UHD_BDREMUX_DV  = 308

	Q_UNKNOWN = 0
	Q_A       = 1   // Авторский, по типу Гоблина или старых переводчиков
	Q_L1      = 100 // Любительский одноголосый закадровый
	Q_L2      = 101 // Любительский двухголосый закадровый
	Q_L       = 102 // Любительский 3-5 человек закадровый
	Q_LS      = 103 // Любительский студия
	Q_P1      = 200 // Професиональный одноголосый закадровый
	Q_P2      = 201 // Профессиональный двухголосый закадровый
	Q_P       = 202 // Профессиональный 3-5 человек закадровый
	Q_PS      = 203 // Профессиональный студия
	Q_D       = 300 // Официальное профессиональное многоголосое озвучивание
	Q_LICENSE = 301 // Лицензия
)

type TorrentDetails struct {
	Title        string
	Name         string
	Names        []string
	Categories   string
	Size         string
	CreateDate   time.Time
	Tracker      string
	Link         string
	Year         int
	Peer         int
	Seed         int
	Magnet       string
	Hash         string
	IMDBID       string
	VideoQuality int
	AudioQuality int
}

type TorrentFile struct {
	Name string
	Size int64
}

func (d TorrentDetails) GetNames() string {
	return strings.Join(d.Names, " ")
}
