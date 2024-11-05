#!/bin/sh

echo "Curling go-bob-firewall status..."
curl http://go-bob-firewall:80/firewall/status
