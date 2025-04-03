SUMMARY = "TDX SSH Key Setup Script"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://wait-for-key"

inherit update-rc.d

INITSCRIPT_NAME = "wait-for-key"
INITSCRIPT_PARAMS = "defaults 59"

RDEPENDS:${PN} = "tdx-init"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/wait-for-key ${D}${sysconfdir}/init.d/
}

FILES:${PN} = "${sysconfdir}/init.d/wait-for-key"