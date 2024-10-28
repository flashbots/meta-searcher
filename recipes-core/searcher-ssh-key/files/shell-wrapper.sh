#!/bin/sh

# Set proper environment
export USER=searcher
export LOGNAME=searcher
export HOME=/home/searcher
export SHELL=/bin/sh
export PS1='searcher@tdx:\w\$ '

# Check current mode and handle transition if needed
mode=$(mode-controller status | awk '{print $NF}')
if [ "$mode" = "production" ]; then
    echo "System is in production mode. Initiating maintenance transition..."
    # Execute transition synchronously to ensure state change
    /usr/bin/mode-controller transition-to-maintenance
    echo "Please wait 5 minutes before reconnecting."
    exit 1
fi

# Execute ns-enter with proper environment
exec env PS1='searcher@tdx:\w\$ ' /usr/bin/ns-enter
