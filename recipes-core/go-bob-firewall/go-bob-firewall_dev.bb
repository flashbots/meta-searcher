SUMMARY = "Bob Firewall Implementation in Go"
HOMEPAGE = "https://github.com/flashbots/go-bob-firewall"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://src/${GO_WORKDIR}/LICENSE;md5=c7bc88e866836b5160340e6c3b1aaa10"

inherit go-mod update-rc.d

INITSCRIPT_NAME = "go-bob-firewall-init"
INITSCRIPT_PARAMS = "defaults 86"

GO_IMPORT = "github.com/flashbots/go-bob-firewall"
SRC_URI = "git://${GO_IMPORT};protocol=https;branch=main \
           file://go-bob-firewall-init"
SRCREV = "${AUTOREV}"

GO_INSTALL = "${GO_IMPORT}/cmd/httpserver"
GO_LINKSHARED = ""

INHIBIT_PACKAGE_DEBUG_SPLIT = '1'
INHIBIT_PACKAGE_STRIP = '1'
GO_EXTRA_LDFLAGS:append = " -s -w -buildid= -X ${GO_IMPORT}/common.Version=${PV}"

do_compile[network] = "1"

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/${INITSCRIPT_NAME} ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}
    
    # Rename the binary
    mv ${D}${bindir}/httpserver ${D}${bindir}/go-bob-firewall
}
 
FILES:${PN} = "${sysconfdir}/init.d/${INITSCRIPT_NAME} ${bindir}/go-bob-firewall"
