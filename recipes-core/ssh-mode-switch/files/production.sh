#!/bin/sh

echo "Curling go-bob-firewall production..."
curl http://go-bob-firewall:80/firewall/production
