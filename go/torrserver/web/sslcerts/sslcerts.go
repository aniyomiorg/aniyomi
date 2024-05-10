package sslcerts

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/pem"
	"errors"
	"math/big"
	"net"
	"os"
	"path/filepath"
	"time"

	"server/log"
	"server/settings"
)

func generateSelfSignedCert(ips []string) ([]byte, []byte, error) {
	priv, err := ecdsa.GenerateKey(elliptic.P384(), rand.Reader)
	if err != nil {
		return nil, nil, err
	}

	notBefore := time.Now()
	notAfter := notBefore.Add(365 * 24 * time.Hour) // Valid for 1 year

	serialNumber, err := rand.Int(rand.Reader, new(big.Int).Lsh(big.NewInt(1), 128))
	if err != nil {
		return nil, nil, err
	}

	netIps := make([]net.IP, 0)
	if len(ips) != 0 {
		for _, ip := range ips {
			netIps = append(netIps, net.ParseIP(ip))
		}
	}

	template := x509.Certificate{
		SerialNumber: serialNumber,
		Subject: pkix.Name{
			Organization: []string{"TorrServer"},
		},
		NotBefore:             notBefore,
		NotAfter:              notAfter,
		KeyUsage:              x509.KeyUsageKeyEncipherment | x509.KeyUsageDigitalSignature,
		ExtKeyUsage:           []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth},
		BasicConstraintsValid: true,
		DNSNames:              []string{"localhost"},
		IPAddresses:           netIps,
	}

	certDER, err := x509.CreateCertificate(rand.Reader, &template, &template, &priv.PublicKey, priv)
	if err != nil {
		return nil, nil, err
	}

	certPEM := pem.EncodeToMemory(&pem.Block{Type: "CERTIFICATE", Bytes: certDER})

	privBytes, err := x509.MarshalECPrivateKey(priv)
	if err != nil {
		return nil, nil, err
	}

	privPEM := pem.EncodeToMemory(&pem.Block{Type: "EC PRIVATE KEY", Bytes: privBytes})

	return certPEM, privPEM, nil
}

func MakeCertKeyFiles(ips []string) (string, string) {
	certPEM, privPEM, err := generateSelfSignedCert(ips)
	if err != nil {
		log.TLogln("Error generating certificate:", err)
		os.Exit(1)
	}
	certFile, err := os.Create(filepath.Join(settings.Path, "server.pem"))
	if err != nil {
		log.TLogln("Error creating certificate file:", err)
		os.Exit(1)
	}
	defer certFile.Close()

	privFile, err := os.Create(filepath.Join(settings.Path, "server.key"))
	if err != nil {
		log.TLogln("Error creating private key file:", err)
		os.Exit(1)
	}
	defer privFile.Close()

	_, err = certFile.Write(certPEM)
	if err != nil {
		log.TLogln("Error writing certificate file:", err)
		os.Exit(1)
	}
	_, err = privFile.Write(privPEM)
	if err != nil {
		log.TLogln("Error writing private key file:", err)
		os.Exit(1)
	}
	log.TLogln("Self-signed certificate and private key generated successfully.")

	return getAbsPath(certFile.Name()), getAbsPath(privFile.Name())
}

func getAbsPath(fileName string) string {
	filePath, err := filepath.Abs(fileName)
	if err != nil {
		log.TLogln("Error getting absolute path:", err)
		os.Exit(1)
	}
	return filePath
}

func VerifyCertKeyFiles(certFile, keyFile, port string) error {
	// Load the certificate and key
	cert, err := tls.LoadX509KeyPair(certFile, keyFile)
	if err != nil {
		return err
	}
	// Check if the certificate chain is expired
	for _, cert := range cert.Certificate {
		x509Cert, err := x509.ParseCertificate(cert)
		if err != nil {
			return err
		}
		if x509Cert.NotAfter.Before(time.Now()) {
			return errors.New("certificate has expired")
		}
	}
	// Create a TLS configuration
	config := tls.Config{
		Certificates: []tls.Certificate{cert},
	}
	// Create a listener to check the certificate and key
	ln, err := tls.Listen("tcp", ":"+port, &config)
	if err != nil {
		return err
	}
	defer ln.Close()
	log.TLogln("Certificate and key are valid.")
	return nil
}
