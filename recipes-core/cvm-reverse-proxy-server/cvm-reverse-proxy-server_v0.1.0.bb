SUMMARY = "Server Side reverse proxy (no init script)"
HOMEPAGE = "https://github.com/flashbots/cvm-reverse-proxy"
LICENSE = "AGPL-3.0-only"
LIC_FILES_CHKSUM = "file://src/${GO_WORKDIR}/LICENSE;md5=4ae09d45eac4aa08d013b5f2e01c67f6"

inherit go-mod

GO_IMPORT = "github.com/flashbots/cvm-reverse-proxy"
SRC_URI = "git://${GO_IMPORT};protocol=https;branch=main"
SRCREV = "v0.1.0"

GO_INSTALL = "${GO_IMPORT}/cmd/proxy-server"
GO_LINKSHARED = ""

# reproducible builds
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"
GO_EXTRA_LDFLAGS:append = " -s -w -buildid= -X ${GO_IMPORT}/common.Version=${PV}"

do_compile[network] = "1"

FILES:${PN} = "${bindir}/proxy-server"