SUMMARY = "TDX Disk Encryption Setup Script"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://disk-encryption"

inherit update-rc.d

INITSCRIPT_NAME = "disk-encryption"
INITSCRIPT_PARAMS = "defaults 88"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/disk-encryption ${D}${sysconfdir}/init.d/
}

FILES:${PN} = "${sysconfdir}/init.d/disk-encryption"