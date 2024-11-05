SUMMARY = "Adds production mode user and script"
DESCRIPTION = "Creates the production mode user and sets up SSH access with the provided SSH key, limit's the user to execute the production-mode.sh wrapper script"

require ssh-mode-switch.inc

SSH_USER_ID = "1100"
RDEPENDS:${PN} += "curl"
