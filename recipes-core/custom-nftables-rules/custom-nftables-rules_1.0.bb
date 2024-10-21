DESCRIPTION = "Custom nftables firewall rules"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://nftables.conf \
            file://nftables-init"

do_install() {
    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/nftables.conf ${D}${sysconfdir}/nftables.conf

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/nftables-init ${D}${sysconfdir}/init.d/nftables
}

inherit update-rc.d

INITSCRIPT_NAME = "nftables"
INITSCRIPT_PARAMS = "defaults 40"
