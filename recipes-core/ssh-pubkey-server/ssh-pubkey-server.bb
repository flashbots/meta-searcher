SUMMARY = "SSH Public Key Server"
HOMEPAGE = "https://github.com/flashbots/ssh-pubkey-server"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://src/${GO_WORKDIR}/LICENSE;md5=c7bc88e866836b5160340e6c3b1aaa10"

inherit go-mod update-rc.d

INITSCRIPT_NAME = "ssh-pubkey-server-init"
INITSCRIPT_PARAMS = "defaults 98"

GO_IMPORT = "github.com/flashbots/ssh-pubkey-server"
SRC_URI = "git://${GO_IMPORT};protocol=https;branch=main \
           file://ssh-pubkey-server-init"
SRCREV = "${AUTOREV}"

GO_INSTALL = "${GO_IMPORT}/cmd/httpserver"
GO_LINKSHARED = ""

# reproducible builds
INHIBIT_PACKAGE_DEBUG_SPLIT = '1'
INHIBIT_PACKAGE_STRIP = '1'
GO_EXTRA_LDFLAGS:append = " -s -w -buildid= -X github.com/flashbots/ssh-pubkey-server/common.Version=${PV}"


DEPENDS += "cvm-reverse-proxy"
RDEPENDS:${PN} = "cvm-reverse-proxy"

do_compile[network] = "1"

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/${INITSCRIPT_NAME} ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}
}

FILES:${PN} = "${sysconfdir}/init.d/${INITSCRIPT_NAME} ${bindir}/httpserver"