SUMMARY = "Adds Searcher User and SSH Key"
DESCRIPTION = "Creates the searcher user and sets up SSH access with network isolation"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit useradd
inherit update-rc.d

USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "-m -d /home/searcher -s /bin/sh -u 1000 searcher"

# Init script configuration - runs after nftables (40)
INITSCRIPT_NAME = "searcher-net"
INITSCRIPT_PARAMS = "start 45 2 3 4 5 . stop 55 0 1 6 ."

SRC_URI = " \
    file://searcher-net-init \
    file://ns-enter.c \
"

RDEPENDS:${PN} = "iproute2"

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

CFLAGS += "-D_GNU_SOURCE"
do_compile() {
    ${CC} ${CFLAGS} ${WORKDIR}/ns-enter.c ${LDFLAGS} -o ${WORKDIR}/ns-enter
}

do_install () {
    # Set up SSH
    install -d ${D}/home/searcher/.ssh
    echo "${SEARCHER_SSH_KEY}" > ${D}/home/searcher/.ssh/authorized_keys
    chmod 700 ${D}/home/searcher/.ssh
    chmod 600 ${D}/home/searcher/.ssh/authorized_keys
    chown -R 1000:1000 ${D}/home/searcher

    # Install namespace binary
    install -d ${D}${bindir}
    install -m 4755 ${WORKDIR}/ns-enter ${D}${bindir}/

    # Install init script
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/searcher-net-init ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}

    # Set up .profile
    echo '. /etc/profile' > ${D}/home/searcher/.profile
    echo 'if [ "$(id -u)" = "1000" ]; then' >> ${D}/home/searcher/.profile
    echo '    exec /usr/bin/ns-enter' >> ${D}/home/searcher/.profile
    echo 'fi' >> ${D}/home/searcher/.profile
    chmod 644 ${D}/home/searcher/.profile
    chown 1000:1000 ${D}/home/searcher/.profile
}

FILES:${PN} = " \
    ${bindir}/ns-enter \
    ${sysconfdir}/init.d/${INITSCRIPT_NAME} \
    /home/searcher \
    /home/searcher/.ssh \
    /home/searcher/.ssh/authorized_keys \
    /home/searcher/.profile \
"
