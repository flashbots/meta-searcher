SUMMARY = "TDX Initialization Utility"
DESCRIPTION = "Tool to initialize the system using TDX measurements and manage disk encryption"
HOMEPAGE = "https://github.com/flashbots/tdx-init"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://src/${GO_WORKDIR}/LICENSE;md5=c7bc88e866836b5160340e6c3b1aaa10"

inherit go-mod

GO_IMPORT = "github.com/flashbots/tdx-init"
SRC_URI = "git://${GO_IMPORT};protocol=https;branch=main"
SRCREV = "${AUTOREV}"

GO_INSTALL = "${GO_IMPORT}"
GO_LINKSHARED = ""

# reproducible builds
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"
GO_EXTRA_LDFLAGS:append = " -s -w -buildid="

# Dependencies needed for disk encryption and filesystem operations
RDEPENDS:${PN} += " \
    cryptsetup \
    e2fsprogs-mke2fs \
    util-linux-lsblk \
    e2fsprogs \
    util-linux-mount \
"

do_compile[network] = "1"

FILES:${PN} = "${bindir}/tdx-init"