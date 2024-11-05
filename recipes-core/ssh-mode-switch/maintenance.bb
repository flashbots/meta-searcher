SUMMARY = "Adds maintenance mode user and script"
DESCRIPTION = "Creates the maintenance mode user and sets up SSH access with the provided SSH key, limit's the user to execute the maintenance.sh wrapper script"

require ssh-mode-switch.inc

SSH_USER_ID = "1101"
RDEPENDS:${PN} += "curl"
