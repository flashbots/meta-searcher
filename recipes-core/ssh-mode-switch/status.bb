SUMMARY = "Adds status mode user and script"
DESCRIPTION = "Creates the status mode user and sets up SSH access with the provided SSH key, limit's the user to execute the status.sh wrapper script"

require ssh-mode-switch.inc

SSH_USER_ID = "1102"
RDEPENDS:${PN} += "curl"
