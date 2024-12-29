SUMMARY = "Fluentbit and it's configuration"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
# fyi: https://docs.fluentbit.io/manual/installation/yocto-embedded-linux

inherit useradd

SRC_URI = " \
    file://fluentbit-pod.yaml \
    file://fluentbit.conf \
    file://fluentbit-pod-init \
"

# User/Group creation parameters
USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM:${PN} = "-r fluentbit"
USERADD_PARAM:${PN} = "-r -g fluentbit -s /sbin/nologin fluentbit"

# Init script configuration
INITSCRIPT_NAME:${PN} = "fluentbit-pod"
INITSCRIPT_PARAMS:${PN} = "defaults 99"

RDEPENDS:${PN} = "podman modutils-initscripts"

do_install() {
    # Install pod configuration
    install -d ${D}/etc/fluentbit/pod-config
    install -d ${D}/etc/fluentbit/fluentbitd-config
    install -m 0644 ${WORKDIR}/fluentbit-pod.yaml ${D}/etc/fluentbit/pod-config/
    install -m 0644 ${WORKDIR}/fluentbit.conf ${D}/etc/fluentbit/fluentbitd-config/

    # Install init scripts
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/fluentbit-pod-init ${D}${sysconfdir}/init.d/fluentbit-pod

    chown -R fluentbit:fluentbit ${D}/etc/fluentbit
}

FILES:${PN} = " \
    /etc/fluentbit \
    /etc/fluentbit/pod-config/fluentbit-pod.yaml \
    /etc/fluentbit/pod-config/pod-config/fluentbit-pod.yaml \
    /etc/fluentbit/fluentbitd-config/fluentbit.conf \
    /etc/init.d/fluentbit-pod \
"
