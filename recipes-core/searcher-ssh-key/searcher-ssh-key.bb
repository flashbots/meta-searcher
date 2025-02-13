SUMMARY = "Adds Searcher User and SSH Key"
DESCRIPTION = "Creates the searcher user and sets up SSH access with the provided SSH key"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit useradd

USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "-m -d /home/searcher -s /bin/sh -u 1000 searcher"

export SEARCHER_SSH_KEY := "${SEARCHER_SSH_KEY}"
do_install[vardeps] += "SEARCHER_SSH_KEY"

do_install () {
    if [ -z "${SEARCHER_SSH_KEY}" ]; then
        echo "ERROR: SEARCHER_SSH_KEY is not set"
        return 1
    fi

    install -d ${D}/home/searcher/.ssh
    echo "${SEARCHER_SSH_KEY}" > ${D}/home/searcher/.ssh/authorized_keys
    chmod 700 ${D}/home/searcher/.ssh
    chmod 600 ${D}/home/searcher/.ssh/authorized_keys
    chown -R 1000:1000 ${D}/home/searcher

    # Set up .profile to source /etc/profile
    echo '. /etc/profile' > ${D}/home/searcher/.profile
    chmod 644 ${D}/home/searcher/.profile
    chown 1000:1000 ${D}/home/searcher/.profile

    # give root privileges too
    install -d ${D}/root/.ssh
    echo "${SEARCHER_SSH_KEY}" > ${D}/root/.ssh/authorized_keys
    chmod 700 ${D}/root/.ssh
    chmod 600 ${D}/root/.ssh/authorized_keys
}

FILES:${PN} = "\
    /home/searcher \
    /home/searcher/.ssh \
    /home/searcher/.ssh/authorized_keys \
    /root/.ssh \
    /root/.ssh/authorized_keys \
"