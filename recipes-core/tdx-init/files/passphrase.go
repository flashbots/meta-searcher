package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/exec"
	"strings"
)

func setPassphrase() {
	// Check if already mounted
	if checkMounted() {
		log.Fatalln("Error: Encrypted disk already setup")
	}

	// Check if key exists
	if _, err := os.Stat(keyFile); err != nil {
		log.Fatalln("Error: SSH key not set. Provide public key via HTTP first.")
	}

	// Check if LUKS container exists
	cmd := exec.Command("cryptsetup", "isLuks", devicePath)
	isNewSetup := cmd.Run() != nil

	fmt.Print("Enter passphrase: ")
	var passphrase string
	fmt.Scanln(&passphrase)

	if isNewSetup {
		setupNewDisk(passphrase)
	} else {
		mountExistingDisk(passphrase)
	}
}

func setupNewDisk(passphrase string) {
	// Format with LUKS2
	cmd := exec.Command("cryptsetup", "luksFormat", "--type", "luks2", devicePath)
	cmd.Stdin = strings.NewReader(passphrase)
	if err := cmd.Run(); err != nil {
		log.Fatalf("Error formatting disk: %v\n", err)
	}

	// Open the LUKS container
	cmd = exec.Command("cryptsetup", "open", devicePath, mapperName)
	cmd.Stdin = strings.NewReader(passphrase)
	if err := cmd.Run(); err != nil {
		log.Fatalf("Error opening LUKS device: %v\n", err)
	}

	// Create ext4 filesystem
	if err := exec.Command("mkfs.ext4", mapperDevice).Run(); err != nil {
		exec.Command("cryptsetup", "close", mapperName).Run()
		log.Fatalf("Error creating filesystem: %v\n", err)
	}

	// Mount the filesystem
	os.MkdirAll(mountPoint, 0755)
	if err := exec.Command("mount", mapperDevice, mountPoint).Run(); err != nil {
		exec.Command("cryptsetup", "close", mapperName).Run()
		log.Fatalf("Error mounting filesystem: %v\n", err)
	}

	// Import the SSH key as token
	key, _ := os.ReadFile(keyFile)
	token := Token{
		Type:     "user",
		Keyslots: []string{},
		UserData: map[string]string{
			"metadata": base64.StdEncoding.EncodeToString(key),
		},
	}

	tokenJSON, _ := json.Marshal(token)
	tmpFile, err := os.CreateTemp("", "luks-token-*.json")
	if err != nil {
		cleanupMount()
		log.Fatalf("Error creating temp file: %v\n", err)
	}
	defer os.Remove(tmpFile.Name())

	if _, err := tmpFile.Write(tokenJSON); err != nil {
		cleanupMount()
		log.Fatalf("Error writing LUKS token temp file: %v\n", err)
	}
	tmpFile.Close()

	if err := exec.Command("cryptsetup", "token", "import", "--token-id", "1", devicePath, tmpFile.Name()).Run(); err != nil {
		cleanupMount()
		log.Fatalf("Error writing public key to LUKS header: %v\n", err)
	}

	fmt.Println("Encrypted disk initialized and mounted successfully")
}

func mountExistingDisk(passphrase string) {
	// Open the LUKS container
	cmd := exec.Command("cryptsetup", "open", devicePath, mapperName)
	cmd.Stdin = strings.NewReader(passphrase)
	if err := cmd.Run(); err != nil {
		log.Fatalf("Error opening LUKS device: %v\n", err)
	}

	// Mount the filesystem
	os.MkdirAll(mountPoint, 0755)
	if err := exec.Command("mount", mapperDevice, mountPoint).Run(); err != nil {
		exec.Command("cryptsetup", "close", mapperName).Run()
		log.Fatalf("Error mounting filesystem: %v\n", err)
	}

	fmt.Println("Encrypted disk mounted successfully")
}

func checkMounted() bool {
	data, err := os.ReadFile("/proc/mounts")
	if err != nil {
		return false
	}
	return strings.Contains(string(data), " "+mountPoint+" ")
}

func cleanupMount() {
	exec.Command("umount", mountPoint).Run()
	exec.Command("cryptsetup", "close", mapperName).Run()
}
