package main

import (
	"crypto/sha384"
	"encoding/base64"
	"encoding/hex"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"path/filepath"

	"github.com/google/go-tdx-guest/client"
	"github.com/google/logger"
)

const (
	sshKeyPath   = "/etc/searcher_key"
	authKeysPath = "/home/searcher/.ssh/authorized_keys"
	luksDevice   = "/dev/vda3"
	luksName     = "cryptpersistent"
	mountPoint   = "/persistent"
)

func main() {
	printKey := flag.Bool("print-ssh-key", false, "Print the SSH key from MR_OWNER")
	passphrase := flag.String("passphrase", "", "Passphrase for LUKS encryption")
	verbose := flag.Bool("verbose", false, "Enable verbose logging")
	flag.Parse()

	logger.Init("tdx-init", *verbose, false, os.Stdout)
	defer logger.Close()

	// Get TDX measurements
	mrConfigID, mrOwner, err := getTDXMeasurements()
	if err != nil {
		log.Fatalf("TDX measurement error: %v", err)
	}

	// Extract SSH key from MR_OWNER
	sshKey := fmt.Sprintf("ssh-ed25519 %s searcher@tdx", base64.StdEncoding.EncodeToString(mrOwner))

	// If only printing the SSH key
	if *printKey {
		fmt.Println(sshKey)
		return
	}

	// For initialization, verify passphrase and set up LUKS
	if *passphrase == "" {
		log.Fatalf("Missing passphrase (use --passphrase)")
	}

	// Verify passphrase against MR_CONFIG_ID
	if !verifyPassphrase(*passphrase, mrConfigID) {
		log.Fatalf("Passphrase verification failed - hash doesn't match MR_CONFIG_ID")
	}

	// Initialize LUKS and mount filesystem
	if err := setupEncryption(*passphrase); err != nil {
		log.Fatalf("Encryption setup failed: %v", err)
	}

	// Save SSH key
	if err := ioutil.WriteFile(sshKeyPath, []byte(sshKey), 0644); err != nil {
		log.Fatalf("Failed to write SSH key: %v", err)
	}
	if err := ioutil.WriteFile(authKeysPath, []byte(sshKey), 0600); err != nil {
		log.Fatalf("Failed to write authorized_keys: %v", err)
	}
	exec.Command("chown", "searcher:searcher", authKeysPath).Run()

	fmt.Println("Initialization completed successfully")
}

// Get required TDX measurements (MR_CONFIG_ID and MR_OWNER)
func getTDXMeasurements() ([]byte, []byte, error) {
	var reportData [64]byte

	quoteProvider, err := client.GetQuoteProvider()
	if err != nil {
		return nil, nil, err
	}

	quote, err := client.GetQuote(quoteProvider, reportData)
	if err != nil {
		return nil, nil, err
	}

	quoteV4, ok := quote.(*client.QuoteV4)
	if !ok {
		return nil, nil, fmt.Errorf("quote is not a QuoteV4")
	}

	return quoteV4.GetTdQuoteBody().GetMrConfigId(),
		quoteV4.GetTdQuoteBody().GetMrOwner(),
		nil
}

// Verify the passphrase against MR_CONFIG_ID
func verifyPassphrase(passphrase string, mrConfigID []byte) bool {
	hasher := sha384.New()
	hasher.Write([]byte(passphrase))
	return hex.EncodeToString(hasher.Sum(nil)) == hex.EncodeToString(mrConfigID)
}

// Setup LUKS encryption and mount the filesystem
func setupEncryption(passphrase string) error {
	// Check if LUKS container exists
	isLuks := exec.Command("cryptsetup", "isLuks", luksDevice).Run() == nil

	// Prepare LUKS device
	if isLuks {
		// Open existing LUKS device
		cmd := exec.Command("cryptsetup", "open", "--type", "luks", luksDevice, luksName)
		stdin, _ := cmd.StdinPipe()
		cmd.Start()
		stdin.Write([]byte(passphrase + "\n"))
		stdin.Close()
		if err := cmd.Wait(); err != nil {
			return fmt.Errorf("failed to open LUKS device: %v", err)
		}
	} else {
		// Create new LUKS device
		cmd := exec.Command("cryptsetup", "luksFormat", "--type", "luks2", "-q", luksDevice)
		stdin, _ := cmd.StdinPipe()
		cmd.Start()
		stdin.Write([]byte(passphrase + "\n"))
		stdin.Close()
		if err := cmd.Wait(); err != nil {
			return fmt.Errorf("failed to format LUKS device: %v", err)
		}

		// Open the new device
		cmd = exec.Command("cryptsetup", "open", "--type", "luks", luksDevice, luksName)
		stdin, _ = cmd.StdinPipe()
		cmd.Start()
		stdin.Write([]byte(passphrase + "\n"))
		stdin.Close()
		if err := cmd.Wait(); err != nil {
			return fmt.Errorf("failed to open LUKS device: %v", err)
		}

		// Create filesystem
		if err := exec.Command("mkfs.ext4", "/dev/mapper/"+luksName).Run(); err != nil {
			return fmt.Errorf("failed to create filesystem: %v", err)
		}
	}

	// Mount the filesystem
	os.MkdirAll(mountPoint, 0755)
	if err := exec.Command("mount", "/dev/mapper/"+luksName, mountPoint).Run(); err != nil {
		return fmt.Errorf("failed to mount filesystem: %v", err)
	}

	// Create required directories
	for _, dir := range []string{"searcher", "searcher_logs", "delayed_logs"} {
		os.MkdirAll(filepath.Join(mountPoint, dir), 0755)
	}

	return nil
}
