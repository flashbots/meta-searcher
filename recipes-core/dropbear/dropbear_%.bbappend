FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append = " file://dropbear.default"

do_install:append() {
    # override default poky dropbear configurations with local dropbear.default file
    install -m 0644 ${WORKDIR}/dropbear.default ${D}${sysconfdir}/default/dropbear
}