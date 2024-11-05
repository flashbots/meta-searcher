# Generic SSH user setup include file
# Required variables:
# - SSH_USER: Username to create
# - SSH_USER_ID: Numeric user ID
# - SSH_USER_KEY: SSH public key for the user
# Optional variables:
# - SSH_USER_SHELL: Shell for the user (defaults to /bin/sh)
# - SSH_USER_HOME: Home directory (defaults to /home/${SSH_USER})

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit useradd

SRC_URI = "file://${BPN}.sh"

# Set defaults if not defined
SSH_USER ?= "${PN}"
SSH_USER_KEY ?= "${SEARCHER_SSH_KEY}"
SSH_USER_SHELL ?= "/bin/${PN}"
SSH_USER_HOME ?= "/home/${SSH_USER}"

USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "-m -d ${SSH_USER_HOME} -s ${SSH_USER_SHELL} -u ${SSH_USER_ID} ${SSH_USER}"

python () {
    # Check if SSH_USER_KEY is set in the environment or in local.conf
    ssh_key = d.getVar('SSH_USER_KEY')
    
    if ssh_key is None:
        # If not set, check the original environment
        origenv = d.getVar("BB_ORIGENV", False)
        if origenv:
            ssh_key = origenv.getVar('SSH_USER_KEY')
    
    if ssh_key:
        # If SSH_USER_KEY is set, keep its value
        d.setVar('SSH_USER_KEY', ssh_key)
    else:
        # If SSH_USER_KEY is not set, raise an error
        bb.fatal("SSH_USER_KEY must be set. Please provide an SSH public key.")
}

do_install () {
    install -d ${D}${SSH_USER_HOME}/.ssh
    echo "${SSH_USER_KEY}" > ${D}${SSH_USER_HOME}/.ssh/authorized_keys
    chmod 700 ${D}${SSH_USER_HOME}/.ssh
    chmod 600 ${D}${SSH_USER_HOME}/.ssh/authorized_keys
    chown -R ${SSH_USER_ID}:${SSH_USER_ID} ${D}${SSH_USER_HOME}

    # Set up .profile to source /etc/profile
    echo '. /etc/profile' > ${D}${SSH_USER_HOME}/.profile
    chmod 644 ${D}${SSH_USER_HOME}/.profile
    chown ${SSH_USER_ID}:${SSH_USER_ID} ${D}${SSH_USER_HOME}/.profile

    # set up wrapper script as user shell
    install -d ${D}/bin
    install -m 0555 ${WORKDIR}/${BPN}.sh ${D}${SSH_USER_SHELL}
}

FILES:${PN} = "${SSH_USER_HOME} \
               ${SSH_USER_HOME}/.ssh \
               ${SSH_USER_HOME}/.ssh/authorized_keys \
               ${SSH_USER_HOME}/.profile \
	       ${SSH_USER_SHELL}"
