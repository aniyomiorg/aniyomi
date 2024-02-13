package utils

import (
	"image"
	_ "image/gif"
	_ "image/jpeg"
	_ "image/png"
	"net/http"
	"strings"

	"golang.org/x/image/webp"

	"server/log"
)

func CheckImgUrl(link string) bool {
	if link == "" {
		return false
	}
	resp, err := http.Get(link)
	if err != nil {
		log.TLogln("Error check image:", err)
		return false
	}
	defer resp.Body.Close()
	if strings.HasSuffix(link, ".webp") {
		_, err = webp.Decode(resp.Body)
	} else {
		_, _, err = image.Decode(resp.Body)
	}
	if err != nil {
		log.TLogln("Error decode image:", err)
		return false
	}
	return err == nil
}
