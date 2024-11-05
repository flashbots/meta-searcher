SUMMARY = "Adds failsafe mode user and script"
DESCRIPTION = "Creates the failsafe mode user and sets up SSH access with the provided SSH key, limit's the user to execute the failsafe.sh wrapper script"

require ssh-mode-switch.inc

SSH_USER_ID = "1103"
RDEPENDS:${PN} += "curl"
