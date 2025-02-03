SUMMARY = "Fluentbit and it's configuration"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
# fyi: https://docs.fluentbit.io/manual/installation/yocto-embedded-linux

inherit update-rc.d
inherit useradd

SRC_URI = " \
    file://fluentbit.conf \
    file://fluentbit-pod-init \
    file://delay.lua \
"

# User/Group creation parameters
USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM:${PN} = "-r fluentbit"
# USERADD_PARAM:${PN} = "-r -g fluentbit -s /sbin/nologin fluentbit"
USERADD_PARAM:${PN} = "-m -g fluentbit -g adm -d /home/fluentbit -s /sbin/nologin -u 1011 fluentbit"
# USERADD_PARAM:${PN} = "-m -g fluentbit -g adm -d /home/fluentbit -s /bin/bash -u 1010 fluentbit"

# Init script configuration
INITSCRIPT_PACKAGES = "${PN}"
INITSCRIPT_NAME:${PN} = "fluentbit-pod"
INITSCRIPT_PARAMS:${PN} = "defaults 99"

# Does it really depend on podman?
RDEPENDS:${PN} = "podman modutils-initscripts"

do_install() {
    # Install fluentbit configuration
    install -d ${D}/etc/fluentbit/config
    install -m 0644 ${WORKDIR}/fluentbit.conf ${D}/etc/fluentbit/config/
    install -m 0644 ${WORKDIR}/delay.lua ${D}/etc/fluentbit/config/

    # Install init scripts
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/fluentbit-pod-init ${D}${sysconfdir}/init.d/fluentbit-pod

    chown -R fluentbit:fluentbit ${D}/etc/fluentbit

    # Create logs directory with proper permissions
    install -d ${D}/delayed_logs
    chown fluentbit:adm ${D}/delayed_logs
}

FILES:${PN} = " \
    /home/fluentbit \
    /etc/fluentbit \
    /etc/fluentbit/config/fluentbit.conf \
    /etc/init.d/fluentbit-pod \
    /delayed_logs \
    /etc/fluentbit/config/delay.lua \
"
