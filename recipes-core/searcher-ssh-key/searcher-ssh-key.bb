SUMMARY = "Adds Searcher SSH Key for Searcher User"
DESCRIPTION = "An initialization script that sets up SSH access for the searcher user with only the searcher's SSH key and ensures the necessary directory permissions for dropbear."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://searcher-ssh-key.sh"

inherit update-rc.d

INITSCRIPT_NAME = "searcher-ssh-key"
# "defaults" means it will use the default start and stop levels specified in searcher-ssh-key.sh
# "99" indicates it should start towards the end of the boot process
INITSCRIPT_PARAMS = "defaults 99"

do_install() {
    # Ensure init.d directory exists
    install -d ${D}${sysconfdir}/init.d

    # Install the searcher-ssh-key script in init.d
    install -m 0755 ${WORKDIR}/searcher-ssh-key.sh ${D}${sysconfdir}/init.d/searcher-ssh-key
}

FILES_${PN} = "${sysconfdir}/init.d/searcher-ssh-key"