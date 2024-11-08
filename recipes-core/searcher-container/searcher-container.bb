SUMMARY = "CNI Network Configurations"
DESCRIPTION = "Sets up CNI network configurations for maintenance and production networks"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit update-rc.d
inherit useradd

SRC_URI = " \
    file://maintenance.conflist \
    file://production.conflist \
    file://searcher-ssh-pod.yaml \
    file://searcher-pod-init \
    file://searcher-network-init \
"

# Create separate package for network setup
PACKAGES =+ "${PN}-network"

# Init script configuration
INITSCRIPT_PACKAGES = "${PN} ${PN}-network"
INITSCRIPT_NAME:${PN} = "searcher-pod"
INITSCRIPT_PARAMS:${PN} = "defaults 99"
INITSCRIPT_NAME:${PN}-network = "searcher-network"
INITSCRIPT_PARAMS:${PN}-network = "defaults 98"

RDEPENDS:${PN} = "podman netavark catatonit modutils-initscripts iproute2 bridge-utils kernel-modules"
RDEPENDS:${PN}-network = "iptables"

# User/Group creation parameters
USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "-m -d /home/searcher -s /sbin/nologin -u 1000 searcher"

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

do_install() {
    # Install pod configuration
    install -d ${D}/home/searcher/pod-config
    install -m 0644 ${WORKDIR}/searcher-ssh-pod.yaml ${D}/home/searcher/pod-config/

    # Install init scripts
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/searcher-pod-init ${D}${sysconfdir}/init.d/searcher-pod
    install -m 0755 ${WORKDIR}/searcher-network-init ${D}${sysconfdir}/init.d/searcher-network

    # Install CNI network configurations for searcher user
    install -d ${D}/home/searcher/.config/cni/net.d
    install -m 0644 ${WORKDIR}/maintenance.conflist ${D}/home/searcher/.config/cni/net.d/maintenance.conflist
    install -m 0644 ${WORKDIR}/production.conflist ${D}/home/searcher/.config/cni/net.d/production.conflist

    # Ensure proper ownership
    chown -R searcher:searcher ${D}/home/searcher

    # Create persistent directory
    install -d ${D}/persistent

    # Add searcher ssh key
    echo "${SEARCHER_SSH_KEY}" > ${D}/etc/searcher_key
}

FILES:${PN} = " \
    /home/searcher/.config/cni/net.d/* \
    /home/searcher/pod-config/searcher-ssh-pod.yaml \
    /etc/init.d/searcher-pod \
    /persistent \
    /etc/searcher_key \
"

FILES:${PN}-network = " \
    /etc/init.d/searcher-network \
"
