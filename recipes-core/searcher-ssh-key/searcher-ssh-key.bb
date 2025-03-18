SUMMARY = "Searcher SSH Key Setup"
DESCRIPTION = "Sets up the searcher's SSH key from TDX measurements"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit update-rc.d

SRC_URI = "file://setup-ssh-key.sh"

INITSCRIPT_NAME = "setup-ssh-key"
INITSCRIPT_PARAMS = "defaults 97"

RDEPENDS:${PN} = "tdx-init"

do_install() {
    # Install init script
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/setup-ssh-key.sh ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}
}

FILES:${PN} = "${sysconfdir}/init.d/${INITSCRIPT_NAME}"