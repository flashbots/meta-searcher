#!/bin/sh
### BEGIN INIT INFO
# Provides:          setup-ssh-key
# Required-Start:    $network $remote_fs
# Required-Stop:     $network $remote_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Set up SSH key from TDX measurements
### END INIT INFO

TDXINIT=/usr/bin/tdx-init

case "$1" in
  start)
    echo "Setting up SSH key from TDX measurements"
    
    # Extract the SSH key from MR_OWNER and save it
    $TDXINIT --print-ssh-key > /etc/searcher_key
    
    # Copy to authorized_keys
    if [ -f /etc/searcher_key ]; then
      mkdir -p /home/searcher/.ssh
      cp /etc/searcher_key /home/searcher/.ssh/authorized_keys
      chown -R searcher:searcher /home/searcher/.ssh
      chmod 700 /home/searcher/.ssh
      chmod 600 /home/searcher/.ssh/authorized_keys
      echo "SSH key set up successfully"
    else
      echo "Failed to extract SSH key"
      exit 1
    fi
    ;;
  stop)
    echo "Nothing to do for stop"
    ;;
  *)
    echo "Usage: $0 {start|stop}"
    exit 1
    ;;
esac

exit 0