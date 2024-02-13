package torr

import (
	"context"
	"fmt"
	"log"
	"net"
	"sync"

	"github.com/anacrolix/publicip"
	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"

	"server/settings"
	"server/torr/storage/torrstor"
	"server/torr/utils"
	"server/version"
)

type BTServer struct {
	config *torrent.ClientConfig
	client *torrent.Client

	storage *torrstor.Storage

	torrents map[metainfo.Hash]*Torrent

	mu sync.Mutex
}

var privateIPBlocks []*net.IPNet

func init() {
	for _, cidr := range []string{
		"127.0.0.0/8",    // IPv4 loopback
		"10.0.0.0/8",     // RFC1918
		"172.16.0.0/12",  // RFC1918
		"192.168.0.0/16", // RFC1918
		"169.254.0.0/16", // RFC3927 link-local
		"::1/128",        // IPv6 loopback
		"fe80::/10",      // IPv6 link-local
		"fc00::/7",       // IPv6 unique local addr
	} {
		_, block, err := net.ParseCIDR(cidr)
		if err != nil {
			panic(fmt.Errorf("parse error on %q: %v", cidr, err))
		}
		privateIPBlocks = append(privateIPBlocks, block)
	}
}

func NewBTS() *BTServer {
	bts := new(BTServer)
	bts.torrents = make(map[metainfo.Hash]*Torrent)
	return bts
}

func (bt *BTServer) Connect() error {
	bt.mu.Lock()
	defer bt.mu.Unlock()
	var err error
	bt.configure(context.TODO())
	bt.client, err = torrent.NewClient(bt.config)
	bt.torrents = make(map[metainfo.Hash]*Torrent)
	InitApiHelper(bt)
	return err
}

func (bt *BTServer) Disconnect() {
	bt.mu.Lock()
	defer bt.mu.Unlock()
	if bt.client != nil {
		bt.client.Close()
		bt.client = nil
		utils.FreeOSMemGC()
	}
}

func (bt *BTServer) configure(ctx context.Context) {
	blocklist, _ := utils.ReadBlockedIP()
	bt.config = torrent.NewDefaultClientConfig()

	bt.storage = torrstor.NewStorage(settings.BTsets.CacheSize)
	bt.config.DefaultStorage = bt.storage

	userAgent := "qBittorrent/4.3.9"
	peerID := "-qB4390-"
	upnpID := "TorrServer/" + version.Version
	cliVers := userAgent

	//	bt.config.AlwaysWantConns = true
	bt.config.Debug = settings.BTsets.EnableDebug
	bt.config.DisableIPv6 = !settings.BTsets.EnableIPv6
	bt.config.DisableTCP = settings.BTsets.DisableTCP
	bt.config.DisableUTP = settings.BTsets.DisableUTP
	//	https://github.com/anacrolix/torrent/issues/703
	//  bt.config.DisableWebtorrent = true // TODO: check memory usage
	//  bt.config.DisableWebseeds = false
	bt.config.NoDefaultPortForwarding = settings.BTsets.DisableUPNP
	bt.config.NoDHT = settings.BTsets.DisableDHT
	bt.config.DisablePEX = settings.BTsets.DisablePEX
	bt.config.NoUpload = settings.BTsets.DisableUpload
	bt.config.IPBlocklist = blocklist
	bt.config.Bep20 = peerID
	bt.config.PeerID = utils.PeerIDRandom(peerID)
	bt.config.UpnpID = upnpID
	bt.config.HTTPUserAgent = userAgent
	bt.config.ExtendedHandshakeClientVersion = cliVers
	bt.config.EstablishedConnsPerTorrent = settings.BTsets.ConnectionsLimit
	bt.config.TotalHalfOpenConns = 500
	// Encryption/Obfuscation
	bt.config.EncryptionPolicy = torrent.EncryptionPolicy{
		ForceEncryption: settings.BTsets.ForceEncrypt,
	}
	//	bt.config.HeaderObfuscationPolicy = torrent.HeaderObfuscationPolicy{
	//		RequirePreferred: settings.BTsets.ForceEncrypt,
	//		Preferred:        true,
	//	}
	if settings.BTsets.DownloadRateLimit > 0 {
		bt.config.DownloadRateLimiter = utils.Limit(settings.BTsets.DownloadRateLimit * 1024)
	}
	if settings.BTsets.UploadRateLimit > 0 {
		bt.config.UploadRateLimiter = utils.Limit(settings.BTsets.UploadRateLimit * 1024)
	}
	if settings.TorAddr != "" {
		log.Println("Set listen addr", settings.TorAddr)
		bt.config.SetListenAddr(settings.TorAddr)
	} else {
		if settings.BTsets.PeersListenPort > 0 {
			log.Println("Set listen port", settings.BTsets.PeersListenPort)
			bt.config.ListenPort = settings.BTsets.PeersListenPort
		} else {
			// lport := 32000
			// for {
			// 	log.Println("Check listen port", lport)
			// 	l, err := net.Listen("tcp", ":"+strconv.Itoa(lport))
			// 	if l != nil {
			// 		l.Close()
			// 	}
			// 	if err == nil {
			// 		break
			// 	}
			// 	lport++
			// }
			// log.Println("Set listen port", lport)
			log.Println("Set listen port to random autoselect (0)")
			bt.config.ListenPort = 0 // lport
		}
	}

	log.Println("Client config:", settings.BTsets)

	var err error

	// set public IPv4
	if settings.PubIPv4 != "" {
		if ip4 := net.ParseIP(settings.PubIPv4); ip4.To4() != nil && !isPrivateIP(ip4) {
			bt.config.PublicIp4 = ip4
		}
	}
	if bt.config.PublicIp4 == nil {
		bt.config.PublicIp4, err = publicip.Get4(ctx)
		if err != nil {
			log.Printf("error getting public ipv4 address: %v", err)
		}
	}
	if bt.config.PublicIp4 != nil {
		log.Println("PublicIp4:", bt.config.PublicIp4)
	}

	// set public IPv6
	if settings.PubIPv6 != "" {
		if ip6 := net.ParseIP(settings.PubIPv6); ip6.To16() != nil && ip6.To4() == nil && !isPrivateIP(ip6) {
			bt.config.PublicIp6 = ip6
		}
	}
	if bt.config.PublicIp6 == nil && settings.BTsets.EnableIPv6 {
		bt.config.PublicIp6, err = publicip.Get6(ctx)
		if err != nil {
			log.Printf("error getting public ipv6 address: %v", err)
		}
	}
	if bt.config.PublicIp6 != nil {
		log.Println("PublicIp6:", bt.config.PublicIp6)
	}
}

func (bt *BTServer) GetTorrent(hash torrent.InfoHash) *Torrent {
	if torr, ok := bt.torrents[hash]; ok {
		return torr
	}
	return nil
}

func (bt *BTServer) ListTorrents() map[metainfo.Hash]*Torrent {
	list := make(map[metainfo.Hash]*Torrent)
	for k, v := range bt.torrents {
		list[k] = v
	}
	return list
}

func (bt *BTServer) RemoveTorrent(hash torrent.InfoHash) bool {
	if torr, ok := bt.torrents[hash]; ok {
		return torr.Close()
	}
	return false
}

func isPrivateIP(ip net.IP) bool {
	if ip.IsLoopback() || ip.IsPrivate() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
		return true
	}

	for _, block := range privateIPBlocks {
		if block.Contains(ip) {
			return true
		}
	}
	return false
}

func getPublicIp4() net.IP {
	ifaces, err := net.Interfaces()
	if err != nil {
		log.Println("Error get public IPv4")
		return nil
	}
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
				if !isPrivateIP(ip) && ip.To4() != nil {
					return ip
				}
			}
		}
	}
	return nil
}

func getPublicIp6() net.IP {
	ifaces, err := net.Interfaces()
	if err != nil {
		log.Println("Error get public IPv6")
		return nil
	}
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
				if !isPrivateIP(ip) && ip.To16() != nil && ip.To4() == nil {
					return ip
				}
			}
		}
	}
	return nil
}
