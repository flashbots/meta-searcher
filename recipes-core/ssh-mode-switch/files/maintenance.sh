#!/bin/sh

echo "Curling go-bob-firewall maintenance..."
curl http://go-bob-firewall:80/firewall/maintenance
