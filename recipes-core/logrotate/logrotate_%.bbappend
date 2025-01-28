FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " file://fluentbit-logs.conf "

do_install:append() {
    # Ensure the logrotate.d directory exists in the target filesystem
    install -d ${D}${sysconfdir}/logrotate.d

    # Install fluentbit logs configuration
    install -m 0644 ${WORKDIR}/fluentbit-logs.conf ${D}${sysconfdir}/logrotate.d/fluentbit-logs.conf
}