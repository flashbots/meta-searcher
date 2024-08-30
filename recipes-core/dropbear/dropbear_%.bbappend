FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append = " file://dropbear.default"

RDEPENDS:${PN} += "openssh-sftp-server"

do_install:append() {
    install -d ${D}${sysconfdir}/default
    # override default poky dropbear configurations with local dropbear.default file
    install -m 0644 ${WORKDIR}/dropbear.default ${D}${sysconfdir}/default/dropbear
}

FILES:${PN} += "${sysconfdir}/default/dropbear"
