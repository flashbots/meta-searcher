package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
)

func waitForKey() {
	// Check if LUKS container exists
	cmd := exec.Command("cryptsetup", "isLuks", devicePath)
	if cmd.Run() == nil {
		// LUKS exists, extract key from header
		log.Println("Found existing LUKS container, extracting key...")
		cmd := exec.Command("cryptsetup", "token", "export", "--token-id", "1", devicePath)
		output, err := cmd.Output()
		if err != nil {
			log.Fatalf("Error exporting LUKS token: %v", err)
		}

		var token Token
		if err := json.Unmarshal(output, &token); err != nil {
			log.Fatalf("Error parsing token JSON: %v", err)
		}

		keyData, ok := token.UserData["metadata"]
		if !ok {
			log.Fatalln("Error: No metadata found in token")
		}

		// Write the key
		writeKey(string(keyData))
		log.Printf("Key extracted from LUKS header and written to %s", keyFile)
		return
	}

	// No LUKS container, start HTTP server to wait for key
	log.Printf("No LUKS container found, starting HTTP server on port %s...", httpPort)
	log.Println("Waiting for key to be provided via HTTP...")

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			fmt.Fprint(w, "Only POST method is allowed")
			return
		}

		body, err := io.ReadAll(r.Body)
		if err != nil {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprintf(w, "Error reading request: %v", err)
			return
		}

		key := string(body)
		matched, _ := regexp.MatchString(`^[A-Za-z0-9+/]{68}$`, key)
		if !matched {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprint(w, "Invalid key format, expected base64-encoded OpenSSH ed25519 public key")
			return
		}

		writeKey(key)
		w.WriteHeader(http.StatusOK)
		fmt.Fprint(w, "Key received and stored successfully")
		os.Exit(0) // Exit after successfully receiving the key
	})

	log.Fatal(http.ListenAndServe(":"+httpPort, nil))
}

func writeKey(key string) {
	// Write authorized_keys
	os.MkdirAll(sshDir, 0700)

	authKeysFile := filepath.Join(sshDir, "authorized_keys")
	f, err := os.OpenFile(authKeysFile, os.O_CREATE|os.O_WRONLY, 0600)
	if err != nil {
		log.Fatalf("Error opening authorized_keys: %v", err)
	}
	defer f.Close()

	if _, err := f.WriteString("ssh-ed25519 " + key + "\n"); err != nil {
		log.Fatalf("Error writing to authorized_keys: %v", err)
	}

	// Write to separate key file
	if err := os.WriteFile(keyFile, []byte(key), 0600); err != nil {
		log.Fatalf("Error writing key file: %v", err)
	}
}
