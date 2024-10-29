# searcher-ssh-key.bb
SUMMARY = "Adds Searcher User and SSH Key"
DESCRIPTION = "Creates the searcher user and sets up SSH access with network isolation and mode management"
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
    file://mode-controller.c \
    file://shell-wrapper.sh \
"

RDEPENDS:${PN} = "iproute2"

python () {
    searcher_ssh_key = d.getVar('SEARCHER_SSH_KEY')
    
    if searcher_ssh_key is None:
        origenv = d.getVar("BB_ORIGENV", False)
        if origenv:
            searcher_ssh_key = origenv.getVar('SEARCHER_SSH_KEY')
    
    if searcher_ssh_key:
        d.setVar('SEARCHER_SSH_KEY', searcher_ssh_key)
    else:
        bb.fatal("SEARCHER_SSH_KEY must be set. Please provide an SSH public key.")
}

CFLAGS += "-D_GNU_SOURCE"
do_compile() {
    ${CC} ${CFLAGS} ${WORKDIR}/ns-enter.c ${LDFLAGS} -o ${WORKDIR}/ns-enter
    ${CC} ${CFLAGS} ${WORKDIR}/mode-controller.c ${LDFLAGS} -o ${WORKDIR}/mode-controller
}

do_install () {
    # Set up SSH with explicit ownership and permissions
    install -d -m 0755 ${D}/home/searcher
    install -d -m 0700 ${D}/home/searcher/.ssh
    echo "${SEARCHER_SSH_KEY}" > ${D}/home/searcher/.ssh/authorized_keys
    chmod 600 ${D}/home/searcher/.ssh/authorized_keys
    chown -R 1000:1000 ${D}/home/searcher

    # Install binaries
    install -d ${D}${bindir}
    install -m 4755 ${WORKDIR}/ns-enter ${D}${bindir}/
    install -m 4755 ${WORKDIR}/mode-controller ${D}${bindir}/
    install -m 0755 ${WORKDIR}/shell-wrapper.sh ${D}${bindir}/shell-wrapper

    # Install init script
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/searcher-net-init ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}    
}


pkg_postinst_${PN}() {
    #!/bin/sh
    # Create state file on first boot
    if [ ! -f /run/system-mode.state ]; then
        echo "maintenance" > /run/system-mode.state
        chmod 644 /run/system-mode.state
    fi
}

FILES:${PN} += " \
    ${bindir}/ns-enter \
    ${bindir}/mode-controller \
    ${bindir}/shell-wrapper \
    ${sysconfdir}/init.d/${INITSCRIPT_NAME} \
    ${sysconfdir}/dropbear/config \
    /home/searcher \
    /home/searcher/.ssh \
    /home/searcher/.ssh/authorized_keys \
    /run/system-mode.state \
"
