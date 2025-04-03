SUMMARY = "TDX Initialization Utility"
DESCRIPTION = "Tool to initialize the system using TDX measurements and manage disk encryption"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://tdx-init.go \
    file://keys.go \
    file://passphrase.go \
    file://go.mod \
"

inherit go

GO_IMPORT = "github.com/flashbots/tdx-init"

# Dependencies needed for disk encryption and filesystem operations
RDEPENDS:${PN} += " \
    cryptsetup \
    e2fsprogs-mke2fs \
    util-linux-lsblk \
    e2fsprogs \
    util-linux-mount \
"

do_compile() {
    cd ${S}/src/${GO_IMPORT}
    ${GO} build -o tdx-init
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/src/${GO_IMPORT}/tdx-init ${D}${bindir}/
}
