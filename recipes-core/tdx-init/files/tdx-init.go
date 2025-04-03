package main

import (
	"log"
	"os"
	"path/filepath"
)

const (
	keyFile      = "/etc/searcher_key"
	deviceGlob   = "/dev/disk/by-path/*scsi-0:0:0:10"
	sshDir       = "/home/searcher/.ssh"
	mountPoint   = "/persistent"
	mapperName   = "cryptdisk"
	mapperDevice = "/dev/mapper/" + mapperName
	httpPort     = "8080"
)

var devicePath string

type Token struct {
	Type     string            `json:"type"`
	Keyslots []string          `json:"keyslots"`
	UserData map[string]string `json:"user_data"`
}

func main() {
	if len(os.Args) < 2 {
		log.Fatalln("Usage: tdx-init [wait-for-key|set-passphrase]")
	}

	os.Setenv("PATH", "/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin")

	devices, err := filepath.Glob(deviceGlob)
	if err != nil || len(devices) == 0 {
		log.Fatalln("Error: SCSI device not found")
	}
	devicePath = devices[0]

	switch os.Args[1] {
	case "wait-for-key":
		waitForKey()
	case "set-passphrase":
		setPassphrase()
	default:
		log.Fatalf("Unknown command: %s\n", os.Args[1])
	}
}
