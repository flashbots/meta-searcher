SUMMARY = "Seacher and Searcher Network Configurations"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit update-rc.d
inherit useradd

SRC_URI = " \
    file://searcher-pod-init \
    file://searcher-network-init \
    file://toggle \
    file://searchersh.c \
    file://99-searcher \
"

# Create separate package for network setup
PACKAGES =+ "${PN}-network"

# Init script configuration
INITSCRIPT_PACKAGES = "${PN} ${PN}-network"
INITSCRIPT_NAME:${PN} = "searcher-pod"
INITSCRIPT_PARAMS:${PN} = "defaults 98"
INITSCRIPT_NAME:${PN}-network = "searcher-network"
INITSCRIPT_PARAMS:${PN}-network = "defaults 99"

RDEPENDS:${PN} = "podman catatonit modutils-initscripts kernel-modules"
RDEPENDS:${PN}-network = "iptables netavark socat sudo"

# User/Group creation parameters
USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "-m -d /home/searcher -s /usr/bin/searchersh -u 1000 searcher"

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

do_compile() {
    ${CC} ${WORKDIR}/searchersh.c ${LDFLAGS} -o ${WORKDIR}/searchersh
}

do_install() {

    # Install init scripts
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/searcher-pod-init ${D}${sysconfdir}/init.d/searcher-pod
    install -m 0755 ${WORKDIR}/searcher-network-init ${D}${sysconfdir}/init.d/searcher-network

    # Install searcher shell and toggle script
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/searchersh ${D}${bindir}
    install -m 0755 ${WORKDIR}/toggle ${D}${bindir}

    # Install sudoers configuration
    install -d ${D}${sysconfdir}/sudoers.d
    install -m 0440 ${WORKDIR}/99-searcher ${D}${sysconfdir}/sudoers.d/

    # Create persistent directory
    install -d ${D}/persistent

    # Create logs directory
    install -d ${D}/searcher_logs
    # TODO: add noexec and other flags to mount

    # Add searcher ssh key
    echo "${SEARCHER_SSH_KEY}" > ${D}/etc/searcher_key

    install -d ${D}/home/searcher/.ssh
    echo "${SEARCHER_SSH_KEY}" > ${D}/home/searcher/.ssh/authorized_keys
    chmod 700 ${D}/home/searcher/.ssh
    chmod 600 ${D}/home/searcher/.ssh/authorized_keys
    chown -R 1000:1000 ${D}/home/searcher
}

pkg_postinst:${PN} () {
        grep -q "^/usr/bin/searchersh$" $D${sysconfdir}/shells || echo /usr/bin/searchersh >> $D${sysconfdir}/shells
}

pkg_postrm:${PN} () {
        printf "$(grep -v "^/usr/bin/searchersh$" $D${sysconfdir}/shells)\n" > $D${sysconfdir}/shells
}

FILES:${PN} = " \
    /home/searcher \
    /home/searcher/.config/cni/net.d/* \
    /etc/init.d/searcher-pod \
    /persistent \
    /etc/searcher_key \
    /home/searcher/.ssh \
    /home/searcher/.ssh/authorized_keys \
    /searcher_logs \
"

FILES:${PN}-network = " \
    /etc/init.d/searcher-network \
    /usr/bin/searchersh \
    /usr/bin/toggle \
    /etc/sudoers.d/99-searcher \
"
