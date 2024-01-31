package auth

import (
	"encoding/base64"
	"encoding/json"
	"net/http"
	"os"
	"path/filepath"
	"reflect"
	"strings"
	"unsafe"

	"github.com/gin-gonic/gin"

	"server/log"
	"server/settings"
)

func SetupAuth(engine *gin.Engine) *gin.RouterGroup {
	if !settings.HttpAuth {
		return nil
	}
	accs := getAccounts()
	if accs == nil {
		return nil
	}
	return engine.Group("/", BasicAuth(accs))
}

func getAccounts() gin.Accounts {
	buf, err := os.ReadFile(filepath.Join(settings.Path, "accs.db"))
	if err != nil {
		return nil
	}
	var accs gin.Accounts
	err = json.Unmarshal(buf, &accs)
	if err != nil {
		log.TLogln("Error parse accs.db", err)
	}
	return accs
}

type authPair struct {
	value string
	user  string
}
type authPairs []authPair

func (a authPairs) searchCredential(authValue string) (string, bool) {
	if authValue == "" {
		return "", false
	}
	for _, pair := range a {
		if pair.value == authValue {
			return pair.user, true
		}
	}
	return "", false
}

func BasicAuth(accounts gin.Accounts) gin.HandlerFunc {
	pairs := processAccounts(accounts)
	return func(c *gin.Context) {
		user, found := pairs.searchCredential(c.Request.Header.Get("Authorization"))
		if !found { // always accessible
			if strings.HasPrefix(c.FullPath(), "/stream") ||
				c.FullPath() == "/site.webmanifest" ||
				// https://github.com/YouROK/TorrServer/issues/172
				(strings.HasPrefix(c.FullPath(), "/play") && c.FullPath() != "/playlistall/all.m3u") ||
				(settings.SearchWA && strings.HasPrefix(c.FullPath(), "/search")) {
				c.Set("not_auth", true)
				return
			}
			c.Header("WWW-Authenticate", "Basic realm=Authorization Required")
			c.AbortWithStatus(http.StatusUnauthorized)
			return
		}
		c.Set(gin.AuthUserKey, user)
	}
}

func processAccounts(accounts gin.Accounts) authPairs {
	pairs := make(authPairs, 0, len(accounts))
	for user, password := range accounts {
		value := authorizationHeader(user, password)
		pairs = append(pairs, authPair{
			value: value,
			user:  user,
		})
	}
	return pairs
}

func authorizationHeader(user, password string) string {
	base := user + ":" + password
	return "Basic " + base64.StdEncoding.EncodeToString(StringToBytes(base))
}

func StringToBytes(s string) (b []byte) {
	sh := *(*reflect.StringHeader)(unsafe.Pointer(&s))
	bh := (*reflect.SliceHeader)(unsafe.Pointer(&b))
	bh.Data, bh.Len, bh.Cap = sh.Data, sh.Len, sh.Len
	return b
}
