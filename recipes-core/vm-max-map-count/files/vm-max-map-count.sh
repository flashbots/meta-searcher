#!/bin/sh
### BEGIN INIT INFO
# Provides:          vm-max-map-count
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Set vm.max_map_count on startup
### END INIT INFO

case "$1" in
  start)
    echo "Setting vm.max_map_count to 2097152"
    sysctl -w vm.max_map_count=2097152
    ;;
  stop)
    echo "No action taken on stop."
    ;;
  restart|reload|force-reload)
    echo "Restarting setting of vm.max_map_count"
    sysctl -w vm.max_map_count=2097152
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|reload|force-reload}"
    exit 1
    ;;
esac

exit 0