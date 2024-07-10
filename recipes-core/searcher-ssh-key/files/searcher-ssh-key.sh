#!/bin/sh
### BEGIN INIT INFO
# Provides:          searcher-ssh-key
# Required-Start:    $all
# Required-Stop:     
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6   
# Short-Description: Adds searcher ssh key to authorized_keys
### END INIT INFO

case "$1" in
  start)
    echo "Starting searcher-ssh-key script"

    # Create .ssh directory and set permission
    mkdir -p /home/root/.ssh
    chmod 700 /home/root/.ssh

    # Add searcher's ssh key to authorized_keys file
    SSH_KEY="ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCtZXRyJDNyFkDiqL5AeFL8L6RHsYCsA1zUeaFuGD5Spi9oRNc0mWefPTjprmDPsA3+oKqzIWwqdcM5/JqYocCsLm/5H28YczH6K89kV7aUUGg0VUvIyFT4NINYRPKja+uYdNqXqhLyX+sm/ClddyiqXX3MfU+aIFFgC3oi7m0ByEf7/6Qxe25GgAXJXhb9jatyeogynHrDEeXm1d+obLN3KZ5XPp9Me69VsZjKuFof8vKuOTGldfdoMtxabEzQUUpoHE1bz5qWbRy4y3XayHHoIeeeG7YeeLmcEXYjnXeARjbBzsiAxpUge1gz/fuV3ataxcSHP25uX3tdEvT1rbSn princess"
    echo $SSH_KEY >> /home/root/.ssh/authorized_keys
    chmod 600 /home/root/.ssh/authorized_keys
    echo "Added Searcher SSH key: $SSH_KEY"

    # TEMP: Setting up local networking for runqemu with static IP: 192.168.7.2
    echo "TEMP: Setting up local networking for runqemu with static IP: 192.168.7.2"
    echo "nameserver 8.8.8.8" > /etc/resolv.conf
    cat <<EOF > /etc/network/interfaces
# /etc/network/interfaces -- configuration file for ifup(8), ifdown(8)
 
# The loopback interface
auto lo
iface lo inet loopback

# Wired or wireless interfaces including predictable names
auto eth0
iface eth0 inet static
	address 192.168.7.2
	netmask 255.255.255.0
	network 192.168.7.0
	gateway 192.168.7.1
EOF
    /etc/init.d/networking restart
    /etc/init.d/dropbear restart
    ;;
  stop)
    echo "Stopping searcher-ssh-key script"
    ;;
  *)
    echo "Usage: $0 {start|stop}"
    exit 1
    ;;
esac

exit 0