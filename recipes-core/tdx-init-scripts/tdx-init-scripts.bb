SUMMARY = "TDX Initialization Scripts"
DESCRIPTION = "SysVinit scripts for TDX key setup and disk encryption"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://wait-for-key \
    file://disk-encryption \
"

inherit update-rc.d

INITSCRIPT_PACKAGES = "tdx-wait-for-key tdx-disk-encryption"
INITSCRIPT_NAME:tdx-wait-for-key = "wait-for-key"
INITSCRIPT_PARAMS:tdx-wait-for-key = "defaults 59"
INITSCRIPT_NAME:tdx-disk-encryption = "disk-encryption"
INITSCRIPT_PARAMS:tdx-disk-encryption = "defaults 88"

RDEPENDS:tdx-wait-for-key = "tdx-init"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/wait-for-key ${D}${sysconfdir}/init.d/
    install -m 0755 ${WORKDIR}/disk-encryption ${D}${sysconfdir}/init.d/
}

FILES:tdx-wait-for-key = "${sysconfdir}/init.d/wait-for-key"
FILES:tdx-disk-encryption = "${sysconfdir}/init.d/disk-encryption"