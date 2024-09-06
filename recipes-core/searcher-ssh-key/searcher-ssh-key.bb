SUMMARY = "Adds Searcher User and SSH Key"
DESCRIPTION = "Creates the searcher user and sets up SSH access with the provided SSH key"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit useradd

USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "-m -d /home/searcher -s /bin/sh -u 1000 searcher"

python () {
    # Check if SEARCHER_SSH_KEY is set in the environment or in local.conf
    searcher_ssh_key = d.getVar('SEARCHER_SSH_KEY')
    
    if searcher_ssh_key is None:
        # If not set, check the original environment
        origenv = d.getVar("BB_ORIGENV", False)
        if origenv:
            searcher_ssh_key = origenv.getVar('SEARCHER_SSH_KEY')
    
    if searcher_ssh_key:
        # If SEARCHER_SSH_KEY is set, keep its value
        d.setVar('SEARCHER_SSH_KEY', searcher_ssh_key)
    else:
        # If SEARCHER_SSH_KEY is not set, raise an error
        bb.fatal("SEARCHER_SSH_KEY must be set. Please provide an SSH public key.")
}

do_install () {
    install -d ${D}/home/searcher/.ssh
    echo "${SEARCHER_SSH_KEY}" > ${D}/home/searcher/.ssh/authorized_keys
    chmod 700 ${D}/home/searcher/.ssh
    chmod 600 ${D}/home/searcher/.ssh/authorized_keys
    chown -R 1000:1000 ${D}/home/searcher

    # Set up .profile to source /etc/profile
    echo '. /etc/profile' > ${D}/home/searcher/.profile
    chmod 644 ${D}/home/searcher/.profile
    chown 1000:1000 ${D}/home/searcher/.profile
}

FILES:${PN} = "/home/searcher /home/searcher/.ssh /home/searcher/.ssh/authorized_keys"

