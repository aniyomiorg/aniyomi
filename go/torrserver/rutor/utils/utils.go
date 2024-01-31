package utils

import (
	"crypto/sha256"
	"encoding/hex"
	"os"
	"strings"
)

func ClearStr(str string) string {
	ret := ""
	str = strings.ToLower(str)
	for _, r := range str {
		if (r >= '0' && r <= '9') || (r >= 'a' && r <= 'z') || (r >= 'а' && r <= 'я') || r == 'ё' {
			ret = ret + string(r)
		}
	}
	return ret
}

func MD5File(fname string) string {
	f, err := os.Open(fname)
	if err != nil {
		return ""
	}

	defer f.Close()

	buf := make([]byte, 1024*1024)
	h := sha256.New()

	for {
		bytesRead, err := f.Read(buf)
		if err != nil {
			break
		}

		h.Write(buf[:bytesRead])
	}

	return hex.EncodeToString(h.Sum(nil))
}
