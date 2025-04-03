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

inherit go-mod

GO_IMPORT = "github.com/flashbots/tdx-init"

GO_LINKSHARED = ""
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"
GO_EXTRA_LDFLAGS = "-s -w -buildid="

# Dependencies needed for disk encryption and filesystem operations
RDEPENDS:${PN} += " \
    cryptsetup \
    e2fsprogs-mke2fs \
    util-linux-lsblk \
    e2fsprogs \
    util-linux-mount \
"

do_compile() {
    cd ${WORKDIR}
    ${GO} build -trimpath -ldflags "${GO_EXTRA_LDFLAGS}" -o tdx-init
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/tdx-init ${D}${bindir}/
}
