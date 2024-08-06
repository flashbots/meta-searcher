SUMMARY = "Adds Searcher SSH Key for Searcher User"
DESCRIPTION = "An initialization script that sets up SSH access for the searcher user with only the searcher's SSH key and ensures the necessary directory permissions for dropbear."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://searcher-ssh-key.sh.in"

inherit update-rc.d

INITSCRIPT_NAME = "searcher-ssh-key"
INITSCRIPT_PARAMS = "defaults 98"

python () {
    origenv = d.getVar("BB_ORIGENV", False)
    ssh_key = origenv.getVar('SEARCHER_SSH_KEY') or origenv.getenv('SEARCHER_SSH_KEY')
    if not ssh_key:
        bb.fatal("SEARCHER_SSH_KEY must be set. Please provide an SSH public key.")
    d.setVar('SEARCHER_SSH_KEY', ssh_key)
}

do_install() {
    # Ensure init.d directory exists
    install -d ${D}${sysconfdir}/init.d
    
    # Use sed to replace the SSH_KEY placeholder in the script
    sed -e 's|@SEARCHER_SSH_KEY@|${SEARCHER_SSH_KEY}|g' \
        ${WORKDIR}/searcher-ssh-key.sh.in > ${WORKDIR}/searcher-ssh-key.sh
    
    install -m 0755 ${WORKDIR}/searcher-ssh-key.sh ${D}${sysconfdir}/init.d/searcher-ssh-key
}

DEPENDS += "cvm-reverse-proxy"
RDEPENDS:${PN} = "cvm-reverse-proxy"

FILES:${PN} = "${sysconfdir}/init.d/searcher-ssh-key"
