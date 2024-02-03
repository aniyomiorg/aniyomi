package web

import (
	"net"
	"net/http"
	"os"
	"sort"

	"server/rutor"

	"github.com/gin-contrib/cors"
	"github.com/gin-contrib/location"
	"github.com/gin-gonic/gin"

	"server/settings"
	"server/web/msx"

	"server/log"
	"server/torr"
	"server/version"
	"server/web/api"
	"server/web/auth"
	"server/web/blocker"
	"server/web/sslcerts"

	swaggerFiles "github.com/swaggo/files"     // swagger embed files
	ginSwagger "github.com/swaggo/gin-swagger" // gin-swagger middleware
)

var (
	BTS        = torr.NewBTS()
	waitChan   = make(chan error)
	httpServer *http.Server
)

//	@title			Swagger Torrserver API
//	@version		{version.Version}
//	@description	Torrent streaming server.

//	@license.name	GPL 3.0

//	@BasePath	/

//	@securityDefinitions.basic	BasicAuth

// @externalDocs.description	OpenAPI
// @externalDocs.url			https://swagger.io/resources/open-api/
func Start() {
	log.TLogln("Start TorrServer " + version.Version + " torrent " + version.GetTorrentVersion())
	ips := getLocalIps()
	if len(ips) > 0 {
		log.TLogln("Local IPs:", ips)
	}
	err := BTS.Connect()
	if err != nil {
		log.TLogln("BTS.Connect() error!", err) // waitChan <- err
		os.Exit(1)                              // return
	}
	rutor.Start()

	gin.SetMode(gin.ReleaseMode)

	// corsCfg := cors.DefaultConfig()
	// corsCfg.AllowAllOrigins = true
	// corsCfg.AllowHeaders = []string{"*"}
	// corsCfg.AllowMethods = []string{"*"}
	// corsCfg.AllowPrivateNetwork = true
	corsCfg := cors.DefaultConfig()
	corsCfg.AllowAllOrigins = true
	corsCfg.AllowHeaders = []string{"Origin", "Content-Length", "Content-Type", "X-Requested-With", "Accept", "Authorization"}

	route := gin.New()
	route.Use(log.WebLogger(), blocker.Blocker(), gin.Recovery(), cors.New(corsCfg), location.Default())

	route.GET("/echo", echo)

	routeAuth := auth.SetupAuth(route)
	if routeAuth != nil {
		api.SetupRoute(routeAuth)
		msx.SetupRoute(routeAuth)
		//pages.SetupRoute(routeAuth)
	} else {
		api.SetupRoute(&route.RouterGroup)
		msx.SetupRoute(&route.RouterGroup)
		//pages.SetupRoute(&route.RouterGroup)
	}

	route.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))

	// check if https enabled
	if settings.Ssl {
		// if no cert and key files set in db/settings, generate new self-signed cert and key files
		if settings.BTsets.SslCert == "" || settings.BTsets.SslKey == "" {
			settings.BTsets.SslCert, settings.BTsets.SslKey = sslcerts.MakeCertKeyFiles(ips)
			log.TLogln("Saving path to ssl cert and key in db", settings.BTsets.SslCert, settings.BTsets.SslKey)
			settings.SetBTSets(settings.BTsets)
		}
		// verify if cert and key files are valid
		err = sslcerts.VerifyCertKeyFiles(settings.BTsets.SslCert, settings.BTsets.SslKey, settings.SslPort)
		// if not valid, generate new self-signed cert and key files
		if err != nil {
			log.TLogln("Error checking certificate and private key files:", err)
			settings.BTsets.SslCert, settings.BTsets.SslKey = sslcerts.MakeCertKeyFiles(ips)
			log.TLogln("Saving path to ssl cert and key in db", settings.BTsets.SslCert, settings.BTsets.SslKey)
			settings.SetBTSets(settings.BTsets)
		}
		go func() {
			log.TLogln("Start https server at port", settings.SslPort)
			waitChan <- route.RunTLS(":"+settings.SslPort, settings.BTsets.SslCert, settings.BTsets.SslKey)
		}()
	}

	httpServer = &http.Server{
		Addr:    ":" + settings.Port,
		Handler: route,
	}

	go func() {
		log.TLogln("Start http server at port", settings.Port)
		httpServer.ListenAndServe()
		//waitChan <- route.Run(" :" + settings.Port)
	}()
}

func Wait() error {
	return <-waitChan
}

func Stop() {
	if httpServer != nil {
		httpServer.Close()
	}
	BTS.Disconnect()
	//waitChan <- nil
}

// echo godoc
//
//	@Summary		Tests server status
//	@Description	Tests whether server is alive or not
//
//	@Tags			API
//
//	@Produce		plain
//	@Success		200	{string}	string	"Server version"
//	@Router			/echo [get]
func echo(c *gin.Context) {
	c.String(200, "%v", version.Version)
}

func getLocalIps() []string {
	ifaces, err := net.Interfaces()
	if err != nil {
		log.TLogln("Error get local IPs")
		return nil
	}
	var list []string
	for _, i := range ifaces {
		addrs, _ := i.Addrs()
		if i.Flags&net.FlagUp == net.FlagUp {
			for _, addr := range addrs {
				var ip net.IP
				switch v := addr.(type) {
				case *net.IPNet:
					ip = v.IP
				case *net.IPAddr:
					ip = v.IP
				}
				if !ip.IsLoopback() && !ip.IsLinkLocalUnicast() && !ip.IsLinkLocalMulticast() {
					list = append(list, ip.String())
				}
			}
		}
	}
	sort.Strings(list)
	return list
}
