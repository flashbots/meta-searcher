#!/bin/sh
### BEGIN INIT INFO
# Provides:          restrict-su
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Implement su and root access restrictions
### END INIT INFO

case "$1" in
  start)
    echo "Implementing su and root access restrictions"
    
    # Remove execute permissions from su for non-root users
    chmod 700 /bin/su
    
    # Ensure only root owns su
    chown root:root /bin/su

    # Lock the root account
    passwd -l root

    echo "Su and root access restrictions implemented"
    ;;
  stop)
    echo "Su and root access restrictions cannot be undone automatically"
    ;;
  *)
    echo "Usage: $0 {start|stop}"
    exit 1
    ;;
esac

exit 0
