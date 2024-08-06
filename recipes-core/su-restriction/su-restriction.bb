SUMMARY = "Implement su and root access restrictions"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://restrict-su.sh"

inherit update-rc.d

INITSCRIPT_NAME = "restrict-su"
INITSCRIPT_PARAMS = "defaults 99"

do_install:append() {
    # Ensure init.d directory exists
    install -d ${D}${sysconfdir}/init.d

    install -m 0755 ${WORKDIR}/restrict-su.sh ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}
}

FILES:${PN} = "${sysconfdir}/init.d/${INITSCRIPT_NAME}"
