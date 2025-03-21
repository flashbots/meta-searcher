FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

do_install:append() {
    # Busybox-based init typically calls: sysctl -p /etc/sysctl.conf
    # So simply append our setting to the end of sysctl.conf
    echo 'vm.max_map_count=2097152' >> ${D}${sysconfdir}/sysctl.conf
}