SUMMARY = "TDX Initialization Scripts"
DESCRIPTION = "SysVinit scripts for TDX key setup and disk encryption"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://wait-for-key \
    file://disk-encryption \
"

inherit update-rc.d

# Init script configuration
INITSCRIPT_PACKAGES = "${PN}"
INITSCRIPT_NAME_${PN} = "wait-for-key"
INITSCRIPT_PARAMS_${PN} = "defaults 59"
INITSCRIPT_NAME_${PN}-disk-encryption = "disk-encryption"
INITSCRIPT_PARAMS_${PN}-disk-encryption = "defaults 88"

RDEPENDS:${PN} = "tdx-init"

do_install() {
    # Install init scripts
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/wait-for-key ${D}${sysconfdir}/init.d/
    install -m 0755 ${WORKDIR}/disk-encryption ${D}${sysconfdir}/init.d/
}

FILES:${PN} = " \
    ${sysconfdir}/init.d/wait-for-key \
    ${sysconfdir}/init.d/disk-encryption \
"